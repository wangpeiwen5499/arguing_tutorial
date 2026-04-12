import { request } from './index'

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
 * @param {string} audioFilePath - 录音文件临时路径
 * @returns {Promise<Object>} { code, data: { replyText, ... } }
 */
export function chat(sessionId, audioFilePath) {
  return new Promise((resolve, reject) => {
    const guestToken = uni.getStorageSync('guest_token')
    // #ifdef MP-WEIXIN
    wx.cloud.callContainer({
      name: 'arguing-tutorial-server',
      path: `/api/sessions/${sessionId}/chat`,
      method: 'POST',
      dataType: 'other',
      data: {
        audio: audioFilePath
      },
      header: {
        'X-WX-OPENID': '{openid}',
        'X-Guest-Token': guestToken || '',
        'content-type': 'multipart/form-data'
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
            resolve(data)
          } catch (e) {
            reject(e)
          }
        } else {
          reject(res)
        }
      },
      fail: reject
    })
    // #endif
    // #ifndef MP-WEIXIN
    const task = uni.uploadFile({
      url: 'http://localhost:8080' + `/api/sessions/${sessionId}/chat`,
      filePath: audioFilePath,
      name: 'audio',
      header: {
        'X-Guest-Token': guestToken || ''
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const data = JSON.parse(res.data)
            resolve(data)
          } catch (e) {
            reject(e)
          }
        } else {
          reject(res)
        }
      },
      fail: reject
    })
    // #endif
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
