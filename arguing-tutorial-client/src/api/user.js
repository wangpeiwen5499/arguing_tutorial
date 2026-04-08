import { request } from './index'

/**
 * 获取当前用户信息
 * @returns {Promise<Object>} 用户信息
 */
export function getUserInfo() {
  return request({
    url: '/api/users/me',
    method: 'GET'
  })
}

/**
 * 更新用户信息
 * @param {Object} data - 更新数据 { nickname, avatarUrl }
 * @returns {Promise<Object>} 更新后的用户信息
 */
export function updateUserInfo(data) {
  return request({
    url: '/api/users/me',
    method: 'PUT',
    data
  })
}
