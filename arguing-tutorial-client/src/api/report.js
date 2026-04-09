import { request } from './index'

/**
 * 获取复盘报告
 * @param {number} sessionId - 会话 ID
 * @returns {Promise<Object>} 报告详情
 */
export function getReport(sessionId) {
  return request({
    url: `/api/reports/${sessionId}`,
    method: 'GET'
  })
}

/**
 * 获取历史报告列表
 * @param {number} [page=1] - 页码
 * @param {number} [size=10] - 每页条数
 * @returns {Promise<Object>} 报告列表
 */
export function getReportList(page = 1, size = 10) {
  return request({
    url: '/api/reports',
    method: 'GET',
    data: { page, size }
  })
}

/**
 * 获取分享卡片图片
 * @param {number} sessionId - 会话 ID
 * @returns {Promise<Object>} 分享卡片信息
 */
export function getShareCard(sessionId) {
  return request({
    url: `/api/reports/${sessionId}/share-card`,
    method: 'GET'
  })
}
