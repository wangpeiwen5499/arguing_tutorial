---
name: wxcloud-upload-file
description: Use when uploading files from WeChat mini program to wxcloud object storage, using wx.cloud.uploadFile API
---

# 微信云托管 - 小程序上传文件

## 概述

将本地资源上传至微信云托管对象存储空间。上传至同一路径为覆盖写。本文档适用于微信小程序端。

## API

```javascript
wx.cloud.uploadFile({
  cloudPath: String,  // 必填 - 对象存储路径，不要 / 开头
  filePath: String,   // 必填 - 本地文件路径（wxfile:// 或临时路径）
  config: {
    env: String       // 可选 - 指定环境ID，不填则用 init 指定的环境
  },
  success: Function,  // 可选
  fail: Function,     // 可选
  complete: Function  // 可选
})
```

### 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cloudPath | String | 是 | 对象存储路径。根路径直接填文件名，文件夹例子 `test/文件名`，不要 `/` 开头 |
| filePath | String | 是 | 微信本地文件路径，通过选择图片、聊天文件等接口获取 |
| config.env | String | 否 | 使用的环境ID，填写后忽略 init 指定的环境 |

### success 返回

| 字段 | 类型 | 说明 |
|------|------|------|
| fileID | String | 文件ID |
| statusCode | Number | HTTP状态码 |
| errMsg | String | 格式 `uploadFile:ok` |

### fail 返回

| 字段 | 类型 | 说明 |
|------|------|------|
| errCode | Number | 错误码 |
| errMsg | String | 格式 `uploadFile:fail msg` |

### 返回值

带有 success/fail/complete 任一回调时，返回 **UploadTask** 对象，可监听上传进度和取消上传。

## 用法示例

### Promise 风格（推荐）

```javascript
wx.cloud.uploadFile({
  cloudPath: 'example.png',
  filePath: 'wxfile://test',
  config: {
    env: 'your-env-id' // 替换为自己的环境ID
  }
}).then(res => {
  console.log(res.fileID)
}).catch(err => {
  console.error(err)
})
```

### 带进度监听的完整封装

```javascript
/**
 * 上传文件到微信云托管对象存储
 * @param {string} file   微信本地文件路径
 * @param {string} path   对象存储路径，不要 / 开头
 * @param {function} onCall 上传进度回调，返回 false 可中断上传
 * @returns {Promise<string>} fileID
 */
function uploadFile(file, path, onCall = () => {}) {
  return new Promise((resolve, reject) => {
    const task = wx.cloud.uploadFile({
      cloudPath: path,
      filePath: file,
      config: { env: 'your-env-id' },
      success: res => resolve(res.fileID),
      fail: e => {
        const info = e.toString()
        reject(new Error(info.includes('abort')
          ? '【文件上传失败】中断上传'
          : '【文件上传失败】网络或其他错误'))
      }
    })
    task.onProgressUpdate(res => {
      console.log(`上传进度：${res.progress}%`)
      if (onCall(res) === false) {
        task.abort()
      }
    })
  })
}
```

## 资源复用模式

跨小程序调用其他账号的云托管环境时，需先初始化：

```javascript
// app.js 中
App({
  async onLaunch() {
    const c1 = new wx.cloud.Cloud({
      resourceAppid: 'wx886699112233', // 环境所属账号appid
      resourceEnv: 'prod-weruntest',   // 环境ID
    })
    await c1.init()
    this.cloud = c1 // 页面中用 getApp().cloud 访问
  }
})

// 页面中使用
const app = getApp()
app.cloud.uploadFile({
  cloudPath: 'example.png',
  filePath: tempFilePath,
  success: res => console.log(res.fileID)
})
```

## 注意事项

- `cloudPath` 不要以 `/` 开头
- 同路径上传会覆盖已有文件
- 返回的 `fileID` 是文件唯一标识，用于后续下载/删除操作
- 资源复用时 `wx.cloud` 替换为初始化后的 cloud 实例
