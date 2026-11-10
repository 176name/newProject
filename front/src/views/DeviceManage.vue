<template>
  <div class="device-manage-container">
    <!-- 标签页切换 -->
    <div class="tabs">
      <button
          class="tab-btn"
          :class="{ active: activeTab === 'current' }"
          @click="activeTab = 'current'"
      >
        <span class="tab-icon">📷</span>
        <span>当前设备</span>
        <span class="tab-count">{{ devices.length }}</span>
      </button>
      <button
          class="tab-btn"
          :class="{ active: activeTab === 'history' }"
          @click="activeTab = 'history'; loadHistoryDevices()"
      >
        <span class="tab-icon">🗑️</span>
        <span>历史设备</span>
        <span class="tab-count">{{ historyDevices.length }}</span>
      </button>
    </div>

    <!-- 当前设备列表 -->
    <div v-show="activeTab === 'current'" class="device-content">
      <div class="toolbar">
        <button class="add-device-btn" @click="openAddModal">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5V19M5 12H19"/>
          </svg>
          添加设备
        </button>
        <div class="search-box">
          <input
              type="text"
              v-model="searchKeyword"
              placeholder="搜索设备名称或IP..."
          />
          <span class="search-icon">🔍</span>
        </div>
      </div>

      <div class="device-table">
        <div class="table-header">
          <span class="col-id">序号</span>
          <span class="col-name">设备名称</span>
          <span class="col-ip">IP地址</span>
          <span class="col-brand">品牌</span>
          <span class="col-stream">流ID</span>
          <span class="col-status">状态</span>
          <span class="col-action">操作</span>
        </div>
        <div class="table-body">
          <div
              v-for="(device, idx) in filteredDevices"
              :key="device.id"
              class="table-row"
          >
            <span class="col-id">{{ idx + 1 }}</span>
            <span class="col-name">{{ device.name }}</span>
            <span class="col-ip">{{ device.ip }}</span>
            <span class="col-brand">{{ device.brand || '海康威视' }}</span>
            <span class="col-stream mono">{{ device.stream_id || '-' }}</span>
            <span class="col-status">
              <span class="status-badge" :class="device.status === 1 ? 'online' : 'offline'">
                {{ device.status === 1 ? '● 在线' : '○ 离线' }}
              </span>
            </span>
            <span class="col-action">
              <button class="action-btn edit" @click="openEditModal(device)" title="编辑">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 3L21 7M7 21H3V17L17 3L21 7L7 21Z"/>
                </svg>
              </button>
              <button class="action-btn delete" @click="deleteDevice(device.id, device.name)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 7H20M10 11V16M14 11V16M5 7L6 19C6 19.5304 6.21071 20.0391 6.58579 20.4142C6.96086 20.7893 7.46957 21 8 21H16C16.5304 21 17.0391 20.7893 17.4142 20.4142C17.7893 20.0391 18 19.5304 18 19L19 7M9 7V4C9 3.73478 9.10536 3.48043 9.29289 3.29289C9.48043 3.10536 9.73478 3 10 3H14C14.2652 3 14.5196 3.10536 14.7071 3.29289C14.8946 3.48043 15 3.73478 15 4V7"/>
                </svg>
              </button>
            </span>
          </div>
          <div v-if="filteredDevices.length === 0" class="empty-state">
            <span>📭</span>
            <p>暂无设备数据</p>
            <p class="empty-tip">点击"添加设备"按钮添加摄像头</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史设备列表（软删除的设备） -->
    <div v-show="activeTab === 'history'" class="device-content">
      <div class="toolbar">
        <div class="toolbar-title">
          <span>🗑️ 已删除设备</span>
        </div>
        <button class="refresh-btn" @click="loadHistoryDevices">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 4V10H17M1 20V14H7M3.51 9C4.66 5.63 7.99 3.13 12 3.13C16.01 3.13 19.34 5.63 20.49 9M20.49 15C19.34 18.37 16.01 20.87 12 20.87C7.99 20.87 4.66 18.37 3.51 15"/>
          </svg>
          刷新
        </button>
      </div>

      <div class="device-table">
        <div class="table-header">
          <span class="col-id">序号</span>
          <span class="col-name">设备名称</span>
          <span class="col-ip">IP地址</span>
          <span class="col-brand">品牌</span>
          <span class="col-time">删除时间</span>
          <span class="col-action">操作</span>
        </div>
        <div class="table-body">
          <div
              v-for="(device, idx) in historyDevices"
              :key="device.id"
              class="table-row"
          >
            <span class="col-id">{{ idx + 1 }}</span>
            <span class="col-name">{{ device.name }}</span>
            <span class="col-ip">{{ device.ip }}</span>
            <span class="col-brand">{{ device.brand || '海康威视' }}</span>
            <span class="col-time">{{ formatDateTime(device.deleted_at) || '--' }}</span>
            <span class="col-action">
              <button class="action-btn restore" @click="restoreDevice(device.id, device.name)" title="恢复">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 12C20 16.4 16.4 20 12 20C7.6 20 4 16.4 4 12C4 7.6 7.6 4 12 4C14.1 4 16.1 4.8 17.6 6.1"/>
                  <path d="M12 8V12L14 14"/>
                  <path d="M17 4L20 7L17 10"/>
                </svg>
              </button>
              <button class="action-btn permanent-delete" @click="permanentDelete(device.id, device.name)" title="彻底删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 7H20M10 11V16M14 11V16M5 7L6 19C6 19.5304 6.21071 20.0391 6.58579 20.4142C6.96086 20.7893 7.46957 21 8 21H16C16.5304 21 17.0391 20.7893 17.4142 20.4142C17.7893 20.0391 18 19.5304 18 19L19 7M9 7V4C9 3.73478 9.10536 3.48043 9.29289 3.29289C9.48043 3.10536 9.73478 3 10 3H14C14.2652 3 14.5196 3.10536 14.7071 3.29289C14.8946 3.48043 15 3.73478 15 4V7"/>
                </svg>
              </button>
            </span>
          </div>
          <div v-if="historyDevices.length === 0" class="empty-state">
            <span>📭</span>
            <p>暂无历史设备</p>
            <p class="empty-tip">删除的设备会出现在这里，可以恢复</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑设备弹窗 -->
    <div v-if="showDeviceModal" class="modal-overlay" @click="closeDeviceModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑设备' : '添加设备' }}</h3>
          <button class="modal-close" @click="closeDeviceModal">×</button>
        </div>
        <div class="modal-body">
          <div class="input-group">
            <label>设备名称 *</label>
            <input v-model="form.name" type="text" placeholder="例如：海康摄像头_主出入口" />
          </div>
          <div class="input-group">
            <label>IP地址 *</label>
            <input v-model="form.ip" type="text" placeholder="例如：192.168.1.100" />
          </div>
          <div class="input-group">
            <label>设备品牌</label>
            <select v-model="form.brand">
              <option value="海康威视">海康威视 (Hikvision)</option>
              <option value="大华">大华 (Dahua)</option>
              <option value="宇视">宇视 (Uniview)</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div class="input-group">
            <label>流ID (Stream ID)</label>
            <input v-model="form.stream_id" type="text" placeholder="自动生成，可不填" />
          </div>
          <div class="input-group">
            <label>RTSP用户名</label>
            <input v-model="form.username" type="text" placeholder="默认：admin" />
          </div>
          <div class="input-group">
            <label>RTSP密码</label>
            <input v-model="form.password" type="password" placeholder="摄像头登录密码" />
          </div>
          <div class="input-group">
            <label>通道号</label>
            <input v-model.number="form.channel" type="number" placeholder="默认：101" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeDeviceModal">取消</button>
          <button class="save-btn" @click="saveDevice" :disabled="saving">
            {{ saving ? '保存中...' : (isEditing ? '保存修改' : '添加设备') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 提示 -->
    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../api/request.js'

// ========== 状态 ==========
const activeTab = ref('current')
const devices = ref([])
const historyDevices = ref([])
const searchKeyword = ref('')
const showDeviceModal = ref(false)
const isEditing = ref(false)
const saving = ref(false)
const toastMsg = ref('')

// 表单
const form = ref({
  id: null,
  name: '',
  ip: '',
  brand: '海康威视',
  stream_id: '',
  username: 'admin',
  password: '',
  channel: 101,
  status: 0
})

// API 基础地址
const API_BASE = `http://${window.location.hostname}:8081/api/video`

// ========== 计算属性 ==========
const filteredDevices = computed(() => {
  if (!searchKeyword.value) return devices.value
  const kw = searchKeyword.value.toLowerCase()
  return devices.value.filter(d =>
      d.name?.toLowerCase().includes(kw) ||
      d.ip?.includes(kw)
  )
})

// ========== 通用方法 ==========
const showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 2000)
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
  } catch {
    return dateStr
  }
}

