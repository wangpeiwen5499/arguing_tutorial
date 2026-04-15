<template>
  <view class="practice-page">
    <!-- 顶部状态栏 -->
    <view class="top-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="progress-wrap">
        <view class="progress-bar">
          <view
            class="progress-fill"
            :style="{ width: progressPercent + '%' }"
          ></view>
        </view>
        <text class="round-text">{{ currentRound }}/{{ totalRounds }}</text>
      </view>
      <text class="scene-name">{{ sceneName }}</text>
    </view>

    <!-- 中间数字人占位区域 -->
    <view class="avatar-area">
      <view class="avatar-placeholder">
        <text class="avatar-icon">{{ aiEmotionIcon }}</text>
      </view>
    </view>

    <!-- 思考中状态 -->
    <view v-if="isThinking" class="thinking-area">
      <view class="thinking-dots">
        <text class="dot dot-1">.</text>
        <text class="dot dot-2">.</text>
        <text class="dot dot-3">.</text>
      </view>
      <text class="thinking-text">AI 正在思考</text>
    </view>

    <!-- AI 回复字幕 -->
    <view v-if="subtitle && !isThinking" class="subtitle-area">
      <text class="subtitle-text">{{ subtitle }}</text>
    </view>

    <!-- 底部操作区域 -->
    <view class="bottom-area">
      <!-- 提示按钮 -->
      <view class="action-row">
        <view class="hint-btn" @tap="onHint">
          <text class="hint-icon">💡</text>
          <text class="hint-label">提示</text>
        </view>
        <view class="end-btn" @tap="onEnd">
          <text class="end-label">结束对练</text>
        </view>
      </view>

      <!-- 麦克风按钮 -->
      <VoiceRecorder
        :disabled="!canRecord"
        @record="onRecordComplete"
      />
    </view>

    <!-- 提示弹窗 -->
    <view v-if="hintText" class="hint-modal" @tap="hintText = ''">
      <view class="hint-modal-content" @tap.stop>
        <text class="hint-modal-title">💡 策略提示</text>
        <text class="hint-modal-text">{{ hintText }}</text>
        <view class="hint-modal-close" @tap="hintText = ''">
          <text class="close-text">知道了</text>
        </view>
      </view>
    </view>

    <!-- AudioPlayer 逻辑组件（无模板） -->
    <AudioPlayer ref="audioPlayerRef" @ended="onAudioEnded" @error="onAudioError" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import VoiceRecorder from '@/components/VoiceRecorder.vue'
import AudioPlayer from '@/components/AudioPlayer.vue'
import { startSession, chat, requestHint, endSession } from '@/api/session'
import { uploadToCloud } from '@/api/upload'
import { useGuest } from '@/composables/useGuest'

// ===== 状态 =====
const statusBarHeight = ref(0)
const sceneId = ref<number>(0)
const sceneName = ref('')
const sessionId = ref<number>(0)
const currentRound = ref(0)
const totalRounds = ref(0)

const subtitle = ref('')
const isThinking = ref(false)
const isAiSpeaking = ref(false)
const canRecord = ref(false)
const hintText = ref('')

const audioPlayerRef = ref<InstanceType<typeof AudioPlayer> | null>(null)

const { ensureToken } = useGuest()

// ===== 计算属性 =====
const progressPercent = computed(() => {
  if (totalRounds.value === 0) return 0
  return Math.min(100, (currentRound.value / totalRounds.value) * 100)
})

const aiEmotionIcon = computed(() => {
  return '🤖'
})

// ===== 生命周期 =====
onMounted(async () => {
  // 获取状态栏高度
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20

  // 请求录音权限
  requestRecordPermission()

  // 确保游客 token
  await ensureToken()

  // 从页面参数获取 sceneId
  // uni-app onLoad 在 setup 中的替代方式
})

