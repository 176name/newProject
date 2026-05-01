<template>
  <div class="screenshots-container">
    <div class="header">
      <h2>📸 告警截图</h2>
      <button class="back-btn" @click="$router.back()">返回</button>
    </div>

    <div class="tabs">
      <button :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部</button>
      <button :class="{ active: activeTab === 'stranger' }" @click="activeTab = 'stranger'">陌生人闯入</button>
      <button :class="{ active: activeTab === 'fall' }" @click="activeTab = 'fall'">摔倒检测</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredImages.length === 0" class="empty">
      <span class="empty-icon">📭</span>
      <p>暂无告警截图</p>
    </div>
    <div class="screenshot-list" v-else>
      <div v-for="img in filteredImages" :key="img.name" class="screenshot-item">
        <img :src="img.url" :alt="img.name" @error="handleImageError" />
        <div class="info">
          <span class="type" :class="img.type === 'stranger' ? 'stranger' : 'fall'">
            {{ img.type === 'stranger' ? '陌生人' : '摔倒' }}
          </span>
          <span class="time">{{ img.time }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const API_BASE = 'http://' + window.location.hostname + ':8081'

const images = ref([])
const loading = ref(true)
const activeTab = ref('all')

const filteredImages = computed(() => {
  if (activeTab.value === 'all') return images.value
  return images.value.filter(img => img.type === activeTab.value)
})

const loadScreenshots = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE}/api/screenshots/list`)
    if (res.data.code === 200) {
      images.value = res.data.data || []
    }
  } catch (err) {
    console.error('加载截图失败:', err)
  } finally {
    loading.value = false
  }
}

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%" viewBox="0 0 24 24" fill="none" stroke="%23999"%3E%3Crect x="2" y="2" width="20" height="20" rx="2" stroke-width="1.5"/%3E%3Cpath d="M8 10h.01M16 10h.01M12 15h.01" stroke-width="1.5"/%3E%3C/svg%3E'
}

onMounted(() => {
  loadScreenshots()
})
</script>

<style scoped>
.screenshots-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.back-btn { background: #6c7a8e; color: white; border: none; padding: 8px 16px; border-radius: 8px; cursor: pointer; }
.tabs { display: flex; gap: 10px; margin-bottom: 20px; }
.tabs button { padding: 6px 16px; border: 1px solid #ddd; background: white; border-radius: 20px; cursor: pointer; transition: all 0.2s; }
.tabs button.active { background: #42b983; color: white; border-color: #42b983; }
.screenshot-list { display: flex; flex-wrap: wrap; gap: 20px; }
.screenshot-item { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 320px; transition: transform 0.2s; }
.screenshot-item:hover { transform: translateY(-4px); }
.screenshot-item img { width: 100%; height: 200px; object-fit: cover; background: #f0f0f0; }
.info { padding: 12px; display: flex; justify-content: space-between; align-items: center; }
.type { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.type.stranger { background: rgba(255,77,79,0.15); color: #ff4d4f; }
.type.fall { background: rgba(255,153,0,0.15); color: #ff9900; }
.time { font-size: 11px; color: #8a99aa; }
.loading, .empty { text-align: center; padding: 60px; color: #8a99aa; }
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
</style>