// ========== 加载数据 ==========
const loadDevices = async () => {
  try {
    const res = await request.get('/api/video/devices')
    if (res.data.code === 200) {
      devices.value = res.data.data || []
      console.log('加载设备成功:', devices.value.length)
    }
  } catch (err) {
    console.error('加载设备失败:', err)
    showToast('加载设备失败')
  }
}

const loadHistoryDevices = async () => {
  try {
    const res = await request.get('/api/video/devices/history')
    if (res.data.code === 200) {
      historyDevices.value = res.data.data || []
      console.log('加载历史设备成功:', historyDevices.value.length)
    }
  } catch (err) {
    console.error('加载历史设备失败:', err)
    historyDevices.value = []
  }
}

// ========== 设备操作 ==========
const closeDeviceModal = () => {
  showDeviceModal.value = false
}

const openAddModal = () => {
  isEditing.value = false
  form.value = {
    id: null,
    name: '',
    ip: '',
    brand: '海康威视',
    stream_id: '',
    username: 'admin',
    password: '',
    channel: 101,
    status: 0
  }
  showDeviceModal.value = true
}

const openEditModal = (device) => {
  isEditing.value = true
  form.value = { ...device }
  showDeviceModal.value = true
}

const saveDevice = async () => {
  if (!form.value.name || !form.value.ip) {
    showToast('请填写设备名称和IP地址')
    return
  }

  saving.value = true
  try {
    let res
    if (isEditing.value) {
      res = await request.put(`/api/video/device/${form.value.id}`, form.value)
      if (res.data.code === 200) {
        showToast('设备修改成功')
      }
    } else {
      res = await request.post('/api/video/device', form.value)
      if (res.data.code === 200) {
        showToast('设备添加成功')
      }
    }

    if (res.data.code === 200) {
      closeDeviceModal()
      await loadDevices()
    } else {
      showToast(res.data.msg || '操作失败')
    }
  } catch (err) {
    console.error('保存设备失败:', err)
    showToast('保存失败: ' + (err.response?.data?.msg || err.message))
  } finally {
    saving.value = false
  }
}

