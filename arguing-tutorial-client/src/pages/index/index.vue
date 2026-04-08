<template>
  <view class="container">
    <!-- 顶部 banner -->
    <view class="banner">
      <text class="banner-title">🔥 吵架修炼场</text>
      <text class="banner-sub">选择你的战场</text>
    </view>
    <!-- 分类 tab -->
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab', activeTab === tab.value ? 'tab-active' : '']"
        @click="onTabChange(tab.value)"
      >
        <text :class="['tab-text', activeTab === tab.value ? 'tab-text-active' : '']">{{ tab.label }}</text>
      </view>
    </view>
    <!-- 场景卡片列表 -->
    <scroll-view scroll-y class="scene-list">
      <view v-if="loading" class="loading">
        <text class="loading-text">加载中...</text>
      </view>
      <view v-else-if="filteredScenes.length === 0" class="empty">
        <text class="empty-text">暂无场景</text>
      </view>
      <SceneCard v-for="scene in filteredScenes" :key="scene.id" :scene="scene" />
    </scroll-view>
    <!-- 自定义场景入口 -->
    <view class="custom-entry" @click="onCustomScene">
      <text class="custom-entry-text">+ 自定义场景</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getSceneList } from '@/api/scene'
import { useGuest } from '@/composables/useGuest'
import SceneCard from '@/components/SceneCard.vue'

interface Scene {
  id: number | string
  name: string
  description: string
  category: string
  difficulty: number
}

const tabs = [
  { label: '全部', value: '' },
  { label: '职场', value: '职场' },
  { label: '技术', value: '技术' },
  { label: '日常', value: '日常' },
]

const activeTab = ref('')
const scenes = ref<Scene[]>([])
const loading = ref(false)

const { ensureToken } = useGuest()

const filteredScenes = computed(() => {
  if (!activeTab.value) return scenes.value
  return scenes.value.filter((s) => s.category === activeTab.value)
})

function onTabChange(value: string) {
  activeTab.value = value
}

function onCustomScene() {
  uni.navigateTo({ url: '/pages/practice/index?sceneId=custom' })
}

onMounted(async () => {
  loading.value = true
  try {
    await ensureToken()
    const res = await getSceneList()
    scenes.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    console.error('加载场景列表失败:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #1a1a2e;
}

/* Banner */
.banner {
  padding: 40rpx 40rpx 24rpx;
  background: linear-gradient(180deg, #16213e 0%, #1a1a2e 100%);
}

.banner-title {
  display: block;
  font-size: 52rpx;
  color: #ffffff;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.banner-sub {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.6);
}

/* Tabs */
.tabs {
  display: flex;
  flex-direction: row;
  padding: 16rpx 32rpx;
  gap: 20rpx;
}

.tab {
  padding: 12rpx 32rpx;
  border-radius: 40rpx;
  background-color: rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.tab-active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.4);
}

.tab-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.6);
  transition: color 0.3s ease;
}

.tab-text-active {
  color: #ffffff;
  font-weight: 500;
}

/* Scene List */
.scene-list {
  flex: 1;
  padding: 0 32rpx;
  height: 0;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80rpx 0;
}

.loading-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
}

.empty {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.4);
}

/* Custom Entry */
.custom-entry {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 24rpx 32rpx 40rpx;
  padding: 24rpx 0;
  border: 2rpx dashed rgba(255, 255, 255, 0.3);
  border-radius: 24rpx;
  transition: all 0.3s ease;
}

.custom-entry:active {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.5);
}

.custom-entry-text {
  font-size: 30rpx;
  color: rgba(255, 255, 255, 0.6);
}
</style>
