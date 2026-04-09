import { reactive } from 'vue'

/**
 * 数字人 Avatar composable
 * 使用 Canvas 2D + 几何图形绘制数字人上半身
 * 适配微信小程序 uni.createCanvasContext API
 */

// ===== 常量定义 =====

// 衣服颜色映射
const CLOTHING_COLORS = {
  suit: '#4a4a4a',    // 深灰 - 西装
  casual: '#3a7bd5',  // 蓝 - 休闲
  tshirt: '#2a2a2a',  // 黑 - T恤
  hoodie: '#8a8a8a'   // 灰 - 卫衣
}

// 肤色
const SKIN_COLOR = '#f5d0a9'
const SKIN_SHADOW = '#e8be8a'

// 头发颜色
const HAIR_COLOR = '#2c2c2c'

// 口型参数映射
const MOUTH_SHAPES = {
  a: { widthRatio: 0.28, heightRatio: 0.22, type: 'ellipse' },
  i: { widthRatio: 0.30, heightRatio: 0.04, type: 'line' },
  u: { widthRatio: 0.14, heightRatio: 0.12, type: 'ellipse' },
  e: { widthRatio: 0.22, heightRatio: 0.14, type: 'ellipse' },
  o: { widthRatio: 0.20, heightRatio: 0.18, type: 'ellipse' },
  closed: { widthRatio: 0.22, heightRatio: 0.03, type: 'line' }
}

// 眉毛角度映射（左眉内端和外端相对Y偏移）
const EYEBROW_ANGLES = {
  angry: { innerOffset: 6, outerOffset: -4 },
  confident: { innerOffset: 0, outerOffset: 0 },
  hesitant: { innerOffset: -4, outerOffset: 3 },
  neutral: { innerOffset: 0, outerOffset: 0 },
  sarcastic: { innerOffset: -2, outerOffset: 2 },
  compromising: { innerOffset: 1, outerOffset: -1 }
}

// 瞳孔大小映射
const PUPIL_SIZES = {
  angry: 0.7,
  confident: 0.85,
  hesitant: 0.5,
  neutral: 0.65,
  sarcastic: 0.6,
  compromising: 0.6
}

/**
 * 兼容性椭圆绘制（微信小程序旧版不支持 ctx.ellipse）
 * 使用 save/translate/scale/arc/restore 模拟椭圆
 */
function drawEllipse(ctx, cx, cy, rx, ry, startAngle, endAngle) {
  ctx.save()
  ctx.translate(cx, cy)
  const ratio = rx / Math.max(ry, 0.001)
  ctx.scale(ratio, 1)
  ctx.beginPath()
  ctx.arc(0, 0, ry, startAngle || 0, endAngle || 2 * Math.PI)
  // 注意：不在这里 restore，让调用方决定何时 restore
  // 因为 fill/stroke 需要在 scale 后调用
}

function finishEllipse(ctx) {
  ctx.restore()
}

/**
 * 创建并返回 useAvatar 实例
 */
