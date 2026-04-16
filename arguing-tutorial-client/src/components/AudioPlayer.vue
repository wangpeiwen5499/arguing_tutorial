<template>
  <view style="display: none;"></view>
</template>

<script>
/**
 * AudioPlayer 组件
 * 封装 InnerAudioContext，提供 play / stop 方法
 *
 * 注意：真机上 InnerAudioContext 的所有回调（onEnded、onPlay 等）均不触发，
 * 因此改用 setInterval 轮询 currentTime/duration 属性来检测播放结束。
 * 额外增加卡顿检测：currentTime 连续不变时判定为播放已结束。
 * Emits: ended, error
 */
export default {
  name: 'AudioPlayer',
  emits: ['ended', 'error'],
  data() {
    return {
      innerAudio: null,
      playing: false,
      endedFired: false,
      pollTimer: null,
      // 轮询阶段：0=等待开始播放, 1=正在播放/轮询中
      pollPhase: 0,
      // 已轮询次数（用于超时保护）
      pollCount: 0,
      // 上一次的 currentTime，用于卡顿检测
      lastCurrentTime: -1,
      // currentTime 连续不变的次数
      stallCount: 0,
      // 是否曾经播放过（currentTime > 0）
      hasPlayed: false
    }
  },
  beforeUnmount() {
    console.log('[AudioPlayer] beforeUnmount, 销毁音频')
    this.destroy()
  },
  methods: {
    _emitEnded(source) {
      if (this.endedFired) {
        console.log('[AudioPlayer] _emitEnded 已触发过, 跳过, 来源=', source)
        return
      }
      console.log('[AudioPlayer] _emitEnded 正式触发 ended, 来源=', source)
      this.endedFired = true
      this.playing = false
      this._stopPolling()
      this.$emit('ended')
    },

    _startPolling() {
      this._stopPolling()
      this.pollPhase = 0
      this.pollCount = 0
      this.lastCurrentTime = -1
      this.stallCount = 0
      this.hasPlayed = false
      console.log('[AudioPlayer] 启动轮询检测')

      this.pollTimer = setInterval(() => {
        this.pollCount++

        if (!this.innerAudio || !this.playing) {
          console.log('[AudioPlayer] 轮询终止: innerAudio或playing状态异常')
          this._stopPolling()
          return
        }

        const ct = this.innerAudio.currentTime
        const dur = this.innerAudio.duration
        const phase = this.pollPhase

        // 每隔10次轮询输出一次日志（约3秒），避免日志过多
        if (this.pollCount % 10 === 1) {
          console.log('[AudioPlayer] 轮询#' + this.pollCount +
            ', phase=' + phase +
            ', currentTime=' + ct +
            ', duration=' + dur +
            ', stall=' + this.stallCount)
        }

        // 阶段0：等待音频开始播放（currentTime > 0 或 duration > 0）
        if (phase === 0) {
          if (ct > 0 || dur > 0) {
            this.pollPhase = 1
            console.log('[AudioPlayer] 轮询进入播放阶段, currentTime=', ct, ', duration=', dur)
          }
          // 超时保护：等待15秒仍未开始播放
          if (this.pollCount > 50) {
            console.warn('[AudioPlayer] 轮询超时: 等待15秒音频仍未开始播放')
            this.$emit('error', { errMsg: '音频加载超时' })
            this.destroy()
          }
          return
        }

        // 阶段1：正在播放，检测结束

        // 记录是否曾播放过
        if (ct > 0) {
          this.hasPlayed = true
        }

        // 检测1：currentTime 从正值归零 = 播放结束
        if (this.hasPlayed && ct === 0) {
          console.log('[AudioPlayer] 轮询检测到播放结束(归零), lastCurrentTime=', this.lastCurrentTime, ', duration=', dur)
          this._emitEnded('polling-reset')
          return
        }

        // 检测2：currentTime 达到 duration 附近
        if (dur > 0 && ct >= dur - 0.3) {
          console.log('[AudioPlayer] 轮询检测到播放结束(到达末尾), currentTime=', ct, ', duration=', dur)
          this._emitEnded('polling-eof')
          return
        }

        // 检测2：卡顿检测 - currentTime 连续不变
        if (ct === this.lastCurrentTime && ct > 0) {
          this.stallCount++
        } else {
          this.stallCount = 0
        }
        this.lastCurrentTime = ct

        // 连续3秒（10次 * 300ms）currentTime 不变，且已经播放过一段时间
        // 说明音频已停止播放但未触发 onEnded
        if (this.stallCount >= 10 && this.pollCount > 20) {
          console.log('[AudioPlayer] 轮询检测到卡顿(播放停止), currentTime=', ct,
            ', duration=', dur, ', stallCount=', this.stallCount)
          this._emitEnded('polling-stall')
          return
        }

        // 超时保护：播放阶段最多等待180秒（600次 * 300ms）
        if (this.pollCount > 600) {
          console.warn('[AudioPlayer] 轮询超时: 播放超过180秒')
          this._emitEnded('polling-timeout')
        }
      }, 300)
    },

    _stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
        console.log('[AudioPlayer] 轮询已停止')
      }
    },

    play(url) {
      console.log('[AudioPlayer] play() 被调用, url长度=', url ? url.length : 0)

      if (!url) {
        console.log('[AudioPlayer] url为空, 直接触发 ended')
        this.$emit('ended')
        return
      }

      this.destroy()

      this.endedFired = false
      this.innerAudio = uni.createInnerAudioContext()
      this.innerAudio.src = url
      this.playing = true
      console.log('[AudioPlayer] InnerAudioContext 已创建, src已设置')

      // 注册回调作为辅助（真机可能不触发，但模拟器可能触发）
      this.innerAudio.onError = (err) => {
        console.error('[AudioPlayer] onError 回调触发, err=', JSON.stringify(err))
        this.playing = false
        this._stopPolling()
        this.$emit('error', err)
      }

      this.innerAudio.onEnded = () => {
        console.log('[AudioPlayer] onEnded 回调触发')
        this._emitEnded('onEnded-callback')
      }

      console.log('[AudioPlayer] 调用 innerAudio.play()')
      this.innerAudio.play()

      // 启动轮询检测（核心机制，不依赖回调）
      this._startPolling()
    },

    stop() {
      console.log('[AudioPlayer] stop() 被调用, playing=', this.playing)
      if (this.innerAudio) {
        this.innerAudio.stop()
        this.playing = false
      }
      this._stopPolling()
    },

    destroy() {
      this._stopPolling()
      if (this.innerAudio) {
        console.log('[AudioPlayer] destroy() 销毁旧音频上下文')
        this.innerAudio.onError = null
        this.innerAudio.onEnded = null
        try {
          this.innerAudio.stop()
          this.innerAudio.destroy()
        } catch (e) {
          // 忽略销毁时的异常
        }
        this.innerAudio = null
        this.playing = false
      }
    }
  }
}
</script>
