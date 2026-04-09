<template>
  <view class="share-card">
    <image
      v-if="cardImageUrl"
      :src="cardImageUrl"
      mode="widthFix"
      class="share-image"
    />
    <view v-else class="share-placeholder">
      <text class="placeholder-text">生成分享卡片中...</text>
    </view>
    <view class="share-actions">
      <button class="share-btn share-friend" @tap="shareToFriend">分享给好友</button>
      <button class="share-btn share-timeline" @tap="shareToTimeline">分享到朋友圈</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getShareCard } from '@/api/report'

const props = defineProps<{
  sessionId: number | string
  sceneName: string
  totalScore: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const cardImageUrl = ref('')

onMounted(async () => {
  try {
    const res: any = await getShareCard(props.sessionId)
    if (res.data && res.data.imageUrl) {
      cardImageUrl.value = res.data.imageUrl
    } else if (typeof res === 'string') {
      cardImageUrl.value = res
    }
  } catch (err) {
    console.error('获取分享卡片失败:', err)
  }
})

function shareToFriend() {
  // #ifdef MP-WEIXIN
  uni.shareAppMessage({
    title: `我在「${props.sceneName}」对练中得了 ${props.totalScore} 分，来挑战我吧！`,
    path: `/pages/practice/index?sceneId=${props.sessionId}`,
    imageUrl: cardImageUrl.value || '',
  })
  // #endif
}

function shareToTimeline() {
  // #ifdef MP-WEIXIN
  uni.shareTimeline({
    title: `我在「${props.sceneName}」对练中得了 ${props.totalScore} 分，来挑战我吧！`,
    path: `/pages/practice/index?sceneId=${props.sessionId}`,
    imageUrl: cardImageUrl.value || '',
  })
  // #endif
}
</script>

<style scoped>
.share-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx;
}

.share-image {
  width: 100%;
  border-radius: 16rpx;
}

.share-placeholder {
  width: 100%;
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 16rpx;
}

.placeholder-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.4);
}

.share-actions {
  display: flex;
  flex-direction: row;
  gap: 24rpx;
  margin-top: 32rpx;
  width: 100%;
}

.share-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  color: #ffffff;
  border: none;
  border-radius: 40rpx;
  text-align: center;
  padding: 0;
  margin: 0;
}

.share-btn::after {
  border: none;
}

.share-friend {
  background: linear-gradient(135deg, #4CAF50, #388E3C);
}

.share-timeline {
  background: linear-gradient(135deg, #667eea, #764ba2);
}
</style>
