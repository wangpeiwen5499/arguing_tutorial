---
name: wxcloud-download-file
description: Use when implementing WeChat Mini Program file download from wxcloudrun object storage, using wx.cloud.downloadFile API, handling download progress, or saving downloaded files locally
---

# 微信云托管对象存储 - 下载文件

从微信云托管对象存储空间下载文件到小程序本地。仅适用于微信小程序端。

## API 概览

```javascript
wx.cloud.downloadFile({
  fileID: 'cloud://xxx', // 必填，云文件 ID
  config: { env: '环境ID' }, // 可选，指定环境
  success: res => {},
  fail: err => {},
  complete: () => {}
})
```

## 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileID | String | 是 | 对象存储文件ID，从上传接口或控制台获取 |
| config | Object | 否 | 配置对象，含 `env` 字段指定环境ID |
| success | Function | 否 | 成功回调 |
| fail | Function | 否 | 失败回调 |
| complete | Function | 否 | 结束回调 |

### success 返回参数

| 字段 | 类型 | 说明 |
|------|------|------|
| tempFilePath | String | 临时文件路径 |
| statusCode | Number | HTTP 状态码 |
| errMsg | String | 成功为 `downloadFile:ok` |

### fail 返回参数

| 字段 | 类型 | 说明 |
|------|------|------|
| errCode | Number | 错误码 |
| errMsg | String | 格式 `downloadFile:fail msg` |

### 返回值

返回 `downloadTask` 对象，可监听下载进度和取消任务：
- `downloadTask.onProgressUpdate(callback)` - 监听进度
- `downloadTask.abort()` - 取消下载

## 使用方式

### 1. Promise 风格（推荐）

```javascript
const res = await wx.cloud.downloadFile({
  fileID: 'cloud://test.png'
})
console.log(res.tempFilePath)
```

### 2. Callback 风格

```javascript
wx.cloud.downloadFile({
  fileID: 'cloud://test.png',
  success: res => {
    console.log(res.tempFilePath)
  },
  fail: err => {
    console.error(err)
  }
})
```

### 3. 带进度监听的完整封装

```javascript
/**
 * 下载微信云托管对象存储文件到本地
 * @param {string} fileID 对象存储文件ID
 * @param {function} onCall 进度回调，返回 false 可中断下载
 * @returns {Promise} 返回包含 tempFilePath 的结果对象
 */
function downloadFile(fileID, onCall = () => {}) {
  return new Promise((resolve, reject) => {
    const task = wx.cloud.downloadFile({
      fileID,
      success: res => resolve(res),
      fail: e => {
        const info = e.toString()
        if (info.indexOf('abort') !== -1) {
          reject(new Error('【文件下载失败】中断下载'))
        } else {
          reject(new Error('【文件下载失败】网络或其他错误'))
        }
      }
    })
    task.onProgressUpdate((res) => {
      if (onCall(res) === false) {
        task.abort()
      }
    })
  })
}
```

### 4. 下载后保存到本地

```javascript
const res = await wx.cloud.downloadFile({
  fileID: 'cloud://test.png'
})
await wx.saveFile({
  tempFilePath: res.tempFilePath
})
```

## 资源复用（跨账号访问）

当需要访问其他微信账号下的云托管环境时，需先初始化跨账号 Cloud 实例：

```javascript
// 在 app.js 中初始化
App({
  async onLaunch() {
    const cloud = new wx.cloud.Cloud({
      resourceAppid: 'wx886699112233', // 环境所属账号 appid
      resourceEnv: 'prod-weruntest'    // 云托管环境ID
    })
    await cloud.init()
    this.cloud = cloud
  }
})

// 在页面中使用
const app = getApp()
const res = await app.cloud.downloadFile({
  fileID: 'cloud://test.png'
})
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| fileID 无效 | 从上传接口返回值或控制台获取正确的 cloud:// ID |
| 环境不匹配 | 通过 config.env 指定正确的环境ID |
| 跨账号访问失败 | 确保已初始化 wx.cloud.Cloud 并 await init() 完成 |
| 下载中断 | 检查 onCall 回调是否误返回 false |
| 临时文件过期 | 及时调用 wx.saveFile 保存到本地 |