// uni-app 页面生命周期
onLoad((options: any) => {
  if (options?.sceneId) {
    sceneId.value = Number(options.sceneId)
    // 获取场景名称（从页面传参或 API）
    sceneName.value = options.sceneName || '对练场景'
    initSession()
  }
})

onShow(() => {
  requestRecordPermission()
})

onBeforeUnmount(() => {
  if (audioPlayerRef.value) {
    audioPlayerRef.value.stop()
  }
})

// ===== 方法 =====

/** 请求录音权限 */
function requestRecordPermission() {
  uni.authorize({
    scope: 'scope.record',
    success: () => {
      console.log('录音权限已获取')
    },
    fail: () => {
      console.log('录音权限被拒绝')
    }
  })
}

/** 初始化会话 */
async function initSession() {
  try {
    uni.showLoading({ title: '加载中...' })
    const res = await startSession(sceneId.value) as any
    uni.hideLoading()

    if (res.code === 200 && res.data) {
      const data = res.data
      sessionId.value = data.sessionId
      currentRound.value = data.currentRound || 0
      totalRounds.value = data.totalRounds || 10

      // 显示 AI 开场白
      if (data.text) {
        subtitle.value = data.text
        // 尝试播放语音
        if (data.audioUrl && audioPlayerRef.value) {
          isAiSpeaking.value = true
          canRecord.value = false
          audioPlayerRef.value.play(data.audioUrl)
        } else {
          // 没有语音，直接开启录音
          canRecord.value = true
        }
      } else {
        canRecord.value = true
      }
    }
  } catch (err) {
    uni.hideLoading()
    console.error('初始化会话失败:', err)
    uni.showToast({ title: '加载失败，请重试', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }
}

/** 录音完成回调 */
async function onRecordComplete(filePath: string) {
  if (!filePath || !sessionId.value) return

  canRecord.value = false
  isThinking.value = true
  subtitle.value = ''

  try {
    // 1. 上传录音到云托管内置 COS
    const cloudPath = `audio/${sessionId.value}/${Date.now()}.mp3`
    await uploadToCloud(filePath, cloudPath)

    // 2. 发送 chat 请求（传 cloudPath）
    const res = await chat(sessionId.value, cloudPath) as any

    isThinking.value = false

    if (res.code === 200 && res.data) {
      const data = res.data
      currentRound.value = data.currentRound || currentRound.value + 1
      totalRounds.value = data.totalRounds || totalRounds.value

      // 显示 AI 回复字幕
      if (data.text) {
        subtitle.value = data.text
      }

      // 检查是否已达到最后一轮
      if (currentRound.value >= totalRounds.value) {
        // 会话结束
        await endSession(sessionId.value)
        goToReport()
        return
      }

      // 尝试播放 AI 语音
      if (data.audioUrl && audioPlayerRef.value) {
        isAiSpeaking.value = true
        audioPlayerRef.value.play(data.audioUrl)
      } else {
        // 没有语音，直接开启下一轮录音
        canRecord.value = true
      }
    }
  } catch (err) {
    isThinking.value = false
    console.error('发送语音失败:', err)
    uni.showToast({ title: '发送失败，请重试', icon: 'none' })
    canRecord.value = true
  }
}

/** 音频播放结束 */
function onAudioEnded() {
  isAiSpeaking.value = false

  // 检查是否已达到最后一轮
  if (currentRound.value >= totalRounds.value) {
    goToReport()
    return
  }

  canRecord.value = true
}

/** 音频播放错误 */
function onAudioError(err: any) {
  console.error('音频播放错误:', err)
  isAiSpeaking.value = false
  // 降级为纯文字模式，直接开启录音
  uni.showToast({ title: '语音播放失败', icon: 'none' })

  if (currentRound.value >= totalRounds.value) {
    goToReport()
    return
  }

  canRecord.value = true
}

/** 请求策略提示 */
async function onHint() {
  if (!sessionId.value) return

  try {
    const res = await requestHint(sessionId.value) as any
    if (res.code === 200 && res.data) {
      hintText.value = res.data.hint || '暂无提示'
    }
  } catch (err) {
    console.error('请求提示失败:', err)
    uni.showToast({ title: '获取提示失败', icon: 'none' })
  }
}

/** 结束对练 */
function onEnd() {
  uni.showModal({
    title: '确认结束',
    content: '确定要结束本次对练吗？结束后将生成复盘报告。',
    success: async (res) => {
      if (res.confirm) {
        try {
          await endSession(sessionId.value)
          goToReport()
        } catch (err) {
          console.error('结束会话失败:', err)
          // 即使失败也跳转
          goToReport()
        }
      }
    }
  })
}

/** 跳转到报告页 */
function goToReport() {
  canRecord.value = false
  uni.redirectTo({
    url: `/pages/report/index?sessionId=${sessionId.value}`
  })
}
</script>

<style scoped>
.practice-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #1a1a2e;
  color: #ffffff;
  position: relative;
  overflow: hidden;
}

/* 顶部状态栏 */
.top-bar {
  padding: 16rpx 32rpx 24rpx;
  background-color: rgba(26, 26, 46, 0.95);
}

.progress-wrap {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16rpx;
}

.progress-bar {
  flex: 1;
  height: 8rpx;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6c63ff, #4834d4);
  border-radius: 4rpx;
  transition: width 0.5s ease;
}

.round-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.6);
  min-width: 60rpx;
}

