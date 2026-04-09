<template>
  <view class="voice-recorder">
    <!-- 录音中覆盖提示 -->
    <view v-if="showCancelTip" class="cancel-tip">
      <text class="cancel-tip-text">松开取消录音</text>
    </view>

    <!-- 录音波形动画 -->
    <view v-if="recording" class="waveform">
      <view
        v-for="i in 7"
        :key="i"
        class="wave-bar"
        :class="'wave-bar-' + i"
      ></view>
    </view>

    <!-- 麦克风按钮 -->
    <view
      class="mic-btn"
      :class="{ 'mic-btn-active': recording, 'mic-btn-disabled': disabled }"
      @touchstart.prevent="onTouchStart"
      @touchmove.prevent="onTouchMove"
      @touchend.prevent="onTouchEnd"
      @touchcancel.prevent="onTouchEnd"
    >
      <text class="mic-icon">{{ recording ? '🎙️' : '🎤' }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['record'])

const recording = ref(false)
const showCancelTip = ref(false)

let startY = 0
let currentY = 0
let recorderManager = null

function getRecorderManager() {
  if (!recorderManager) {
    recorderManager = uni.getRecorderManager()
    recorderManager.onStop((res) => {
      if (!recording.value) return
      recording.value = false
      showCancelTip.value = false

      // 计算录音时长
      const duration = res.duration || 0
      if (duration < 1000) {
        uni.showToast({ title: '录音时间太短', icon: 'none' })
        return
      }

      emit('record', res.tempFilePath)
    })

    recorderManager.onError((err) => {
      console.error('录音错误:', err)
      recording.value = false
      showCancelTip.value = false
      uni.showToast({ title: '录音失败', icon: 'none' })
    })
  }
  return recorderManager
}

let startTime = 0

function onTouchStart(e) {
  if (props.disabled) return

  startY = e.touches[0].clientY
  currentY = startY
  showCancelTip.value = false

  // 请求权限并开始录音
  uni.authorize({
    scope: 'scope.record',
    success: () => {
      startRecording()
    },
    fail: () => {
      // 已经授权过或需要用户手动授权
      uni.getSetting({
        success: (res) => {
          if (res.authSetting['scope.record']) {
            startRecording()
          } else {
            showAuthDialog()
          }
        }
      })
    }
  })
}

function startRecording() {
  const manager = getRecorderManager()
  startTime = Date.now()
  recording.value = true
  manager.start({
    format: 'aac',
    sampleRate: 16000,
    numberOfChannels: 1,
    encodeBitRate: 96000,
    duration: 60000
  })
}

function showAuthDialog() {
  uni.showModal({
    title: '需要麦克风权限',
    content: '请在设置中开启麦克风权限以使用语音对练功能',
    confirmText: '打开设置',
    success: (res) => {
      if (res.confirm) {
        uni.openSetting()
      }
    }
  })
}

function onTouchMove(e) {
  if (!recording.value) return
  currentY = e.touches[0].clientY
  const deltaY = startY - currentY
  showCancelTip.value = deltaY > 50
}

function onTouchEnd() {
  if (!recording.value) return

  if (showCancelTip.value) {
    // 取消录音
    const manager = getRecorderManager()
    recording.value = false
    showCancelTip.value = false
    manager.stop()
    uni.showToast({ title: '已取消', icon: 'none' })
    return
  }

  // 正常结束录音
  const manager = getRecorderManager()
  manager.stop()
}
</script>

<style scoped>
.voice-recorder {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.cancel-tip {
  position: absolute;
  top: -80rpx;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.7);
  padding: 16rpx 32rpx;
  border-radius: 16rpx;
  z-index: 10;
}

.cancel-tip-text {
  color: #ff6b6b;
  font-size: 28rpx;
}

.waveform {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 60rpx;
  margin-bottom: 24rpx;
}

.wave-bar {
  width: 6rpx;
  height: 20rpx;
  background-color: #6c63ff;
  border-radius: 3rpx;
  animation: wave 0.6s ease-in-out infinite alternate;
}

.wave-bar-1 { animation-delay: 0s; }
.wave-bar-2 { animation-delay: 0.1s; }
.wave-bar-3 { animation-delay: 0.2s; }
.wave-bar-4 { animation-delay: 0.3s; }
.wave-bar-5 { animation-delay: 0.4s; }
.wave-bar-6 { animation-delay: 0.15s; }
.wave-bar-7 { animation-delay: 0.25s; }

@keyframes wave {
  0% { height: 10rpx; }
  50% { height: 40rpx; }
  100% { height: 60rpx; }
}

.mic-btn {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c63ff, #4834d4);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 30rpx rgba(108, 99, 255, 0.4);
  transition: all 0.2s ease;
}

.mic-btn:active {
  transform: scale(0.95);
}

.mic-btn-active {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  box-shadow: 0 8rpx 30rpx rgba(255, 107, 107, 0.4);
  animation: pulse 1.5s ease-in-out infinite;
}

.mic-btn-disabled {
  opacity: 0.3;
  box-shadow: none;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.08); }
  100% { transform: scale(1); }
}

.mic-icon {
  font-size: 56rpx;
}
</style>
