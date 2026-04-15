import { ref } from 'vue'

/**
 * 封装微信小程序录音管理器
 * - start(): 开始录音（MP3, 16000Hz, 最大60s自动截止）
 * - stop(): 停止录音，返回 { filePath, duration }，duration < 1s 返回 null
 * - cancel(): 取消录音
 */
export function useRecording() {
  const recorderManager = uni.getRecorderManager()
  const isRecording = ref(false)

  let startTime = 0
  let resolveStop = null
  let rejectStop = null

  // 监听录音结束事件
  recorderManager.onStop((res) => {
    const duration = Date.now() - startTime
    isRecording.value = false

    if (resolveStop) {
      if (duration < 1000) {
        // 录音时长不足 1 秒，视为无效
        resolveStop(null)
      } else {
        resolveStop({
          filePath: res.tempFilePath,
          duration: Math.floor(duration / 1000)
        })
      }
      resolveStop = null
      rejectStop = null
    }
  })

  // 监听录音错误事件
  recorderManager.onError((err) => {
    console.error('录音错误:', err)
    isRecording.value = false
    if (rejectStop) {
      rejectStop(err)
      resolveStop = null
      rejectStop = null
    }
  })

  /**
   * 开始录音
   */
  function start() {
    if (isRecording.value) return

    return new Promise((resolve, reject) => {
      uni.authorize({
        scope: 'scope.record',
        success: () => {
          doStart(resolve, reject)
        },
        fail: () => {
          // 权限被拒绝，尝试直接开始（部分情况用户已授权过）
          doStart(resolve, reject)
        }
      })
    })
  }

  function doStart(resolve, reject) {
    startTime = Date.now()
    isRecording.value = true
    resolveStop = resolve
    rejectStop = reject

    recorderManager.start({
      format: 'mp3',
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 96000,
      duration: 60000 // 最大 60 秒
    })
  }

  /**
   * 停止录音，返回 Promise<{ filePath, duration } | null>
   */
  function stop() {
    if (!isRecording.value) return Promise.resolve(null)

    return new Promise((resolve, reject) => {
      resolveStop = resolve
      rejectStop = reject
      recorderManager.stop()
    })
  }

  /**
   * 取消录音
   */
  function cancel() {
    if (!isRecording.value) return

    // 将 resolveStop 设为返回 null 的函数
    const originalResolve = resolveStop
    resolveStop = null
    rejectStop = null
    isRecording.value = false

    recorderManager.stop()
    if (originalResolve) {
      originalResolve(null)
    }
  }

  return {
    isRecording,
    start,
    stop,
    cancel
  }
}
