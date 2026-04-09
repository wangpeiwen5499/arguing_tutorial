<template>
  <view class="container">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">报告生成中...</text>
    </view>

    <template v-else-if="report">
      <scroll-view scroll-y class="scroll-area">
        <!-- 顶部总分区域 -->
        <view class="score-header">
          <text class="score-label">综合得分</text>
          <view class="score-row">
            <text class="score-number">{{ report.totalScore }}</text>
            <view v-if="report.scoreDiff !== 0" class="score-diff">
              <text :class="['diff-text', report.scoreDiff > 0 ? 'diff-up' : 'diff-down']">
                {{ report.scoreDiff > 0 ? '+' : '' }}{{ report.scoreDiff }}
              </text>
            </view>
          </view>
          <text v-if="report.scoreDiff !== 0" class="diff-hint">
            较上次 {{ report.scoreDiff > 0 ? '提升' : '下降' }} {{ Math.abs(report.scoreDiff) }} 分
          </text>
        </view>

        <!-- 雷达图 -->
        <view class="radar-section">
          <ScoreRadar :dimensions="radarDimensions" :size="260" />
        </view>

        <!-- 做得好的 -->
        <view v-if="report.strengths && report.strengths.length" class="section">
          <text class="section-title section-title-green">做得好的</text>
          <view
            v-for="(item, idx) in report.strengths"
            :key="'s' + idx"
            class="card card-green"
          >
            <text class="card-text">{{ item }}</text>
          </view>
        </view>

        <!-- 可以改进的 -->
        <view v-if="report.improvements && report.improvements.length" class="section">
          <text class="section-title section-title-orange">可以改进的</text>
          <view
            v-for="(item, idx) in report.improvements"
            :key="'i' + idx"
            class="card card-orange"
          >
            <text class="card-text">{{ item }}</text>
          </view>
        </view>

        <!-- 每轮复盘 -->
        <view v-if="report.rounds && report.rounds.length" class="section">
          <text class="section-title">每轮复盘</text>
          <view
            v-for="(round, rIdx) in report.rounds"
            :key="'r' + rIdx"
            class="round-card"
          >
            <view class="round-header" @tap="toggleRound(rIdx)">
              <text class="round-title">第 {{ round.roundNumber || rIdx + 1 }} 轮</text>
              <text class="round-score">{{ round.score }} 分</text>
              <text class="round-arrow" :class="{ 'arrow-open': openRounds[rIdx] }">&#9662;</text>
            </view>
            <view v-if="openRounds[rIdx]" class="round-body">
              <text class="round-comment">{{ round.comment }}</text>
              <view v-if="round.tags && round.tags.length" class="round-tags">
                <text v-for="(tag, tIdx) in round.tags" :key="tIdx" class="round-tag">{{ tag }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 底部间距 -->
        <view class="bottom-spacer"></view>
      </scroll-view>

      <!-- 底部按钮 -->
      <view class="bottom-bar">
        <button class="bottom-btn btn-retry" @tap="onRetry">再来一次</button>
        <button class="bottom-btn btn-share" @tap="onShare">分享战绩</button>
      </view>

      <!-- 分享卡片弹窗 -->
      <view v-if="showShareCard" class="share-modal" @tap="showShareCard = false">
        <view class="share-modal-content" @tap.stop>
          <ShareCard
            :sessionId="sessionId"
            :sceneName="report.sceneName || '对练场景'"
            :totalScore="report.totalScore"
          />
          <view class="share-close" @tap="showShareCard = false">
            <text class="close-text">关闭</text>
          </view>
        </view>
      </view>
    </template>

    <!-- 错误状态 -->
    <view v-else class="error-wrap">
      <text class="error-text">报告加载失败</text>
      <button class="retry-btn" @tap="loadReport">重新加载</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getReport } from '@/api/report'
import ScoreRadar from '@/components/ScoreRadar.vue'
import ShareCard from '@/components/ShareCard.vue'

// ===== 类型 =====
interface RoundReview {
  roundNumber: number
  score: number
  comment: string
  tags?: string[]
}

interface ReportData {
  totalScore: number
  scoreDiff: number
  sceneId: number | string
  sceneName: string
  logicScore: number
  emotionScore: number
  persuasionScore: number
  strategyScore: number
  clarityScore: number
  strengths: string[]
  improvements: string[]
  rounds: RoundReview[]
}

// ===== 状态 =====
const sessionId = ref<string>('')
const loading = ref(true)
const report = ref<ReportData | null>(null)
const openRounds = reactive<Record<number, boolean>>({})
const showShareCard = ref(false)

// ===== 计算属性 =====
const radarDimensions = computed(() => {
  if (!report.value) return []
  return [
    { label: '逻辑性', score: report.value.logicScore || 0 },
    { label: '情绪控制', score: report.value.emotionScore || 0 },
    { label: '说服力', score: report.value.persuasionScore || 0 },
    { label: '策略运用', score: report.value.strategyScore || 0 },
    { label: '表达清晰度', score: report.value.clarityScore || 0 },
  ]
})