const deleteDevice = async (id, name) => {
  if (!confirm(`确定要删除设备"${name}"吗？删除后将移到历史记录`)) return

  try {
    const res = await request.delete(`/api/video/device/${id}`)
    if (res.data.code === 200) {
      showToast('设备已移至历史记录')
      await loadDevices()
      await loadHistoryDevices()
    } else {
      showToast(res.data.msg || '删除失败')
    }
  } catch (err) {
    console.error('删除设备失败:', err)
    showToast('删除失败')
  }
}

const restoreDevice = async (id, name) => {
  try {
    const res = await request.post(`/api/video/device/${id}/restore`)
    if (res.data.code === 200) {
      showToast(`设备"${name}"恢复成功`)
      await loadDevices()
      await loadHistoryDevices()
    } else {
      showToast(res.data.msg || '恢复失败')
    }
  } catch (err) {
    console.error('恢复设备失败:', err)
    showToast('恢复失败')
  }
}

const permanentDelete = async (id, name) => {
  if (!confirm(`彻底删除"${name}"后无法恢复，确定吗？`)) return

  try {
    const res = await request.delete(`/api/video/device/${id}/permanent`)
    if (res.data.code === 200) {
      showToast('设备已彻底删除')
      await loadHistoryDevices()
    } else {
      showToast(res.data.msg || '删除失败')
    }
  } catch (err) {
    console.error('彻底删除失败:', err)
    showToast('删除失败')
  }
}

// ========== 生命周期 ==========
onMounted(() => {
  loadDevices()
  loadHistoryDevices()
})
</script>

