const CLOUD_ENV = 'prod-3g0nnk2k58e9785d'
const SERVICE_NAME = 'argue-server'
const _wx = globalThis.wx

export function request(options) {
  const guestToken = uni.getStorageSync('guest_token')
  return _wx.cloud.callContainer({
    config: { env: CLOUD_ENV },
    path: options.url,
    method: options.method || 'GET',
    data: options.data || '',
    header: {
      'content-type': 'application/json',
      'X-WX-SERVICE': SERVICE_NAME,
      'X-Guest-Token': guestToken || '',
      ...options.header
    }
  }).then(res => {
    if (res.statusCode >= 200 && res.statusCode < 300) return res.data
    throw res
  })
}
