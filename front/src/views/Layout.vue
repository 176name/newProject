<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <div class="navbar">
      <div class="navbar-brand">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span class="brand-name">HEALING STREAM</span>
        <span class="brand-version">V3.0 Pro</span>
      </div>

      <div class="navbar-menu">
        <router-link to="/monitor" class="nav-item" :class="{ active: $route.path === '/monitor' }">
          <span class="nav-icon">📹</span>
          <span>监控大屏</span>
        </router-link>
        <router-link to="/devices" class="nav-item" :class="{ active: $route.path === '/devices' }">
          <span class="nav-icon">📷</span>
          <span>设备管理</span>
        </router-link>
        <router-link to="/playback" class="nav-item" :class="{ active: $route.path === '/playback' }">
          <span class="nav-icon">🎬</span>
          <span>录像回放</span>
        </router-link>
        <router-link to="/traffic" class="nav-item" :class="{ active: $route.path === '/traffic' }">
          <span class="nav-icon">📊</span>
          <span>流量监控</span>
        </router-link>
        <router-link to="/ai-analysis" class="nav-item" :class="{ active: $route.path === '/ai-analysis' }">
          <span class="nav-icon">🤖</span>
          <span>AI分析</span>
        </router-link>
      </div>

      <div class="navbar-right">
        <div class="status-indicator">
          <span class="status-dot online"></span>
          <span>系统正常</span>
        </div>

        <div class="user-dropdown" ref="dropdownRef">
          <div class="user-info" @click="toggleDropdown">
            <span class="user-avatar">👨‍💻</span>
            <span>{{ userName }}</span>
            <span class="dropdown-arrow" :class="{ open: dropdownOpen }">▼</span>
          </div>
          <div class="dropdown-menu" v-show="dropdownOpen">
            <div class="dropdown-item" @click="goToUserCenter">
              <span>👤</span> 个人中心
            </div>
            <div class="dropdown-item" v-if="isAdmin" @click="goToUserManage">
              <span>👥</span> 用户管理
            </div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item" @click="handleLogout">
              <span>🚪</span> 退出登录
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 页面内容 -->
    <div class="page-content" :class="{ 'full-page': isFullPageRoute }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userName = ref('管理员')
const dropdownOpen = ref(false)
const dropdownRef = ref(null)
const isAdmin = ref(localStorage.getItem('role') === 'ADMIN')

const isFullPageRoute = computed(() => {
  const fullPageRoutes = ['/user-center', '/users']
  return fullPageRoutes.includes(route.path)
})

onMounted(() => {
  const savedName = localStorage.getItem('userName')
  if (savedName) {
    userName.value = savedName
  }
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const toggleDropdown = () => {
  dropdownOpen.value = !dropdownOpen.value
}

const handleClickOutside = (event) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    dropdownOpen.value = false
  }
}

const goToUserCenter = () => {
  dropdownOpen.value = false
  router.push('/user-center')
}

const goToUserManage = () => {
  dropdownOpen.value = false
  router.push('/users')
}

const handleLogout = () => {
  dropdownOpen.value = false
  localStorage.removeItem('token')
  localStorage.removeItem('isLogin')
  localStorage.removeItem('userName')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #e8f0fe 0%, #d4e4fc 100%);
  overflow: hidden;
}

.navbar {
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid rgba(66, 185, 131, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  z-index: 10;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border-radius: 8px;
  padding: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-size: 18px;
  font-weight: bold;
  color: #1a2c3e;
  letter-spacing: 1px;
}

.brand-version {
  font-size: 10px;
  color: #42b983;
  background: rgba(66, 185, 131, 0.1);
  padding: 2px 6px;
  border-radius: 10px;
}

.navbar-menu {
  display: flex;
  gap: 8px;
  background: #f0f4f9;
  padding: 4px;
  border-radius: 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 8px;
  color: #6c7a8e;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.nav-item:hover {
  background: rgba(66, 185, 131, 0.1);
  color: #42b983;
}

.nav-item.active {
  background: #42b983;
  color: white;
}

.nav-icon {
  font-size: 16px;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #2d8f64;
  background: rgba(66, 185, 131, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #42b983;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.user-dropdown {
  position: relative;
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f0f4f9;
  border-radius: 20px;
  font-size: 13px;
  color: #4a5a6e;
  transition: all 0.2s;
}

.user-info:hover {
  background: #e8ecf2;
}

.user-avatar {
  font-size: 16px;
}

.dropdown-arrow {
  font-size: 10px;
  transition: transform 0.2s;
  color: #6c7a8e;
}

.dropdown-arrow.open {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 150px;
  overflow: hidden;
  z-index: 100;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 13px;
  color: #1a2c3e;
  transition: all 0.2s;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f0f4f9;
  color: #42b983;
}

.dropdown-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 4px 0;
}

.page-content {
  flex: 1;
  overflow: auto;
  position: relative;
}

.page-content:not(.full-page) {
  padding: 16px;
}

.page-content.full-page {
  padding: 0;
  overflow: auto;
}

:deep(.monitor-container),
:deep(.playback-container),
:deep(.device-container),
:deep(.traffic-container),
:deep(.ai-analysis-container),
:deep(.user-center-container) {
  min-height: 100%;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f0f4f9;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #42b983;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #2d8f64;
}
</style>
