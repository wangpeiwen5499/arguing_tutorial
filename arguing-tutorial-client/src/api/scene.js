import { request } from './index'

/**
 * 获取场景列表
 * @param {string} [category] - 可选分类筛选
 * @returns {Promise<Array>} 场景列表
 */
export function getSceneList(category) {
  return request({
    url: '/api/scenes',
    method: 'GET',
    data: category ? { category } : {}
  })
}

/**
 * 获取场景详情
 * @param {number} id - 场景 ID
 * @returns {Promise<Object>} 场景详情
 */
export function getSceneDetail(id) {
  return request({
    url: `/api/scenes/${id}`,
    method: 'GET'
  })
}