// ===== 生命周期 =====
onLoad((options: any) => {
  if (options?.sessionId) {
    sessionId.value = options.sessionId
    loadReport()
  }
})

// ===== 方法 =====
async function loadReport() {
  if (!sessionId.value) return
  loading.value = true
  try {
    const res: any = await getReport(sessionId.value)
    const data = res.data || res
    report.value = {
      totalScore: data.totalScore ?? data.total_score ?? 0,
      scoreDiff: data.scoreDiff ?? data.score_diff ?? 0,
      sceneId: data.sceneId ?? data.scene_id ?? '',
      sceneName: data.sceneName ?? data.scene_name ?? '对练场景',
      logicScore: data.logicScore ?? data.logic_score ?? 0,
      emotionScore: data.emotionScore ?? data.emotion_score ?? 0,
      persuasionScore: data.persuasionScore ?? data.persuasion_score ?? 0,
      strategyScore: data.strategyScore ?? data.strategy_score ?? 0,
      clarityScore: data.clarityScore ?? data.clarity_score ?? 0,
      strengths: data.strengths || [],
      improvements: data.improvements || [],
      rounds: (data.rounds || []).map((r: any) => ({
        roundNumber: r.roundNumber ?? r.round_number ?? 0,
        score: r.score ?? 0,
        comment: r.comment ?? '',
        tags: r.tags || [],
      })),
    }
  } catch (err) {
    console.error('加载报告失败:', err)
    report.value = null
  } finally {
    loading.value = false
  }
}

function toggleRound(index: number) {
  openRounds[index] = !openRounds[index]
}

function onRetry() {
  if (!report.value) return
  uni.navigateTo({
    url: `/pages/practice/index?sceneId=${report.value.sceneId}`
  })
}

function onShare() {
  showShareCard.value = true
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #1a1a2e;
  color: #ffffff;
}

/* 加载状态 */
.loading-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
}

/* 滚动区域 */
.scroll-area {
  flex: 1;
  height: 0;
}

/* 顶部总分 */
.score-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx 24rpx;
}

.score-label {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 12rpx;
}

.score-row {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 16rpx;
}

.score-number {
  font-size: 96rpx;
  font-weight: bold;
  color: #ffffff;
  line-height: 1;
}

.score-diff {
  display: flex;
  align-items: center;
}

.diff-text {
  font-size: 32rpx;
  font-weight: bold;
}

.diff-up {
  color: #4CAF50;
}

.diff-down {
  color: #ff6b6b;
}

.diff-hint {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 8rpx;
}

/* 雷达图 */
.radar-section {
  display: flex;
  justify-content: center;
  padding: 16rpx 0 32rpx;
}

/* 区块 */
.section {
  padding: 16rpx 32rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 6rpx solid #6c63ff;
}

.section-title-green {
  border-left-color: #4CAF50;
}

.section-title-orange {
  border-left-color: #FF9800;
}

/* 卡片 */
.card {
  padding: 24rpx 28rpx;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}

.card-green {
  background-color: rgba(76, 175, 80, 0.15);
  border-left: 6rpx solid #4CAF50;
}

.card-orange {
  background-color: rgba(255, 152, 0, 0.15);
  border-left: 6rpx solid #FF9800;
}

.card-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

/* 每轮复盘 */
.round-card {
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
}

.round-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 24rpx 28rpx;
}

.round-title {
  flex: 1;
  font-size: 28rpx;
  font-weight: 500;
  color: #ffffff;
}

.round-score {
  font-size: 28rpx;
  color: #4CAF50;
  font-weight: bold;
  margin-right: 16rpx;
}

.round-arrow {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.4);
  transition: transform 0.3s ease;
}

.arrow-open {
  transform: rotate(180deg);
}

.round-body {
  padding: 0 28rpx 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
}

.round-comment {
  display: block;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
  margin-top: 20rpx;
}

.round-tags {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.round-tag {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  background-color: rgba(108, 99, 255, 0.15);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

/* 底部间距 */
.bottom-spacer {
  height: 140rpx;
}

/* 底部按钮栏 */
.bottom-bar {
  display: flex;
  flex-direction: row;
  gap: 24rpx;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: rgba(26, 26, 46, 0.95);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
}

.bottom-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  color: #ffffff;
  border: none;
  border-radius: 44rpx;
  text-align: center;
  padding: 0;
  margin: 0;
}

.bottom-btn::after {
  border: none;
}

.btn-retry {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.btn-share {
  background: linear-gradient(135deg, #4CAF50, #388E3C);
}

/* 错误状态 */
.error-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 32rpx;
}

.error-text {
  font-size: 30rpx;
  color: rgba(255, 255, 255, 0.5);
}

.retry-btn {
  padding: 16rpx 48rpx;
  font-size: 28rpx;
  color: #ffffff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 32rpx;
  border: none;
}

.retry-btn::after {
  border: none;
}

/* 分享弹窗 */
.share-modal {
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

.share-modal-content {
  width: 85%;
  background-color: #2d2d5e;
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.share-close {
  margin-top: 24rpx;
  padding: 16rpx 48rpx;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 32rpx;
}

.close-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.7);
}
</style>
