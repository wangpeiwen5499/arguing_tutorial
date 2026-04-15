import { defineStore } from 'pinia'
import { getUserInfo, wxLogin } from '@/api/user'
import { request } from '@/api/index'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null, // { nickname, avatarUrl, isGuest, totalSessions, avgScore, bestScore }
    isLoggedIn: false,
    history: [] // [{ sessionId, sceneName, date, score }]
  }),

  actions: {
    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        const data = res.data || res
        this.userInfo = {
          nickname: data.nickname || '',
          avatarUrl: data.avatarUrl || '',
          isGuest: data.isGuest !== false,
          totalSessions: data.totalSessions || 0,
          avgScore: data.avgScore || 0,
          bestScore: data.bestScore || 0
        }
        this.isLoggedIn = !this.userInfo.isGuest
      } catch (e) {
        console.error('获取用户信息失败:', e)
        // 未登录状态使用默认值
        this.userInfo = {
          nickname: '',
          avatarUrl: '',
          isGuest: true,
          totalSessions: 0,
          avgScore: 0,
          bestScore: 0
        }
        this.isLoggedIn = false
      }
    },

    async fetchHistory() {
      try {
        const res = await request({
          url: '/api/users/history',
          method: 'GET'
        })
        const data = res.data || res
        this.history = Array.isArray(data) ? data : []
      } catch (e) {
        console.error('获取历史记录失败:', e)
        this.history = []
      }
    },

    async handleWxLogin() {
      const res = await wxLogin()
      const data = res.data || res
      this.userInfo = {
        nickname: data.nickname || '',
        avatarUrl: data.avatarUrl || '',
        isGuest: false,
        totalSessions: data.totalSessions || 0,
        avgScore: data.avgScore || 0,
        bestScore: data.bestScore || 0
      }
      this.isLoggedIn = true
    },

    logout() {
      uni.removeStorageSync('guest_token')
      this.userInfo = null
      this.isLoggedIn = false
      this.history = []
    }
  }
})