export function useAvatar() {
  // ===== 响应式状态 =====
  const state = reactive({
    emotion: 'neutral',
    mouthShape: 'closed',
    breathPhase: 0,
    blinkPhase: 0,
    isBlinking: false,
    isRunning: false
  })

  const config = reactive({
    modelId: 1,
    clothing: 'suit',
    accessories: 'none'
  })

  let animationFrameId = null
  let lastTime = 0
  let nextBlinkTime = 0
  let blinkStartTime = 0
  let canvasContext = null
  let canvasWidth = 0
  let canvasHeight = 0

  // 呼吸参数
  const BREATH_PERIOD = 3000
  const BREATH_AMPLITUDE = 2

  // 眨眼参数
  const BLINK_DURATION = 200
  const BLINK_INTERVAL_MIN = 3000
  const BLINK_INTERVAL_MAX = 5000

  let breathingSpeed = 1.0
  let blinkingEnabled = true

  /**
   * 初始化 canvas 上下文
   */
  function initCanvas(canvasId, componentInstance, width, height) {
    canvasContext = uni.createCanvasContext(canvasId, componentInstance)
    canvasWidth = width
    canvasHeight = height
  }

  /**
   * 设置 avatar 配置
   */
  function setConfig(newConfig) {
    if (newConfig.modelId !== undefined) config.modelId = newConfig.modelId
    if (newConfig.clothing !== undefined) config.clothing = newConfig.clothing
    if (newConfig.accessories !== undefined) config.accessories = newConfig.accessories
  }

  /**
   * 播放口型
   */
  function playMouth(phoneme) {
    if (MOUTH_SHAPES[phoneme]) {
      state.mouthShape = phoneme
    }
  }

  /**
   * 设置情绪
   */
  function setEmotion(emotion) {
    if (EYEBROW_ANGLES[emotion]) {
      state.emotion = emotion
    }
  }

  /**
   * 设置为空闲状态
   */
  function setIdle() {
    state.emotion = 'neutral'
    state.mouthShape = 'closed'
    breathingSpeed = 1.0
    blinkingEnabled = true
  }

  /**
   * 设置为思考状态
   */
  function setThinking() {
    state.emotion = 'neutral'
    state.mouthShape = 'closed'
    breathingSpeed = 1.5
    blinkingEnabled = false
  }

  /**
   * 设置为说话状态
   */
  function setSpeaking() {
    breathingSpeed = 1.2
    blinkingEnabled = true
  }

  // ===== 绘制辅助函数 =====

  /**
   * 绘制填充椭圆
   */
  function fillEllipse(ctx, cx, cy, rx, ry, color, startAngle, endAngle) {
    ctx.save()
    ctx.translate(cx, cy)
    const ratio = rx / Math.max(ry, 0.001)
    ctx.scale(ratio, 1)
    ctx.beginPath()
    ctx.arc(0, 0, ry, startAngle || 0, endAngle || 2 * Math.PI)
    ctx.setFillStyle(color)
    ctx.fill()
    ctx.restore()
  }

  /**
   * 绘制描边椭圆
   */
  function strokeEllipse(ctx, cx, cy, rx, ry, color, lineWidth) {
    ctx.save()
    ctx.translate(cx, cy)
    const ratio = rx / Math.max(ry, 0.001)
    ctx.scale(ratio, 1)
    ctx.beginPath()
    ctx.arc(0, 0, ry, 0, 2 * Math.PI)
    ctx.setStrokeStyle(color)
    ctx.setLineWidth(lineWidth || 1)
    ctx.stroke()
    ctx.restore()
  }

  // ===== 主体绘制函数 =====

  /**
   * 绘制完整数字人
   */
  function drawAvatar(ctx, drawConfig, drawState) {
    const w = canvasWidth
    const h = canvasHeight

    if (!ctx || w === 0 || h === 0) return

    // 计算呼吸偏移
    const breathOffset = Math.sin(drawState.breathPhase) * BREATH_AMPLITUDE

    // 眨眼时的眼睛高度缩放
    const eyeScale = 1.0 - drawState.blinkPhase

    // 基准点和比例（根据画布尺寸计算）
    const centerX = w / 2
    const baseY = h * 0.42
    const headRX = w * 0.22
    const headRY = h * 0.16

    // ===== 绘制顺序（从后到前） =====

    // 1. 身体/衣服（梯形）
    drawBody(ctx, centerX, baseY, headRX, headRY, drawConfig, breathOffset)

    // 2. 配饰 - macbook
    if (drawConfig.accessories === 'macbook') {
      drawMacbook(ctx, centerX, baseY, headRY, breathOffset)
    }

    // 3. 脖子
    drawNeck(ctx, centerX, baseY, headRX, headRY, breathOffset)

    // 4. 头部
    drawHead(ctx, centerX, baseY, headRX, headRY, breathOffset)

    // 5. 头发
    drawHair(ctx, centerX, baseY, headRX, headRY, breathOffset)

    // 6. 眼睛
    drawEyes(ctx, centerX, baseY, headRX, headRY, drawState, eyeScale, breathOffset)

    // 7. 眉毛
    drawEyebrows(ctx, centerX, baseY, headRX, headRY, drawState, breathOffset)

    // 8. 嘴巴
    drawMouth(ctx, centerX, baseY, headRX, headRY, drawState, breathOffset)

    // 9. 配饰 - 眼镜
    if (drawConfig.accessories === 'glasses') {
      drawGlasses(ctx, centerX, baseY, headRX, headRY, breathOffset)
    }
  }

  /**
   * 绘制身体（梯形衣服）
   */
  function drawBody(ctx, cx, headCY, headRX, headRY, drawConfig, breathOffset) {
    const clothingColor = CLOTHING_COLORS[drawConfig.clothing] || CLOTHING_COLORS.suit

    const neckBottom = headCY + headRY + 20 + breathOffset
    const bodyTop = neckBottom
    const bodyBottom = canvasHeight

    // 梯形参数
    const topHalfWidth = headRX * 0.7
    const bottomHalfWidth = headRX * 2.0

    ctx.beginPath()
    ctx.moveTo(cx - topHalfWidth, bodyTop)
    ctx.lineTo(cx + topHalfWidth, bodyTop)
    ctx.lineTo(cx + bottomHalfWidth, bodyBottom)
    ctx.lineTo(cx - bottomHalfWidth, bodyBottom)
    ctx.closePath()

    ctx.setFillStyle(clothingColor)
    ctx.fill()

    // 领口 V 形（西装/休闲才有）
    if (drawConfig.clothing === 'suit' || drawConfig.clothing === 'casual') {
      ctx.beginPath()
      ctx.moveTo(cx - topHalfWidth * 0.3, bodyTop)
      ctx.lineTo(cx, bodyTop + 40)
      ctx.lineTo(cx + topHalfWidth * 0.3, bodyTop)
      ctx.closePath()
      ctx.setFillStyle(SKIN_COLOR)
      ctx.fill()
    }
  }

  /**
   * 绘制脖子
   */
  function drawNeck(ctx, cx, headCY, headRX, headRY, breathOffset) {
    const neckWidth = headRX * 0.35
    const neckTop = headCY + headRY * 0.7 + breathOffset
    const neckBottom = headCY + headRY + 22 + breathOffset

    ctx.beginPath()
    ctx.rect(cx - neckWidth, neckTop, neckWidth * 2, neckBottom - neckTop)
    ctx.setFillStyle(SKIN_COLOR)
    ctx.fill()

    // 脖子底部阴影
    ctx.beginPath()
    ctx.rect(cx - neckWidth, neckBottom - 4, neckWidth * 2, 4)
    ctx.setFillStyle(SKIN_SHADOW)
    ctx.fill()
  }

  /**
   * 绘制头部（椭圆）
   */
  function drawHead(ctx, cx, headCY, headRX, headRY, breathOffset) {
    const cy = headCY + breathOffset

    fillEllipse(ctx, cx, cy, headRX, headRY, SKIN_COLOR)

    // 下巴阴影（下半椭圆）
    fillEllipse(ctx, cx, cy + headRY * 0.5, headRX * 0.7, headRY * 0.5, SKIN_SHADOW, 0, Math.PI)
  }

  /**
   * 绘制头发（深色半圆，男性短发风格）
   */
  function drawHair(ctx, cx, headCY, headRX, headRY, breathOffset) {
    const cy = headCY + breathOffset

    // 主头发 - 覆盖头顶的半椭圆
    fillEllipse(ctx, cx, cy - headRY * 0.2, headRX * 1.05, headRY * 0.7, HAIR_COLOR, Math.PI, 2 * Math.PI)

    // 两侧鬓角
    const sideWidth = headRX * 0.15
    const sideHeight = headRY * 0.4

    // 左鬓角
    ctx.beginPath()
    ctx.rect(cx - headRX - sideWidth * 0.3, cy - headRY * 0.1, sideWidth, sideHeight)
    ctx.setFillStyle(HAIR_COLOR)
    ctx.fill()

    // 右鬓角
    ctx.beginPath()
    ctx.rect(cx + headRX - sideWidth * 0.7, cy - headRY * 0.1, sideWidth, sideHeight)
    ctx.setFillStyle(HAIR_COLOR)
    ctx.fill()
  }

  /**
   * 绘制眼睛
   */
  function drawEyes(ctx, cx, headCY, headRX, headRY, drawState, eyeScale, breathOffset) {
    const cy = headCY + breathOffset
    const eyeY = cy - headRY * 0.05
    const eyeSpacing = headRX * 0.5
    const eyeRadiusX = headRX * 0.18
    const eyeRadiusY = headRY * 0.14

    const pupilSize = PUPIL_SIZES[drawState.emotion] || 0.65
    const pupilRX = eyeRadiusX * pupilSize
    const pupilRY = eyeRadiusY * pupilSize

    function drawOneEye(ex, ey) {
      // 白色眼球（受眨眼影响）
      const actualRY = eyeRadiusY * Math.max(0.1, eyeScale)

      fillEllipse(ctx, ex, ey, eyeRadiusX, actualRY, '#ffffff')

      // 瞳孔（仅在不完全闭眼时绘制）
      if (eyeScale > 0.15) {
        fillEllipse(ctx, ex, ey + actualRY * 0.1, pupilRX * eyeScale, pupilRY * eyeScale, '#1a1a1a')

        // 高光点
        ctx.beginPath()
        ctx.arc(ex + pupilRX * 0.3, ey - pupilRY * 0.3 * eyeScale, Math.max(1, pupilRX * 0.25), 0, 2 * Math.PI)
        ctx.setFillStyle('#ffffff')
        ctx.fill()
      }
    }

    drawOneEye(cx - eyeSpacing, eyeY)
    drawOneEye(cx + eyeSpacing, eyeY)
  }

  /**
   * 绘制眉毛
   */
  function drawEyebrows(ctx, cx, headCY, headRX, headRY, drawState, breathOffset) {
    const cy = headCY + breathOffset
    const browY = cy - headRY * 0.32
    const browSpacing = headRX * 0.5
    const browWidth = headRX * 0.25
    const browThickness = 2.5

    const angles = EYEBROW_ANGLES[drawState.emotion] || EYEBROW_ANGLES.neutral

    ctx.setStrokeStyle(HAIR_COLOR)
    ctx.setLineWidth(browThickness)
    ctx.setLineCap('round')

    // 左眉（从内到外）
    ctx.beginPath()
    ctx.moveTo(cx - browSpacing + browWidth, browY + angles.innerOffset)
    ctx.lineTo(cx - browSpacing - browWidth, browY + angles.outerOffset)
    ctx.stroke()

    // 右眉（从内到外）
    ctx.beginPath()
    ctx.moveTo(cx + browSpacing - browWidth, browY + angles.innerOffset)
    ctx.lineTo(cx + browSpacing + browWidth, browY + angles.outerOffset)
    ctx.stroke()
  }

  /**
   * 绘制嘴巴
   */
  function drawMouth(ctx, cx, headCY, headRX, headRY, drawState, breathOffset) {
    const cy = headCY + breathOffset
    const mouthY = cy + headRY * 0.4
    const shape = MOUTH_SHAPES[drawState.mouthShape] || MOUTH_SHAPES.closed

    const mouthW = headRX * shape.widthRatio
    const mouthH = headRY * shape.heightRatio

    if (shape.type === 'ellipse') {
      // 椭圆形嘴巴
      fillEllipse(ctx, cx, mouthY, mouthW, mouthH, '#8b3a3a')

      // 口腔内部细节（较大的嘴型才显示）
      if (mouthH > 3) {
        fillEllipse(ctx, cx, mouthY + mouthH * 0.3, mouthW * 0.6, mouthH * 0.4, '#6b2a2a')
      }
    } else {
      // 线条嘴巴
      ctx.beginPath()
      ctx.moveTo(cx - mouthW, mouthY)
      ctx.lineTo(cx + mouthW, mouthY)
      ctx.setStrokeStyle('#8b3a3a')
      ctx.setLineWidth(2)
      ctx.setLineCap('round')
      ctx.stroke()
    }
  }

  /**
   * 绘制眼镜
   */
  function drawGlasses(ctx, cx, headCY, headRX, headRY, breathOffset) {
    const cy = headCY + breathOffset
    const eyeY = cy - headRY * 0.05
    const eyeSpacing = headRX * 0.5
    const glassRX = headRX * 0.24
    const glassRY = headRY * 0.2

    ctx.setStrokeStyle('#333333')
    ctx.setLineWidth(2)

    // 左镜框
    strokeEllipse(ctx, cx - eyeSpacing, eyeY, glassRX, glassRY, '#333333', 2)

    // 右镜框
    strokeEllipse(ctx, cx + eyeSpacing, eyeY, glassRX, glassRY, '#333333', 2)

    // 鼻梁
    ctx.beginPath()
    ctx.moveTo(cx - eyeSpacing + glassRX, eyeY)
    ctx.lineTo(cx + eyeSpacing - glassRX, eyeY)
    ctx.setStrokeStyle('#333333')
    ctx.setLineWidth(2)
    ctx.stroke()

    // 镜腿
    ctx.beginPath()
    ctx.moveTo(cx - eyeSpacing - glassRX, eyeY)
    ctx.lineTo(cx - headRX * 1.05, eyeY - 2)
    ctx.stroke()

    ctx.beginPath()
    ctx.moveTo(cx + eyeSpacing + glassRX, eyeY)
    ctx.lineTo(cx + headRX * 1.05, eyeY - 2)
    ctx.stroke()
  }

  /**
   * 绘制 macbook 配饰
   */
  function drawMacbook(ctx, cx, headCY, headRY, breathOffset) {
    const bodyTop = headCY + headRY + 22 + breathOffset
    const macbookY = bodyTop + 50
    const macbookW = canvasWidth * 0.25
    const macbookH = 18

    // 屏幕
    ctx.beginPath()
    ctx.rect(cx - macbookW / 2, macbookY - macbookH, macbookW, macbookH)
    ctx.setFillStyle('#c0c0c0')
    ctx.fill()
    ctx.setStrokeStyle('#888888')
    ctx.setLineWidth(1)
    ctx.stroke()

    // 键盘底座
    ctx.beginPath()
    ctx.rect(cx - macbookW / 2 - 5, macbookY, macbookW + 10, 8)
    ctx.setFillStyle('#a0a0a0')
    ctx.fill()
  }

  // ===== 动画相关 =====

  /**
   * 更新呼吸相位
   */
  function updateBreath(dt) {
    const period = BREATH_PERIOD / breathingSpeed
    state.breathPhase += (dt / period) * 2 * Math.PI
    if (state.breathPhase > 2 * Math.PI) {
      state.breathPhase -= 2 * Math.PI
    }
  }

  /**
   * 更新眨眼状态
   */
  function updateBlink(now) {
    if (!blinkingEnabled) {
      state.blinkPhase = 0
      return
    }

    if (!state.isBlinking) {
      if (now >= nextBlinkTime) {
        state.isBlinking = true
        blinkStartTime = now
      }
    } else {
      const elapsed = now - blinkStartTime
      const halfDuration = BLINK_DURATION / 2

      if (elapsed < halfDuration) {
        // 闭眼阶段: 0 -> 1
        state.blinkPhase = elapsed / halfDuration
      } else if (elapsed < BLINK_DURATION) {
        // 睁眼阶段: 1 -> 0
        state.blinkPhase = 1 - (elapsed - halfDuration) / halfDuration
      } else {
        // 眨眼完成
        state.blinkPhase = 0
        state.isBlinking = false
        nextBlinkTime = now + randomBlinkInterval()
      }
    }
  }

  /**
   * 随机眨眼间隔（3-5秒）
   */
  function randomBlinkInterval() {
    return BLINK_INTERVAL_MIN + Math.random() * (BLINK_INTERVAL_MAX - BLINK_INTERVAL_MIN)
  }

  /**
   * 动画循环
   */
  function animationLoop(timestamp) {
    if (!state.isRunning) return

    const now = timestamp || Date.now()
    if (lastTime === 0) {
      lastTime = now
      nextBlinkTime = now + randomBlinkInterval()
    }

    const dt = now - lastTime
    lastTime = now

    // 更新状态
    updateBreath(dt)
    updateBlink(now)

    // 绘制
    if (canvasContext) {
      drawAvatar(canvasContext, config, state)
      canvasContext.draw()
    }

    // 继续循环
    animationFrameId = requestAnimationFrame(animationLoop)
  }

  /**
   * 启动动画
   */
  function startAnimation() {
    if (state.isRunning) return
    state.isRunning = true
    lastTime = 0
    animationFrameId = requestAnimationFrame(animationLoop)
  }

  /**
   * 停止动画
   */
  function stopAnimation() {
    state.isRunning = false
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
    lastTime = 0
  }

  /**
   * 清理资源
   */
  function dispose() {
    stopAnimation()
    canvasContext = null
  }

  return {
    // 状态
    state,
    config,

    // 方法
    initCanvas,
    setConfig,
    playMouth,
    setEmotion,
    setIdle,
    setThinking,
    setSpeaking,
    startAnimation,
    stopAnimation,
    dispose,

    // 直接绘制（用于手动调用）
    drawAvatar
  }
}
