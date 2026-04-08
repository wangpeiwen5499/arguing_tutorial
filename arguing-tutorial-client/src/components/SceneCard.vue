<template>
  <view class="scene-card" :style="cardStyle" @click="onTap">
    <!-- 分类标签 -->
    <view class="category-badge" :style="badgeStyle">
      <text class="badge-text">{{ scene.category }}</text>
    </view>
    <!-- 场景名称 -->
    <text class="scene-name">{{ scene.name }}</text>
    <!-- 描述文字 -->
    <text class="scene-desc">{{ scene.description }}</text>
    <!-- 难度星级 -->
    <view class="difficulty">
      <text class="difficulty-stars">{{ starsText }}</text>
      <text class="difficulty-label">难度</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Scene {
  id: number | string
  name: string
  description: string
  category: string
  difficulty: number
}

const props = defineProps<{
  scene: Scene
}>()

const GRADIENTS = {
  '职场': 'linear-gradient(135deg, #667eea, #764ba2)',
  '技术': 'linear-gradient(135deg, #2196F3, #00BCD4)',
  '日常': 'linear-gradient(135deg, #FF9800, #F44336)',
}

const cardStyle = computed(() => ({
  background: GRADIENTS[props.scene.category] || 'linear-gradient(135deg, #667eea, #764ba2)',
}))

const badgeStyle = computed(() => {
  const gradients = {
    '职场': 'rgba(102, 126, 234, 0.6)',
    '技术': 'rgba(33, 150, 243, 0.6)',
    '日常': 'rgba(255, 152, 0, 0.6)',
  }
  return {
    backgroundColor: gradients[props.scene.category] || 'rgba(102, 126, 234, 0.6)',
  }
})

const starsText = computed(() => {
  const count = Math.max(1, Math.min(5, props.scene.difficulty || 1))
  return '\u2B50'.repeat(count)
})

function onTap() {
  uni.navigateTo({ url: '/pages/practice/index?sceneId=' + props.scene.id })
}
</script>

<style scoped>
.scene-card {
  position: relative;
  border-radius: 24rpx;
  padding: 36rpx 32rpx 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.category-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 8rpx 20rpx;
  border-radius: 24rpx 0 16rpx 0;
}

.badge-text {
  font-size: 22rpx;
  color: #ffffff;
  font-weight: 500;
}

.scene-name {
  display: block;
  font-size: 38rpx;
  color: #ffffff;
  font-weight: bold;
  margin-top: 24rpx;
  margin-bottom: 12rpx;
}

.scene-desc {
  display: block;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
  margin-bottom: 20rpx;
}

.difficulty {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10rpx;
}

.difficulty-stars {
  font-size: 24rpx;
}

.difficulty-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
}
</style>
