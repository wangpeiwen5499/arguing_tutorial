<template>
  <view class="container">
    <!-- 头部区域 -->
    <view class="header">
      <view class="header-bg"></view>
      <view class="user-info">
        <!-- 游客：默认头像 + 点击登录 -->
        <view v-if="isGuest" class="avatar-wrapper" @click="onLogin">
          <view class="avatar-default">
            <text class="avatar-icon">?</text>
          </view>
        </view>
        <!-- 已登录：真实头像 -->
        <image
          v-else
          class="avatar-real"
          :src="userInfo?.avatarUrl || ''"
          mode="aspectFill"
        />
        <view class="user-text">
          <text v-if="isGuest" class="login-hint" @click="onLogin">点击登录</text>
          <text v-else class="nickname">{{ userInfo?.nickname || '用户' }}</text>
          <text class="user-tag">{{ isGuest ? '游客模式' : '已登录' }}</text>
        </view>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-section">
      <view class="stats-card">
        <view class="stat-item">
          <text class="stat-value">{{ userInfo?.totalSessions || 0 }}</text>
          <text class="stat-label">总场次</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ userInfo?.avgScore || 0 }}</text>
          <text class="stat-label">平均分</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ userInfo?.bestScore || 0 }}</text>
          <text class="stat-label">最高分</text>
        </view>
      </view>
    </view>

    <!-- 历史记录列表 -->
    <view class="history-section">
      <view class="section-header">
        <text class="section-title">历史记录</text>
      </view>

      <view v-if="loading" class="loading">
        <text class="loading-text">加载中...</text>
      </view>

      <view v-else-if="history.length === 0" class="empty">
        <text class="empty-text">暂无对练记录</text>
        <text class="empty-sub">完成一次对练后这里会显示你的记录</text>
      </view>

      <view v-else class="history-list">
        <view
          v-for="item in history"
          :key="item.sessionId"
          class="history-item"
          @click="onHistoryClick(item)"
        >
          <view class="history-left">
            <text class="history-scene">{{ item.sceneName }}</text>
            <text class="history-date">{{ item.date }}</text>
          </view>
          <view class="history-right">
            <text class="history-score">{{ item.score }}</text>
            <text class="history-score-label">分</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 游客提示 CTA -->
    <view v-if="isGuest" class="guest-cta">
      <text class="guest-cta-text">登录后保存完整历史</text>
      <view class="guest-cta-btn" @click="onLogin">
        <text class="guest-cta-btn-text">微信登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { storeToRefs } from 'pinia'

const userStore = useUserStore()
const { userInfo, history } = storeToRefs(userStore)

const loading = ref(false)

const isGuest = computed(() => {
  return !userStore.isLoggedIn || userInfo.value?.isGuest !== false
})

function onLogin() {
  // 微信小程序登录
  uni.login({
    provider: 'weixin',
    success: (loginRes) => {
      console.log('微信登录成功:', loginRes.code)
      // TODO: 将 code 发送到后端换取用户信息
    },
    fail: (err) => {
      console.error('微信登录失败:', err)
      uni.showToast({ title: '登录失败，请重试', icon: 'none' })
    }
  })
}

function onHistoryClick(item: any) {
  uni.navigateTo({
    url: `/pages/report/index?sessionId=${item.sessionId}`
  })
}

async function loadData() {
  loading.value = true
  try {
    await Promise.all([
      userStore.fetchUserInfo(),
      userStore.fetchHistory()
    ])
  } catch (e) {
    console.error('加载数据失败:', e)
  } finally {
    loading.value = false
  }
}

onShow(() => {
  loadData()
})
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #1a1a2e;
}

/* 头部区域 */
.header {
  position: relative;
  padding: 60rpx 40rpx 40rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, #16213e 0%, #1a1a2e 100%);
}

.user-info {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: center;
  z-index: 1;
}

.avatar-wrapper {
  margin-right: 28rpx;
}

.avatar-default {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-icon {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: bold;
}

.avatar-real {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  margin-right: 28rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.2);
}

.user-text {
  display: flex;
  flex-direction: column;
}

.login-hint {
  font-size: 36rpx;
  color: #ffffff;
  font-weight: bold;
}

.nickname {
  font-size: 36rpx;
  color: #ffffff;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.user-tag {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8rpx;
}

/* 统计卡片 */
.stats-section {
  padding: 0 32rpx;
  margin-top: -20rpx;
}

.stats-card {
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  align-items: center;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  border: 2rpx solid rgba(255, 255, 255, 0.1);
  border-radius: 24rpx;
  padding: 36rpx 20rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.stat-value {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: bold;
}

.stat-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8rpx;
}

.stat-divider {
  width: 2rpx;
  height: 60rpx;
  background-color: rgba(255, 255, 255, 0.1);
}

/* 历史记录 */
.history-section {
  flex: 1;
  padding: 32rpx;
}

.section-header {
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: bold;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80rpx 0;
}

.loading-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 12rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.25);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.history-item {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  background-color: rgba(255, 255, 255, 0.06);
  border-radius: 20rpx;
  transition: background-color 0.2s ease;
}

.history-item:active {
  background-color: rgba(255, 255, 255, 0.1);
}

.history-left {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.history-scene {
  font-size: 30rpx;
  color: #ffffff;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.history-date {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.4);
}

.history-right {
  display: flex;
  flex-direction: row;
  align-items: baseline;
}

.history-score {
  font-size: 44rpx;
  color: #667eea;
  font-weight: bold;
}

.history-score-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.4);
  margin-left: 4rpx;
}

/* 游客 CTA */
.guest-cta {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 32rpx 60rpx;
}

.guest-cta-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 24rpx;
}

.guest-cta-btn {
  width: 100%;
  padding: 24rpx 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 48rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
}

.guest-cta-btn:active {
  opacity: 0.85;
}

.guest-cta-btn-text {
  font-size: 30rpx;
  color: #ffffff;
  font-weight: 500;
}
</style>
