<template>
  <div class="playback-container">
    <!-- Tab 切换栏 -->
    <div class="tabs">
      <button
          class="tab-btn"
          :class="{ active: activeTab === 'auto' }"
          @click="activeTab = 'auto'; searchRecords()"
      >
        <span class="tab-icon">🔄</span>
        <span>全天录像</span>
        <span class="tab-count">{{ autoTotal }}</span>
      </button>
      <button
          class="tab-btn"
          :class="{ active: activeTab === 'manual' }"
          @click="activeTab = 'manual'; searchRecords()"
      >
        <span class="tab-icon">✋</span>
        <span>手动录制</span>
        <span class="tab-count">{{ manualTotal }}</span>
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label>选择设备</label>
        <select v-model="selectedDeviceId" class="filter-select" @change="searchRecords">
          <option value="">所有设备</option>
          <option v-for="d in devices" :key="d.id" :value="d.id">{{ d.name }} ({{ d.ip }})</option>
        </select>
      </div>
      <div class="filter-item">
        <label>开始日期</label>
        <input type="date" v-model="startDate" class="filter-date" />
      </div>
      <div class="filter-item">
        <label>结束日期</label>
        <input type="date" v-model="endDate" class="filter-date" />
      </div>
      <button class="search-btn" @click="searchRecords">🔍 查询</button>
      <button class="refresh-btn" @click="refreshData">🔄 刷新</button>
    </div>

    <!-- 主内容区 -->
    <div class="playback-main">
      <!-- 左侧录像列表 -->
      <div class="record-list">
        <div class="list-header">
          <span>📼 {{ activeTab === 'auto' ? '全天录像' : '手动录制' }}</span>
          <span class="record-count">共 {{ totalRecords }} 条</span>
        </div>
        <div class="list-content">
          <div
              v-for="record in records"
              :key="record.id"
              class="record-item"
              :class="{ active: currentRecord?.id === record.id }"
              @click="selectRecord(record)"
          >
            <div class="record-time">{{ formatDateTime(record.start_time) }}</div>
            <div class="record-duration">{{ formatDuration(record.duration_seconds) }}</div>
            <div class="record-size">{{ formatFileSize(record.file_size) }}</div>
            <button class="delete-record" @click.stop="deleteRecord(record.id, record.device_name)">🗑️</button>
          </div>
          <div v-if="loading" class="empty-tip">
            <span>⏳</span>
            <p>加载中...</p>
          </div>
          <div v-else-if="records.length === 0" class="empty-tip">
            <span>📭</span>
            <p>暂无{{ activeTab === 'auto' ? '全天' : '手动' }}录像数据</p>
            <p class="empty-tip-small">请尝试修改筛选条件</p>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pagination">
          <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">上一页</button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages">下一页</button>
        </div>
      </div>

      <!-- 右侧播放器 -->
      <div class="player-area">
        <div class="player-wrapper">
          <video
              v-if="currentRecord && videoUrl"
              :src="videoUrl"
              controls
              autoplay
              class="video-player"
              @error="onVideoError"
          ></video>
          <div v-else-if="!currentRecord" class="player-placeholder">
            <span>🎬</span>
            <p>请从左侧选择录像文件</p>
          </div>
          <div v-else-if="currentRecord && !currentRecord.file_exists" class="player-placeholder">
            <span>⚠️</span>
            <p>视频文件不存在</p>
            <p class="empty-tip-small">文件路径: {{ currentRecord.file_path }}</p>
          </div>
        </div>
        <div class="player-info" v-if="currentRecord">
          <div class="info-row">
            <span class="label">设备名称：</span>
            <span>{{ currentRecord.device_name || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">录制类型：</span>
            <span>{{ currentRecord.record_type === 'auto' ? '🔄 全天录像' : '✋ 手动录制' }}</span>
          </div>
          <div class="info-row">
            <span class="label">录制时间：</span>
            <span>{{ formatDateTime(currentRecord.start_time) }}</span>
          </div>
          <div class="info-row">
            <span class="label">结束时间：</span>
            <span>{{ formatDateTime(currentRecord.end_time) }}</span>
          </div>
          <div class="info-row">
            <span class="label">时长：</span>
            <span>{{ formatDuration(currentRecord.duration_seconds) }}</span>
          </div>
          <div class="info-row">
            <span class="label">文件大小：</span>
            <span>{{ formatFileSize(currentRecord.file_size) }}</span>
          </div>
          <div class="info-row">
            <span class="label">文件路径：</span>
            <span class="file-path">{{ currentRecord.file_path || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click="closeDeleteModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="modal-close" @click="closeDeleteModal">×</button>
        </div>
        <div class="modal-body">
          <p>确定要删除录像 <strong>{{ deleteRecordName }}</strong> 吗？</p>
          <p class="warning-text">⚠️ 删除后无法恢复，请谨慎操作</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeDeleteModal">取消</button>
          <button class="confirm-delete-btn" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 提示 -->
    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request.js'

// ========== 状态 ==========
const activeTab = ref('auto')
const devices = ref([])
const selectedDeviceId = ref('')
const startDate = ref('')
const endDate = ref('')
const records = ref([])
const currentRecord = ref(null)
const videoUrl = ref('')
const loading = ref(false)
const totalRecords = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const totalPages = ref(0)

// 各类型的总数
const autoTotal = ref(0)
const manualTotal = ref(0)

// 删除弹窗
const showDeleteModal = ref(false)
const deleteRecordId = ref(null)
const deleteRecordName = ref('')
const toastMsg = ref('')
const serverIp = ref('')

// ========== 工具方法 ==========
const showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 3000)
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  try {
    // 手动解析避免浏览器时区歧义（后端返回的是本地时间，无时区标记）
    const parts = dateStr.split('T')
    const d = parts[0].split('-')
    const t = (parts[1] || '00:00:00').split(':')
    const y = parseInt(d[0]), m = parseInt(d[1]) - 1, day = parseInt(d[2])
    const h = parseInt(t[0]), min = parseInt(t[1]), sec = parseInt((t[2] || '0').split('.')[0])
    return `${y}-${String(m + 1).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  } catch {
    return dateStr
  }
}

const formatDuration = (seconds) => {
  if (!seconds) return '0秒'
  if (seconds < 60) return `${seconds}秒`
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins < 60) return `${mins}分${secs}秒`
  const hours = Math.floor(mins / 60)
  const minutes = mins % 60
  return `${hours}小时${minutes}分${secs}秒`
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '-'
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + sizes[i]
}

// ========== 数据加载 ==========
const loadDevices = async () => {
  try {
    const res = await request.get('/api/playback/devices')
    if (res.data.code === 200) {
      devices.value = res.data.data || []
    }
  } catch (err) {
    console.error('加载设备列表失败:', err)
    devices.value = []
  }
}

const searchRecords = async () => {
  loading.value = true
  currentPage.value = 1
  try {
    const params = {
      deviceId: selectedDeviceId.value || 0,
      recordType: activeTab.value,
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await request.get('/api/video/record/history/byType', { params })

    if (res.data.code === 200) {
      const data = res.data.data
      records.value = data.list || []
      totalRecords.value = data.total || 0
      totalPages.value = Math.ceil(totalRecords.value / pageSize.value)

      // 更新对应的总数
      if (activeTab.value === 'auto') {
        autoTotal.value = totalRecords.value
      } else {
        manualTotal.value = totalRecords.value
      }

      if (records.value.length > 0 && !currentRecord.value) {
        selectRecord(records.value[0])
      }
    } else {
      showToast(res.data.msg || '查询失败')
    }
  } catch (err) {
    console.error('查询录像失败:', err)
    showToast('查询失败: ' + (err.response?.data?.msg || err.message))
  } finally {
    loading.value = false
  }
}

const goToPage = async (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loading.value = true

  try {
    const params = {
      deviceId: selectedDeviceId.value || 0,
      recordType: activeTab.value,
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await request.get('/api/video/record/history/byType', { params })

    if (res.data.code === 200) {
      records.value = res.data.data.list || []
      if (currentRecord.value && !records.value.find(r => r.id === currentRecord.value.id)) {
        currentRecord.value = null
        videoUrl.value = ''
      }
    }
  } catch (err) {
    console.error('翻页失败:', err)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  searchRecords()
}

// ========== 录像播放 ==========
const selectRecord = async (record) => {
  currentRecord.value = record

  if (record.file_path && serverIp.value) {
    let relativePath = record.file_path.replace('/opt/media/bin/www/record/', '')
    videoUrl.value = `http://${serverIp.value}:8888/record/${relativePath}`
    console.log('播放地址:', videoUrl.value)
  } else if (record.file_path) {
    showToast('正在获取服务器信息...')
  } else {
    videoUrl.value = ''
    showToast('该录像没有文件路径')
  }
}

const onVideoError = () => {
  showToast('视频加载失败，文件可能已损坏或格式不支持')
}

// ========== 删除录像 ==========
const deleteRecord = (id, name) => {
  deleteRecordId.value = id
  deleteRecordName.value = name
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  try {
    const res = await request.delete(`/api/video/record/${deleteRecordId.value}`)
    if (res.data.code === 200) {
      showToast('删除成功')
      await searchRecords()
      if (currentRecord.value?.id === deleteRecordId.value) {
        currentRecord.value = null
        videoUrl.value = ''
      }
    } else {
      showToast(res.data.msg || '删除失败')
    }
  } catch (err) {
    console.error('删除失败:', err)
    showToast('删除失败: ' + (err.response?.data?.msg || err.message))
  } finally {
    closeDeleteModal()
  }
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  deleteRecordId.value = null
  deleteRecordName.value = ''
}

// ========== 生命周期 ==========
const fetchServerIp = async () => {
  try {
    const res = await request.get('/api/playback/system/ip')
    if (res.data.code === 200) {
      serverIp.value = res.data.data
    }
  } catch (err) {
    console.error('获取服务器IP失败:', err)
    serverIp.value = '192.168.6.145' // fallback
  }
}

onMounted(async () => {
  await fetchServerIp()
  loadDevices()
  searchRecords()
})
</script>

<style scoped>
.playback-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

/* Tab 样式 */
.tabs {
  display: flex;
  gap: 12px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  padding: 6px;
  flex-shrink: 0;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: none;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  color: #6c7a8e;
}

.tab-btn .tab-icon {
  font-size: 18px;
}

.tab-count {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 12px;
}

.tab-btn.active {
  background: linear-gradient(135deg, #42b983, #2d8f64);
  color: white;
}

.tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
}

.filter-bar {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  padding: 16px;
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item label {
  font-size: 12px;
  font-weight: 600;
  color: #6c7a8e;
}

.filter-select, .filter-date {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  min-width: 160px;
}

.search-btn, .refresh-btn {
  padding: 8px 24px;
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  cursor: pointer;
}

.refresh-btn {
  background: #6c7a8e;
}

.playback-main {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
}

.record-list {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}

.record-count {
  font-size: 12px;
  color: #42b983;
}

.list-content {
  flex: 1;
  overflow-y: auto;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.record-item:hover {
  background: rgba(66, 185, 131, 0.05);
}

.record-item.active {
  background: rgba(66, 185, 131, 0.1);
  border-left: 3px solid #42b983;
}

.record-time {
  flex: 1;
  font-size: 13px;
}

.record-duration, .record-size {
  font-size: 11px;
  color: #6c7a8e;
  width: 70px;
}

.delete-record {
  background: none;
  border: none;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 14px;
}

.record-item:hover .delete-record {
  opacity: 1;
}

.delete-record:hover {
  color: #ef4444;
}

.player-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.player-wrapper {
  flex: 1;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.video-player {
  width: 100%;
  height: 100%;
  max-height: 400px;
  object-fit: contain;
}

.player-placeholder {
  text-align: center;
  color: #6c7a8e;
}

.player-placeholder span {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.player-info {
  padding: 12px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  background: #f8fafd;
}

.info-row {
  font-size: 12px;
  margin-bottom: 6px;
  display: flex;
}

.info-row .label {
  color: #6c7a8e;
  width: 80px;
  flex-shrink: 0;
}

.file-path {
  font-family: monospace;
  font-size: 11px;
  word-break: break-all;
  color: #4a5a6e;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #6c7a8e;
}

.empty-tip span {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.empty-tip-small {
  font-size: 12px;
  margin-top: 8px;
  opacity: 0.7;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.pagination button {
  padding: 6px 12px;
  background: #f0f4f9;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #4a5a6e;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 400px;
  max-width: 90%;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}

.modal-body {
  padding: 20px;
}

.warning-text {
  font-size: 12px;
  color: #ef4444;
  margin-top: 8px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 8px 20px;
  background: #f0f4f9;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
}

.confirm-delete-btn {
  padding: 8px 20px;
  background: #ef4444;
  border: none;
  border-radius: 6px;
  color: white;
  cursor: pointer;
}

.toast {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: #1a2c3e;
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  z-index: 1001;
  animation: fadeOut 2s ease forwards;
}

@keyframes fadeOut {
  0% { opacity: 1; }
  70% { opacity: 1; }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}
</style>
