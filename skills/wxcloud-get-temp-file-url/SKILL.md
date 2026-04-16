---
name: wxcloud-get-temp-file-url
description: Use when implementing WeChat Mini Program cloud file URL retrieval from wxcloudrun object storage, using wx.cloud.getTempFileURL API, converting cloud fileIDs to accessible URLs, or handling file link expiration
---

# 微信云托管对象存储 - 获取文件临时链接

用云文件 ID 换取真实访问链接。公有读文件的链接不会过期，私有文件链接默认 24 小时有效期，可自定义。一次最多取 50 个。仅适用于微信小程序端。

## API 概览

```javascript
wx.cloud.getTempFileURL({
  fileList: [{ fileID: 'cloud://xxx', maxAge: 86400 }],
  config: { env: '环境ID' },
  success: res => {},
  fail: err => {},
  complete: () => {}
})
```

## 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileList | Array | 是 | 云文件ID列表，最多 50 个。元素可为字符串或对象 |
| config | Object | 否 | 配置对象，含 `env` 字段指定环境ID |
| success | Function | 否 | 成功回调 |
| fail | Function | 否 | 失败回调 |
| complete | Function | 否 | 结束回调 |

### fileList 元素结构

| 字段 | 类型 | 说明 |
|------|------|------|
| fileID | String | 云文件ID，格式 `cloud://xxx` |
| maxAge | Number | 有效期时长，单位秒，默认 86400（24小时） |

简写形式：`fileList` 也可直接传字符串数组 `['cloud://a.png', 'cloud://b.png']`

### success 返回参数

| 字段 | 类型 | 说明 |
|------|------|------|
| fileList | Array | 文件列表 |
| errMsg | String | 成功为 `ok`，失败为失败原因 |

### 返回 fileList 元素结构

| 属性 | 类型 | 说明 |
|------|------|------|
| fileID | String | 云文件 ID |
| tempFileURL | String | 临时文件访问路径 |
| maxAge | Number | 有效期时长，单位秒 |
| status | Number | 状态码，0 为成功 |
| errMsg | String | 成功为 `ok`，失败为失败原因 |

### fail 返回参数

| 字段 | 类型 | 说明 |
|------|------|------|
| errCode | Number | 错误码 |
| errMsg | String | 格式 `downloadFile:fail msg` |

## 使用方式

### 1. Promise 风格（推荐）

```javascript
// 对象形式
const res = await wx.cloud.getTempFileURL({
  fileList: [{
    fileID: 'cloud://test.png',
    maxAge: 86400
  }]
})
console.log(res.fileList[0].tempFileURL)

// 简写形式
const res = await wx.cloud.getTempFileURL({
  fileList: ['cloud://test.png']
})
console.log(res.fileList[0].tempFileURL)
```

### 2. Callback 风格

```javascript
wx.cloud.getTempFileURL({
  fileList: ['cloud://test.png'],
  success: res => {
    console.log(res.fileList)
  },
  fail: err => {
    console.error(err)
  }
})
```

### 3. 封装方法

```javascript
/**
 * 获取微信云托管对象存储文件的临时访问地址
 * @param {string|string[]} fileID 单个或多个云文件ID
 * @param {number} time 有效时间，单位秒，默认 86400
 */
async function getTempFileURL(fileID, time = 86400) {
  const list = (typeof fileID === 'string' ? [fileID] : fileID).map(item => ({
    fileID: item,
    maxAge: time,
  }))
  return await wx.cloud.getTempFileURL({ fileList: list })
}
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
const res = await app.cloud.getTempFileURL({
  fileList: ['cloud://test.png']
})
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| fileID 无效 | 从上传接口返回值或控制台获取正确的 `cloud://` ID |
| 链接过期 | 私有文件链接有效期默认 24h，可通过 maxAge 自定义 |
| 环境不匹配 | 通过 config.env 指定正确的环境ID |
| 跨账号访问失败 | 确保已初始化 `wx.cloud.Cloud` 并 `await init()` 完成 |
| 超过 50 个限制 | 分批调用，每批不超过 50 个文件ID |
| status 非 0 | 检查对应元素的 errMsg 了解具体失败原因 |
