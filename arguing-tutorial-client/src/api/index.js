const CLOUD_SERVICE_NAME = 'arguing-tutorial-server'

function callCloud(options) {
  const guestToken = uni.getStorageSync('guest_token')
  return new Promise((resolve, reject) => {
    wx.cloud.callContainer({
      name: CLOUD_SERVICE_NAME,
      path: options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'X-WX-OPENID': '{openid}',
        'X-Guest-Token': guestToken || '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else {
          reject(res)
        }
      },
      fail: reject
    })
  })
}

export function request(options) {
  // #ifdef MP-WEIXIN
  return callCloud(options)
  // #endif
  // #ifndef MP-WEIXIN
  const guestToken = uni.getStorageSync('guest_token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: 'http://localhost:8080' + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'X-Guest-Token': guestToken || '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else {
          reject(res)
        }
      },
      fail: reject
    })
  })
  // #endif
}
