import { request } from './index'

const _wx = globalThis.wx
const CLOUD_ENV = 'prod-3g0nnk2k58e9785d'

/**
 * 上传文件到云托管内置 COS
 * @param {string} filePath - 本地临时文件路径
 * @param {string} cloudPath - COS 中的路径（如 "audio/1/123456.mp3"）
 * @param {function} onProgress - 上传进度回调 (progress: number) => void
 * @returns {Promise<string>} fileID
 */
export function uploadToCloud(filePath, cloudPath, onProgress) {
  return new Promise((resolve, reject) => {
    const task = _wx.cloud.uploadFile({
      cloudPath,
      filePath,
      config: { env: CLOUD_ENV },
      success: res => resolve(res.fileID),
      fail: e => {
        const info = e.toString()
        reject(new Error(
          info.includes('abort')
            ? '【文件上传失败】中断上传'
            : '【文件上传失败】网络或其他错误'
        ))
      }
    })
    task.onProgressUpdate(res => {
      console.log(`上传进度：${res.progress}%`)
      if (onProgress) onProgress(res.progress)
    })
  })
}

/**
 * 将 fileID 转换为可播放的临时链接。
 * - 传入 wx.cloud.uploadFile 返回的真实 fileID → 调用 getTempFileURL 换取临时链接
 * - 传入 HTTP URL（后端 COS SDK 上传的文件）→ 直接返回
 * @param {string} fileIDOrUrl - 真实 fileID 或 HTTP URL
 * @returns {Promise<string>} 可访问的链接
 */
export function getPlayableUrl(fileIDOrUrl) {
  if (!fileIDOrUrl) return Promise.resolve('')
  // 已经是完整 HTTP URL，直接返回
  if (fileIDOrUrl.startsWith('http')) return Promise.resolve(fileIDOrUrl)
  // cloud:// 开头的真实 fileID，换取临时链接
  return _wx.cloud.getTempFileURL({
    fileList: [{ fileID: fileIDOrUrl, maxAge: 3600 }],
    config: { env: CLOUD_ENV }
  }).then(res => {
    if (res.fileList && res.fileList.length > 0 && res.fileList[0].tempFileURL) {
      return res.fileList[0].tempFileURL
    }
    throw new Error('获取文件临时链接失败')
  })
}

/**
 * 开始对练会话
 * @param {number} sceneId - 场景 ID
 * @returns {Promise<Object>} { code, data: { sessionId, replyText, ... } }
 */
export function startSession(sceneId) {
  return request({
    url: '/api/sessions',
    method: 'POST',
    data: { sceneId }
  })
}

/**
 * 发送语音消息，获取 AI 回复
 * @param {number} sessionId - 会话 ID
 * @param {string} audioUrl - 音频文件的临时访问链接（后端用于下载）
 * @param {string} audioKey - COS 对象键（永久标识，存数据库）
 * @returns {Promise<Object>} { code, data: { replyText, ... } }
 */
export function chat(sessionId, audioUrl, audioKey) {
  return request({
    url: `/api/sessions/${sessionId}/chat`,
    method: 'POST',
    data: { audioUrl, audioKey }
  })
}

/**
 * 请求策略提示
 * @param {number} sessionId - 会话 ID
 * @returns {Promise<Object>} { code, data: { hint } }
 */
export function requestHint(sessionId) {
  return request({
    url: `/api/sessions/${sessionId}/hint`,
    method: 'POST'
  })
}

/**
 * 结束对练会话
 * @param {number} sessionId - 会话 ID
 * @returns {Promise<Object>} { code, data: { sessionId } }
 */
export function endSession(sessionId) {
  return request({
    url: `/api/sessions/${sessionId}/end`,
    method: 'POST'
  })
}
