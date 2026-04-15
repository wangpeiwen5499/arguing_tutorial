# COS 语音上传改造方案

> 背景：`callContainer` 不支持 `files` 参数上传文件。语音需先通过 `wx.cloud.uploadFile` 上传到云托管内置 COS，再传 `audioCloudPath` 给后端。

---

## 阶段一：后端改造（4 个文件）

### 1. `OssService.java` — 新增 download 和 getUrl 方法

```java
/**
 * 从 COS 下载文件，返回字节数组。
 */
public byte[] download(String key) { ... }

/**
 * 拼接 COS 文件的 HTTP URL。
 */
public String getUrl(String key) { ... }
```

> `download` 用于后端从 COS 取回前端上传的音频文件，传给 ASR 识别。

### 2. `SessionController.java` — chat 接口改为接收 JSON Body

**改前**：`@RequestParam MultipartFile audio`
**改后**：`@RequestBody Map<String, Object> body`，从中取 `audioCloudPath`

```java
@PostMapping("/{id}/chat")
public ResponseEntity<Map<String, Object>> chat(
        @PathVariable("id") Long sessionId,
        @RequestBody Map<String, Object> body,
        HttpServletRequest request) {
    String audioCloudPath = (String) body.get("audioCloudPath");
    // ...
}
```

### 3. `SessionService.java` — chat 方法改用 COS 路径

**改前**：接收 `MultipartFile audioFile`，调 `speechService.recognize(audioFile)` + `saveAudioFile()`
**改后**：接收 `String audioCloudPath`，从 COS 下载音频，调 `speechService.recognize(bytes, format)`

```java
public ChatResponse chat(Long userId, Long sessionId, String audioCloudPath) {
    // 1. 从 COS 下载音频
    byte[] audioData = ossService.download(audioCloudPath);
    // 2. ASR 识别
    String userText = speechService.recognize(audioData, audioCloudPath);
    // 3. audioCloudPath 直接作为 audioUrl 保存（已是 COS 可访问路径）
    String audioUrl = ossService.getUrl(audioCloudPath);
    // ... 后续逻辑不变
}
```

### 4. `SpeechService.java` — 新增 recognize 重载

```java
/**
 * ASR 识别（字节数组版本，用于 COS 下载的音频）
 */
public String recognize(byte[] audioData, String filenameOrPath) { ... }
```

> 原有 `recognize(MultipartFile)` 保留，新增基于 `byte[]` 的重载。

---

## 阶段二：前端改造（4 个文件）

### 1. `api/index.js` — 简化为 Promise 风格

`callContainer` 不传 success/fail 回调时直接返回 Promise，去掉手动 `new Promise` 包装：

```js
export function request(options) {
  const guestToken = uni.getStorageSync('guest_token')
  return _wx.cloud.callContainer({
    config: { env: CLOUD_ENV },
    path: options.url,
    method: options.method || 'GET',
    data: options.data || '',
    header: {
      'content-type': 'application/json',
      'X-WX-SERVICE': SERVICE_NAME,
      'X-Guest-Token': guestToken || '',
      ...options.header
    }
  }).then(res => {
    if (res.statusCode >= 200 && res.statusCode < 300) return res.data
    throw res
  })
}
```

### 2. 新建 `api/upload.js` — COS 上传封装

```js
const _wx = globalThis.wx
const CLOUD_ENV = 'prod-3g0nnk2k58e9785d'

/**
 * 上传文件到云托管内置 COS
 * @param {string} filePath - 本地临时文件路径
 * @param {string} cloudPath - COS 中的路径（如 "audio/xxx.mp3"）
 * @returns {Promise<string>} fileID
 */
export function uploadToCloud(filePath, cloudPath) {
  return _wx.cloud.uploadFile({
    cloudPath,
    filePath,
    config: { env: CLOUD_ENV }
  }).then(res => res.fileID)
}
```

### 3. `api/session.js` — chat 函数改为传 audioCloudPath

**改前**：直接用 `callContainer` + `files` 参数（不支持）
**改后**：调用 `request()` 发送 JSON `{ audioCloudPath }`

```js
export function chat(sessionId, audioCloudPath) {
  return request({
    url: `/api/sessions/${sessionId}/chat`,
    method: 'POST',
    data: { audioCloudPath }
  })
}
```

### 4. `practice/index.vue` — onRecordComplete 增加上传步骤

**改前**：`chat(sessionId, filePath)`
**改后**：`uploadToCloud(filePath, cloudPath)` → `chat(sessionId, cloudPath)`

```js
async function onRecordComplete(filePath) {
  // 1. 上传到 COS
  const cloudPath = `audio/${sessionId.value}/${Date.now()}.mp3`
  const fileID = await uploadToCloud(filePath, cloudPath)
  // 2. 发送 chat 请求（传 cloudPath 而非 filePath）
  const res = await chat(sessionId.value, cloudPath)
  // ...
}
```

---

## 数据流图

```
录音完成 → wx.cloud.uploadFile → COS 存储
                                      ↓
                              返回 cloudPath
                                      ↓
                     callContainer POST { audioCloudPath }
                                      ↓
                          后端 ossService.download(cloudPath)
                                      ↓
                              ASR → AI → TTS
                                      ↓
                           返回 { text, audioUrl, ... }
```

---

## 改动文件清单

| 阶段 | 文件 | 改动类型 |
|------|------|---------|
| 后端 | `OssService.java` | 新增 download/getUrl 方法 |
| 后端 | `SessionController.java` | chat 参数从 MultipartFile 改为 String |
| 后端 | `SessionService.java` | chat 逻辑改为 COS 下载 + 新 recognize |
| 后端 | `SpeechService.java` | 新增 recognize(byte[], String) 重载 |
| 前端 | `api/index.js` | 简化为 Promise 风格 |
| 前端 | `api/upload.js` | 新建，封装 uploadFile |
| 前端 | `api/session.js` | chat 改为传 audioCloudPath |
| 前端 | `practice/index.vue` | onRecordComplete 增加上传步骤 |
