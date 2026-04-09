<template>
  <view class="avatar-renderer">
    <canvas
      canvas-id="avatar-canvas"
      class="avatar-canvas"
      :style="{ width: '300rpx', height: '400rpx' }"
    />
  </view>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useAvatar } from '@/composables/useAvatar'

const props = defineProps({
  /** 头像配置: { modelId, clothing, accessories } */
  avatarConfig: {
    type: Object,
    default: () => ({
      modelId: 1,
      clothing: 'suit',
      accessories: 'none'
    })
  },
  /** 情绪状态: angry/sarcastic/hesitant/compromising/confident/neutral */
  emotion: {
    type: String,
    default: 'neutral'
  },
  /** 运行状态: idle/speaking/thinking */
  status: {
    type: String,
    default: 'idle'
  },
  /** 口型音素序列（用于 speaking 状态按时间切换口型） */
  phonemes: {
    type: Array,
    // 格式: [{ phoneme: 'a', timestamp: 0 }, { phoneme: 'i', timestamp: 200 }, ...]
    default: () => []
  }
})

const instance = getCurrentInstance()

const {
  state,
  initCanvas,
  setConfig,
  playMouth,
  setEmotion,
  setIdle,
  setThinking,
  setSpeaking,
  startAnimation,
  stopAnimation,
  dispose
} = useAvatar()

// 口型播放定时器
let phonemeTimers = []

/**
 * 根据逻辑像素计算实际 canvas 绘制尺寸
 * 微信小程序中 rpx 需要转换为 px
 */
function getCanvasSize() {
  const sysInfo = uni.getSystemInfoSync()
  const pixelRatio = sysInfo.windowWidth / 750

  // 300rpx -> px, 400rpx -> px
  const width = Math.round(300 * pixelRatio)
  const height = Math.round(400 * pixelRatio)

  return { width, height }
}

/**
 * 初始化 canvas 和动画
 */
function init() {
  const { width, height } = getCanvasSize()
  initCanvas('avatar-canvas', instance.proxy, width, height)
  setConfig(props.avatarConfig)

  // 根据初始 props 设置状态
  applyStatus(props.status)
  applyEmotion(props.emotion)

  startAnimation()
}

/**
 * 应用状态
 */
function applyStatus(status) {
  clearPhonemeTimers()

  switch (status) {
    case 'idle':
      setIdle()
      break
    case 'thinking':
      setThinking()
      break
    case 'speaking':
      setSpeaking()
      playPhonemeSequence()
      break
    default:
      setIdle()
  }
}

/**
 * 应用情绪
 */
function applyEmotion(emotion) {
  setEmotion(emotion)
}

/**
 * 播放口型序列
 */
function playPhonemeSequence() {
  clearPhonemeTimers()

  if (!props.phonemes || props.phonemes.length === 0) return

  const now = Date.now()

  props.phonemes.forEach((item) => {
    const delay = item.timestamp || 0
    const timer = setTimeout(() => {
      if (props.status === 'speaking') {
        playMouth(item.phoneme || 'closed')
      }
    }, delay)
    phonemeTimers.push(timer)
  })

  // 序列播放完毕后闭合嘴巴
  if (props.phonemes.length > 0) {
    const lastPhoneme = props.phonemes[props.phonemes.length - 1]
    const closeDelay = (lastPhoneme.timestamp || 0) + 300
    const closeTimer = setTimeout(() => {
      if (props.status === 'speaking') {
        playMouth('closed')
      }
    }, closeDelay)
    phonemeTimers.push(closeTimer)
  }
}

/**
 * 清除口型定时器
 */
function clearPhonemeTimers() {
  phonemeTimers.forEach((timer) => clearTimeout(timer))
  phonemeTimers = []
}

// ===== 监听 props 变化 =====

watch(
  () => props.emotion,
  (newEmotion) => {
    applyEmotion(newEmotion)
  }
)

watch(
  () => props.status,
  (newStatus) => {
    applyStatus(newStatus)
  }
)

watch(
  () => props.avatarConfig,
  (newConfig) => {
    if (newConfig) {
      setConfig(newConfig)
    }
  },
  { deep: true }
)

watch(
  () => props.phonemes,
  () => {
    if (props.status === 'speaking') {
      playPhonemeSequence()
    }
  },
  { deep: true }
)

// ===== 生命周期 =====

onMounted(() => {
  // 延迟初始化，确保 canvas 已渲染
  setTimeout(() => {
    init()
  }, 100)
})

onBeforeUnmount(() => {
  clearPhonemeTimers()
  dispose()
})

// ===== 暴露方法（供父组件调用） =====
defineExpose({
  playMouth,
  setEmotion,
  setIdle,
  setThinking,
  setSpeaking,
  startAnimation,
  stopAnimation
})
</script>

<style scoped>
.avatar-renderer {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-canvas {
  /* 尺寸由 style 属性动态设置 */
}
</style>
