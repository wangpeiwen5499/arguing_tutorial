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
 * @param {string} audioCloudPath - COS 中的音频路径（如 "audio/1/xxx.mp3"）
 * @returns {Promise<Object>} { code, data: { replyText, ... } }
 */
export function chat(sessionId, audioCloudPath) {
  return request({
    url: `/api/sessions/${sessionId}/chat`,
    method: 'POST',
    data: { audioCloudPath }
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
