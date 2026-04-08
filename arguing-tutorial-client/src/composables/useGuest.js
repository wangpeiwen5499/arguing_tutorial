import { request } from '@/api/index'

export function useGuest() {
  const getToken = () => uni.getStorageSync('guest_token')

  const ensureToken = async () => {
    let token = getToken()
    if (!token) {
      const res = await request({ url: '/api/auth/guest', method: 'POST' })
      token = res.guestToken
      uni.setStorageSync('guest_token', token)
    }
    return token
  }

  return { getToken, ensureToken }
}