.scene-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #ffffff;
  margin-top: 12rpx;
}

/* 数字人占位区域 */
.avatar-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.avatar-placeholder {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2d2d5e, #1a1a3e);
  border: 4rpx solid rgba(108, 99, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 60rpx rgba(108, 99, 255, 0.15);
}

.avatar-icon {
  font-size: 80rpx;
}

/* 思考中动画 */
.thinking-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0 30rpx;
}

.thinking-dots {
  display: flex;
  flex-direction: row;
  gap: 8rpx;
}

.dot {
  font-size: 40rpx;
  color: #6c63ff;
  font-weight: bold;
  animation: dot-bounce 1.4s ease-in-out infinite;
}

.dot-1 { animation-delay: 0s; }
.dot-2 { animation-delay: 0.2s; }
.dot-3 { animation-delay: 0.4s; }

@keyframes dot-bounce {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-10rpx); }
}

.thinking-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8rpx;
}

/* 字幕区域 */
.subtitle-area {
  padding: 24rpx 48rpx 30rpx;
}

.subtitle-text {
  font-size: 30rpx;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.6;
  text-align: center;
}

/* 底部操作区域 */
.bottom-area {
  padding: 24rpx 32rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.action-row {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 0 32rpx;
}

.hint-btn {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  background-color: rgba(108, 99, 255, 0.15);
  border-radius: 32rpx;
  border: 2rpx solid rgba(108, 99, 255, 0.3);
}

.hint-icon {
  font-size: 32rpx;
}

.hint-label {
  font-size: 24rpx;
  color: #6c63ff;
}

.end-btn {
  padding: 12rpx 32rpx;
  background-color: rgba(255, 107, 107, 0.1);
  border-radius: 32rpx;
  border: 2rpx solid rgba(255, 107, 107, 0.2);
}

.end-label {
  font-size: 24rpx;
  color: #ff6b6b;
}

/* 提示弹窗 */
.hint-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.hint-modal-content {
  width: 600rpx;
  background-color: #2d2d5e;
  border-radius: 24rpx;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.hint-modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #ffffff;
}

.hint-modal-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

.hint-modal-close {
  padding: 16rpx 48rpx;
  background: linear-gradient(135deg, #6c63ff, #4834d4);
  border-radius: 32rpx;
  margin-top: 12rpx;
}

.close-text {
  font-size: 28rpx;
  color: #ffffff;
}
</style>
