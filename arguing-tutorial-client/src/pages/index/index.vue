<template>
  <view class="container">
    <text class="title">吵架修炼场</text>
    <text class="subtitle">首页 - 场景列表将在这里展示</text>
  </view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useGuest } from '@/composables/useGuest'

const { ensureToken } = useGuest()

onMounted(async () => {
  await ensureToken()
})
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #1a1a2e;
}

.title {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.subtitle {
  font-size: 28rpx;
  color: #888888;
}
</style>
