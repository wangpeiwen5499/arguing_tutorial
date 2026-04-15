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

/**
 * 微信登录（云托管模式）
 * callContainer 自动注入 X-WX-OPENID，无需前端传 code
 * @returns {Promise<Object>} { userId, nickname, avatarUrl, isGuest }
 */
export function wxLogin() {
  return request({
    url: '/api/auth/wx-login',
    method: 'POST'
  })
}
