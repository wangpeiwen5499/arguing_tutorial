<template>
  <view class="radar-wrap">
    <canvas
      :id="canvasId"
      :canvas-id="canvasId"
      type="2d"
      class="radar-canvas"
      :style="{ width: canvasSize + 'px', height: canvasSize + 'px' }"
    ></canvas>
  </view>
</template>

<script setup lang="ts">
import { onMounted, watch, nextTick } from 'vue'

interface DimensionScore {
  label: string
  score: number // 0-100
}

const props = withDefaults(defineProps<{
  dimensions?: DimensionScore[]
  size?: number
}>(), {
  dimensions: () => [
    { label: '逻辑性', score: 0 },
    { label: '情绪控制', score: 0 },
    { label: '说服力', score: 0 },
    { label: '策略运用', score: 0 },
    { label: '表达清晰度', score: 0 },
  ],
  size: 280,
})

const canvasId = 'scoreRadar'
const canvasSize = props.size
const dpr = uni.getSystemInfoSync().pixelRatio || 2

/** 获取五边形某个顶点坐标（从顶部开始顺时针） */
function getVertex(cx: number, cy: number, radius: number, index: number, total: number) {
  // 从顶部 (-90度) 开始，顺时针
  const angle = (Math.PI * 2 * index) / total - Math.PI / 2
  return {
    x: cx + radius * Math.cos(angle),
    y: cy + radius * Math.sin(angle),
  }
}

/** 绘制多边形路径 */
function drawPolygonPath(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  radius: number,
  sides: number
) {
  ctx.beginPath()
  for (let i = 0; i < sides; i++) {
    const v = getVertex(cx, cy, radius, i, sides)
    if (i === 0) ctx.moveTo(v.x, v.y)
    else ctx.lineTo(v.x, v.y)
  }
  ctx.closePath()
}

/** 主绘制逻辑 */
function draw(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d') as CanvasRenderingContext2D
  const w = canvas.width
  const h = canvas.height
  const cx = w / 2
  const cy = h / 2
  const maxRadius = Math.min(cx, cy) * 0.65 // 留空间给标签
  const sides = props.dimensions.length
  const gridLayers = 3

  // 清除画布
  ctx.clearRect(0, 0, w, h)

  // 底色
  ctx.fillStyle = '#1a1a2e'
  ctx.fillRect(0, 0, w, h)

  // 绘制 3 层网格五边形
  for (let layer = 1; layer <= gridLayers; layer++) {
    const r = (maxRadius * layer) / gridLayers
    drawPolygonPath(ctx, cx, cy, r, sides)
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)'
    ctx.lineWidth = 1
    ctx.stroke()
  }

  // 绘制从中心到顶点的轴线
  for (let i = 0; i < sides; i++) {
    const v = getVertex(cx, cy, maxRadius, i, sides)
    ctx.beginPath()
    ctx.moveTo(cx, cy)
    ctx.lineTo(v.x, v.y)
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)'
    ctx.lineWidth = 1
    ctx.stroke()
  }

  // 绘制数据区域
  ctx.beginPath()
  for (let i = 0; i < sides; i++) {
    const score = Math.max(0, Math.min(100, props.dimensions[i].score))
    const r = (score / 100) * maxRadius
    const v = getVertex(cx, cy, r, i, sides)
    if (i === 0) ctx.moveTo(v.x, v.y)
    else ctx.lineTo(v.x, v.y)
  }
  ctx.closePath()

  // 半透明填充
  ctx.fillStyle = 'rgba(76, 175, 80, 0.35)'
  ctx.fill()

  // 数据边框
  ctx.strokeStyle = '#4CAF50'
  ctx.lineWidth = 2
  ctx.stroke()

  // 绘制数据点
  for (let i = 0; i < sides; i++) {
    const score = Math.max(0, Math.min(100, props.dimensions[i].score))
    const r = (score / 100) * maxRadius
    const v = getVertex(cx, cy, r, i, sides)
    ctx.beginPath()
    ctx.arc(v.x, v.y, 4, 0, Math.PI * 2)
    ctx.fillStyle = '#4CAF50'
    ctx.fill()
  }

  // 绘制标签文字
  const fontSize = Math.round(12 * dpr)
  ctx.font = `${fontSize}px sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  for (let i = 0; i < sides; i++) {
    const v = getVertex(cx, cy, maxRadius + 24 * dpr, i, sides)
    ctx.fillStyle = '#ffffff'
    ctx.fillText(props.dimensions[i].label, v.x, v.y)
  }
}

/** 初始化 canvas 并绘制 */
async function initCanvas() {
  await nextTick()
  try {
    const query = uni.createSelectorQuery()
    query
      .select(`#${canvasId}`)
      .fields({ node: true, size: true } as any)
      .exec((res) => {
        if (!res || !res[0]) return
        const canvas = res[0].node
        if (!canvas) return

        canvas.width = canvasSize * dpr
        canvas.height = canvasSize * dpr

        const ctx = canvas.getContext('2d')
        ctx.scale(dpr, dpr)

        draw(canvas)
      })
  } catch (e) {
    console.error('ScoreRadar initCanvas error:', e)
  }
}

onMounted(() => {
  initCanvas()
})

watch(
  () => props.dimensions.map((d) => d.score),
  () => {
    initCanvas()
  },
  { deep: true }
)
</script>

<style scoped>
.radar-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
}

.radar-canvas {
  /* 尺寸通过 style 绑定 */
}
</style>
