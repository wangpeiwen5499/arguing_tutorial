<template>
  <view style="display: none;"></view>
</template>

<script>
/**
 * AudioPlayer 组件
 * 封装 InnerAudioContext，提供 play / stop 方法
 * Emits: ended, error
 *
 * 注意：此组件没有模板，仅作为逻辑封装使用。
 * 在父组件中通过 ref 调用 play(url) / stop() 方法。
 */
export default {
  name: 'AudioPlayer',
  emits: ['ended', 'error'],
  data() {
    return {
      innerAudio: null,
      playing: false
    }
  },
  beforeUnmount() {
    this.destroy()
  },
  methods: {
    /**
     * 播放音频
     * @param {string} url - 音频 URL
     */
    play(url) {
      if (!url) {
        // 没有 URL 时直接触发 ended
        this.$emit('ended')
        return
      }

      this.destroy()

      this.innerAudio = uni.createInnerAudioContext()
      this.innerAudio.src = url
      this.playing = true

      this.innerAudio.onEnded = () => {
        this.playing = false
        this.$emit('ended')
      }

      this.innerAudio.onError = (err) => {
        console.error('音频播放错误:', err)
        this.playing = false
        this.$emit('error', err)
      }

      this.innerAudio.play()
    },

    /**
     * 停止播放
     */
    stop() {
      if (this.innerAudio) {
        this.innerAudio.stop()
        this.playing = false
      }
    },

    /**
     * 销毁音频上下文
     */
    destroy() {
      if (this.innerAudio) {
        this.innerAudio.stop()
        this.innerAudio.destroy()
        this.innerAudio = null
        this.playing = false
      }
    }
  }
}
</script>