<style scoped>
/* 样式和你原来的一样，这里省略避免重复，你复制原来的样式即可 */
.device-manage-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}

/* 标签页 */
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

/* 内容区域 */
.device-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a2c3e;
}

.add-device-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  border-radius: 10px;
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.add-device-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.3);
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f0f4f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #4a5a6e;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: #e2e8f0;
}

.search-box {
  position: relative;
}

.search-box input {
  padding: 10px 36px 10px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  width: 240px;
  font-size: 14px;
  background: white;
  transition: all 0.3s;
}

.search-box input:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
}

.search-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.5;
}

/* 表格 */
.device-table {
  flex: 1;
  overflow: auto;
  padding: 0 20px 20px;
}

.table-header,
.table-row {
  display: grid;
  gap: 12px;
  padding: 14px 0;
  align-items: center;
}

/* 当前设备表格列宽 */
.device-table .table-header,
.device-table .table-row {
  grid-template-columns: 60px 1.5fr 1.2fr 1fr 1.2fr 100px 100px;
}

.table-header {
  color: #6c7a8e;
  font-size: 12px;
  font-weight: 600;
  border-bottom: 1px solid #e8ecf2;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.98);
}

.table-row {
  font-size: 13px;
  border-bottom: 1px solid #f0f4f9;
  transition: all 0.2s;
}

.table-row:hover {
  background: rgba(66, 185, 131, 0.03);
}

.col-id {
  color: #6c7a8e;
  font-weight: 500;
}

.col-name {
  font-weight: 500;
  color: #1a2c3e;
}

.col-ip {
  font-family: monospace;
  font-size: 12px;
  color: #4a5a6e;
}

.col-brand {
  color: #4a5a6e;
}

.col-stream {
  font-family: monospace;
  font-size: 11px;
  color: #6c7a8e;
}

.col-time {
  font-size: 12px;
  color: #6c7a8e;
}

.mono {
  font-family: monospace;
}

/* 状态标签 */
.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
}

.status-badge.online {
  background: rgba(66, 185, 131, 0.1);
  color: #42b983;
}

.status-badge.offline {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

/* 操作按钮 */
.action-btn {
  background: none;
  border: none;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  margin-right: 4px;
}

.action-btn.edit {
  color: #42b983;
}

.action-btn.edit:hover {
  background: rgba(66, 185, 131, 0.1);
  transform: scale(1.05);
}

.action-btn.delete {
  color: #ef4444;
}

.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.1);
  transform: scale(1.05);
}

.action-btn.restore {
  color: #42b983;
}

.action-btn.restore:hover {
  background: rgba(66, 185, 131, 0.1);
  transform: scale(1.05);
}

.action-btn.permanent-delete {
  color: #ef4444;
}

.action-btn.permanent-delete:hover {
  background: rgba(239, 68, 68, 0.1);
  transform: scale(1.05);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6c7a8e;
}

.empty-state span {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.empty-state p {
  margin-bottom: 8px;
}

.empty-tip {
  font-size: 12px;
  opacity: 0.7;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 20px;
  width: 480px;
  max-width: 90%;
  max-height: 85vh;
  overflow: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e8ecf2;
  background: #f8fafd;
}

.modal-header h3 {
  margin: 0;
  color: #1a2c3e;
  font-size: 18px;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6c7a8e;
  transition: color 0.2s;
}

.modal-close:hover {
  color: #ef4444;
}

.modal-body {
  padding: 24px;
}

.input-group {
  margin-bottom: 20px;
}

.input-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #4a5a6e;
  margin-bottom: 8px;
}

.input-group input,
.input-group select {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.3s;
}

.input-group input:focus,
.input-group select:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e8ecf2;
  background: #f8fafd;
}

.cancel-btn {
  padding: 10px 20px;
  background: #f0f4f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #e2e8f0;
}

.save-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.save-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.3);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 提示 */
.toast {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: #1a2c3e;
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 14px;
  z-index: 1001;
  animation: fadeOut 2s ease forwards;
}

@keyframes fadeOut {
  0% { opacity: 1; transform: translateX(-50%) translateY(0); }
  70% { opacity: 1; }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}

/* 滚动条 */
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
