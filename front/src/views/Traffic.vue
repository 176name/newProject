<template>
  <div class="traffic-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1>📊 流量监控</h1>
        <p class="subtitle">实时带宽监控与智能限流策略</p>
      </div>
      <div class="header-right">
        <div class="device-selector">
          <span>📹 选择设备：</span>
          <select v-model="selectedDeviceId" @change="onDeviceChange">
            <option value="all">所有设备</option>
            <option v-for="device in deviceList" :key="device.id" :value="device.id">
              {{ device.name }} ({{ device.ip || 'IP未知' }})
            </option>
          </select>
        </div>
        <div class="live-status">
          <span class="live-dot"></span>
          <span>实时更新</span>
          <span class="update-time">{{ lastUpdateTime }}</span>
          <span class="device-stats">活跃流: {{ streamData.length }} | 设备: {{ deviceList.length }}</span>
        </div>
      </div>
    </div>

    <!-- 带宽指标 -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon blue">📡</div>
        <div class="metric-content">
          <span class="metric-label">当前总带宽</span>
          <span class="metric-value">{{ metrics.currentBandwidth }}<small>Mbps</small></span>
          <span class="metric-trend" :class="metrics.bandwidthTrendClass">
            {{ metrics.bandwidthTrend }}%
          </span>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon green">📤</div>
        <div class="metric-content">
          <span class="metric-label">上行带宽</span>
          <span class="metric-value">{{ metrics.uploadBandwidth }}<small>Mbps</small></span>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadPercent + '%' }"></div>
          </div>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon orange">📥</div>
        <div class="metric-content">
          <span class="metric-label">下行带宽</span>
          <span class="metric-value">{{ metrics.downloadBandwidth }}<small>Mbps</small></span>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: downloadPercent + '%' }"></div>
          </div>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon purple">💾</div>
        <div class="metric-content">
          <span class="metric-label">今日总流量</span>
          <span class="metric-value">{{ metrics.todayTraffic }}<small>GB</small></span>
          <span class="metric-sub">预计 {{ metrics.estimateTraffic }} GB</span>
        </div>
      </div>
    </div>

    <!-- 带宽曲线 -->
    <div class="chart-card">
      <div class="chart-header">
        <span>带宽趋势（最近8小时）</span>
        <div class="chart-legend">
          <span><span class="legend-dot incoming"></span>上行</span>
          <span><span class="legend-dot outgoing"></span>下行</span>
        </div>
      </div>
      <div class="chart-container">
        <canvas ref="bandwidthChart" style="width: 100%; height: 200px;"></canvas>
      </div>
    </div>

    <!-- 流量控制策略 -->
    <div class="policy-card">
      <div class="card-header">
        <span>⚙️ 流量控制策略</span>
        <span class="policy-hint">设置后将自动生效</span>
      </div>
      <div class="policy-body">
        <div class="policy-item">
          <label class="checkbox-label">
            <input type="checkbox" v-model="autoDisconnect">
            <span>无人观看自动断流</span>
          </label>
          <div class="policy-control" v-if="autoDisconnect">
            <span>无操作后</span>
            <select v-model="disconnectMinutes">
              <option value="1">1</option>
              <option value="3">3</option>
              <option value="5">5</option>
              <option value="10">10</option>
            </select>
            <span>分钟自动断开视频流</span>
          </div>
        </div>

        <div class="policy-item">
          <label class="checkbox-label">
            <input type="checkbox" v-model="nightDegrade">
            <span>定时限流</span>
          </label>
          <div class="policy-control" v-if="nightDegrade">
            <span>夜间时段</span>
            <select v-model="nightStart">
              <option value="22">22:00</option>
              <option value="23">23:00</option>
              <option value="0">00:00</option>
            </select>
            <span>-</span>
            <select v-model="nightEnd">
              <option value="6">06:00</option>
              <option value="7">07:00</option>
              <option value="8">08:00</option>
            </select>
            <span>自动降低码率至</span>
            <select v-model="nightBitrate">
              <option value="512">512 kbps</option>
              <option value="1024">1024 kbps</option>
              <option value="2048">2048 kbps</option>
            </select>
          </div>
        </div>

        <div class="policy-item">
          <label class="checkbox-label">
            <input type="checkbox" v-model="bandwidthLimit">
            <span>带宽上限</span>
          </label>
          <div class="policy-control" v-if="bandwidthLimit">
            <span>总带宽超过</span>
            <input type="number" v-model="maxBandwidth" step="10" min="10" max="200">
            <span>Mbps时自动限流</span>
          </div>
        </div>

        <button class="apply-btn" :disabled="policyLoading" @click="applyPolicy">
          {{ policyLoading ? '应用中...' : '应用策略' }}
        </button>
      </div>
    </div>

    <!-- 设备流量排行 -->
    <div class="rank-card">
      <div class="card-header">
        <span>设备流量排行（今日）</span>
        <span class="total-traffic">总流量: {{ totalTraffic }} GB</span>
      </div>
      <div class="rank-list">
        <div class="rank-item" v-for="(item, idx) in trafficRank" :key="item.id">
          <div class="rank-number">{{ idx + 1 }}</div>
          <div class="rank-info">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-ip">{{ item.ip }}</div>
          </div>
          <div class="rank-bar">
            <div class="rank-fill" :style="{ width: item.percent + '%' }"></div>
          </div>
          <div class="rank-value">
            {{ parseFloat(item.traffic) > 0 ? item.traffic + ' GB' : (item.bitrate ? item.bitrate + ' kbps' : '0 GB') }}
          </div>
          <button
              class="limit-btn"
              :disabled="limitingDeviceId === item.id || !item.online"
              @click="limitDevice(item)"
              :title="!item.online ? '设备离线，无法限流' : '限流'">
            {{ limitingDeviceId === item.id ? '限流中...' : '限流' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="toastMsg" class="toast" :class="{ 'toast-error': toastIsError }">
      {{ toastMsg }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import {
  getRealtimeBandwidth,
  getBandwidthHistory,
  getTodayTraffic,
  getDeviceTrafficRank,
  applyPolicy as applyPolicyApi,
  limitDevice as limitDeviceApi,
  getCurrentPolicy,
  getDevices,
  getDeviceTraffic
} from '@/api/video'

// 加载状态
const policyLoading = ref(false)
const limitingDeviceId = ref(null)
const lastUpdateTime = ref('')
const toastMsg = ref('')
const toastIsError = ref(false)

// 设备统计
const totalDeviceCount = ref(0)

// 设备列表
const deviceList = ref([])
const selectedDeviceId = ref('all')

// 带宽数据
const currentBandwidth = ref('0')
const bandwidthChange = ref('0')
const uploadBandwidth = ref('0')
const downloadBandwidth = ref('0')
const todayTraffic = ref('0')
const estimateTraffic = ref('0')

// 指标计算
const metrics = computed(() => ({
  currentBandwidth: currentBandwidth.value,
  bandwidthTrend: bandwidthChange.value,
  bandwidthTrendClass: parseFloat(bandwidthChange.value) >= 0 ? 'up' : 'down',
  uploadBandwidth: uploadBandwidth.value,
  downloadBandwidth: downloadBandwidth.value,
  todayTraffic: todayTraffic.value,
  estimateTraffic: estimateTraffic.value
}))

// 策略配置
const autoDisconnect = ref(false)
const disconnectMinutes = ref('5')
const nightDegrade = ref(false)
const nightStart = ref('23')
const nightEnd = ref('6')
const nightBitrate = ref('512')
const bandwidthLimit = ref(false)
const maxBandwidth = ref('50')

// 排行数据
const totalTraffic = ref('0')
const trafficRank = ref([])

// ZLM实时流数据
const streamData = ref([])

// 图表数据
const timeLabels = ref([])
const incomingData = ref([])
const outgoingData = ref([])

// 定时器
let pollingTimer = null
let animationId = null

// 计算属性
const uploadPercent = computed(() => {
  const total = parseFloat(currentBandwidth.value)
  const upload = parseFloat(uploadBandwidth.value)
  if (total === 0) return 0
  return Math.round(upload / total * 100)
})

const downloadPercent = computed(() => {
  const total = parseFloat(currentBandwidth.value)
  const download = parseFloat(downloadBandwidth.value)
  if (total === 0) return 0
  return Math.round(download / total * 100)
})

// 显示提示
const showToast = (msg, isError = false) => {
  toastMsg.value = msg
  toastIsError.value = isError
  setTimeout(() => {
    toastMsg.value = ''
    toastIsError.value = false
  }, 2000)
}

// 获取设备列表
const fetchDeviceList = async () => {
  try {
    const res = await getDevices()
    if (res.data.code === 200 || res.data.success) {
      deviceList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取设备列表失败:', error)
  }
}

// 获取实时带宽数据
const fetchRealtimeBandwidth = async () => {
  try {
    const res = await getRealtimeBandwidth()
    if (res && res.data) {
      currentBandwidth.value = String(res.data.currentBandwidth || '0')
      bandwidthChange.value = String(res.data.bandwidthChange || '0')
      uploadBandwidth.value = String(res.data.uploadBandwidth || '0')
      downloadBandwidth.value = String(res.data.downloadBandwidth || '0')
      streamData.value = res.data.streams || []
      lastUpdateTime.value = new Date().toLocaleTimeString()
    }
  } catch (error) {
    console.error('获取实时带宽失败:', error)
  }
}

// 获取今日流量统计
const fetchTodayTraffic = async () => {
  try {
    const res = await getTodayTraffic()
    if (res && res.data) {
      todayTraffic.value = String(res.data.todayTraffic || '0')
      estimateTraffic.value = String(res.data.estimateTraffic || '0')
    }
  } catch (error) {
    console.error('获取今日流量失败:', error)
  }
}

// 获取设备流量排行
const fetchDeviceRank = async () => {
  try {
    let res
    if (selectedDeviceId.value === 'all') {
      res = await getDeviceTrafficRank()
    } else {
      const deviceRes = await getDeviceTraffic(selectedDeviceId.value, 'day')
      const device = deviceList.value.find(d => d.id == selectedDeviceId.value)
      let totalTrafficMB = 0
      if (deviceRes.data.code === 200 && deviceRes.data.data) {
        totalTrafficMB = deviceRes.data.data.totalTrafficMB || 0
      }
      const trafficGB = (totalTrafficMB / 1024).toFixed(1)
      res = {
        data: {
          totalTraffic: trafficGB,
          rankList: [{
            id: selectedDeviceId.value,
            name: device?.name || '未知设备',
            ip: device?.ip || '',
            traffic: trafficGB,
            percent: 100,
            online: true
          }]
        }
      }
    }

    if (res && res.data) {
      let rankList = res.data.rankList || []

      // 如果录制历史无当天数据，使用ZLM实时流数据作为fallback
      const hasRealTraffic = rankList.some(item => parseFloat(item.traffic) > 0)
      if (!hasRealTraffic && streamData.value.length > 0) {
        rankList = deviceList.value
          .filter(device => {
            const streamId = device.stream_id || ('camera_' + (device.ip || '').replace(/\./g, '_'))
            return streamData.value.some(s => s.stream === streamId)
          })
          .map(device => {
            const streamId = device.stream_id || ('camera_' + (device.ip || '').replace(/\./g, '_'))
            const stream = streamData.value.find(s => s.stream === streamId)
            return {
              id: device.id,
              name: device.name || '未知设备',
              ip: device.ip || '',
              traffic: '0', // 录制流量为0
              bitrate: stream ? stream.bitrate : 0,
              percent: 0,
              online: true
            }
          })

        // 对实时流数据计算百分比
        const maxBitrate = Math.max(...rankList.map(r => r.bitrate || 0), 1)
        rankList.forEach(item => {
          item.percent = Math.round((item.bitrate / maxBitrate) * 100)
        })
      }

      trafficRank.value = rankList
      totalTraffic.value = String(res.data.totalTraffic || '0')
      totalDeviceCount.value = trafficRank.value.length
    }
  } catch (error) {
    console.error('获取设备排行失败:', error)
    trafficRank.value = []
    totalTraffic.value = '0'
  }
}

// 获取当前策略配置
const fetchCurrentPolicy = async () => {
  try {
    const res = await getCurrentPolicy()
    if (res && res.data) {
      autoDisconnect.value = res.data.autoDisconnect ?? false
      disconnectMinutes.value = String(res.data.disconnectMinutes ?? '5')
      nightDegrade.value = res.data.nightDegrade ?? false
      nightStart.value = String(res.data.nightStart ?? '23')
      nightEnd.value = String(res.data.nightEnd ?? '6')
      nightBitrate.value = String(res.data.nightBitrate ?? '512')
      bandwidthLimit.value = res.data.bandwidthLimit ?? false
      maxBandwidth.value = String(res.data.maxBandwidth ?? '50')
    }
  } catch (error) {
    console.error('获取策略配置失败:', error)
  }
}

// 获取带宽历史趋势
const fetchBandwidthHistory = async () => {
  try {
    const params = { hours: 8 }
    if (selectedDeviceId.value !== 'all') {
      params.deviceId = selectedDeviceId.value
    }
    const res = await getBandwidthHistory(params)
    if (res && res.data) {
      if (res.data.timeLabels && res.data.timeLabels.length > 0) {
        timeLabels.value = res.data.timeLabels
        incomingData.value = res.data.incomingData
        outgoingData.value = res.data.outgoingData
      } else {
        timeLabels.value = ['14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00']
        incomingData.value = [0, 0, 0, 0, 0, 0, 0, 0]
        outgoingData.value = [0, 0, 0, 0, 0, 0, 0, 0]
      }
      await nextTick()
      drawChart()
    }
  } catch (error) {
    console.error('获取带宽历史失败:', error)
  }
}

// 图表绘制
const drawChart = () => {
  const canvas = document.querySelector('canvas')
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const rect = canvas.getBoundingClientRect()
  const width = rect.width
  const height = rect.height

  canvas.width = width
  canvas.height = height

  ctx.clearRect(0, 0, width, height)

  if (timeLabels.value.length === 0 || !timeLabels.value.length) {
    ctx.font = '14px sans-serif'
    ctx.fillStyle = '#94a3b8'
    ctx.textAlign = 'center'
    ctx.fillText('暂无数据', width / 2, height / 2)
    return
  }

  const maxValue = Math.max(...incomingData.value, ...outgoingData.value, 1)
  const topMargin = 20
  const bottomMargin = 30
  const leftMargin = 40
  const rightMargin = 20
  const chartHeight = height - topMargin - bottomMargin
  const chartWidth = width - leftMargin - rightMargin

  const scaleY = chartHeight / maxValue
  const stepX = chartWidth / (timeLabels.value.length - 1)

  ctx.save()
  ctx.strokeStyle = '#e2e8f0'
  ctx.lineWidth = 0.5
  ctx.fillStyle = '#94a3b8'
  ctx.font = '10px sans-serif'
  ctx.textAlign = 'right'

  for (let i = 0; i <= 4; i++) {
    const value = (maxValue / 4) * i
    const y = topMargin + chartHeight - (value * scaleY)
    ctx.beginPath()
    ctx.moveTo(leftMargin, y)
    ctx.lineTo(width - rightMargin, y)
    ctx.stroke()
    ctx.fillText(value.toFixed(1), leftMargin - 5, y + 3)
  }

  ctx.save()
  ctx.translate(15, topMargin + chartHeight / 2)
  ctx.rotate(-Math.PI / 2)
  ctx.fillStyle = '#64748b'
  ctx.font = '11px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('带宽 (Mbps)', 0, 0)
  ctx.restore()

  // 绘制上行线
  ctx.beginPath()
  ctx.strokeStyle = '#3b82f6'
  ctx.lineWidth = 2
  let firstPoint = true
  for (let i = 0; i < incomingData.value.length; i++) {
    const x = leftMargin + i * stepX
    const y = topMargin + chartHeight - (incomingData.value[i] * scaleY)
    if (firstPoint) {
      ctx.moveTo(x, y)
      firstPoint = false
    } else {
      ctx.lineTo(x, y)
    }
  }
  ctx.stroke()

  // 绘制上行数据点
  ctx.fillStyle = '#3b82f6'
  for (let i = 0; i < incomingData.value.length; i++) {
    const x = leftMargin + i * stepX
    const y = topMargin + chartHeight - (incomingData.value[i] * scaleY)
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, 2 * Math.PI)
    ctx.fill()
    ctx.fillStyle = 'white'
    ctx.beginPath()
    ctx.arc(x, y, 1.5, 0, 2 * Math.PI)
    ctx.fill()
    ctx.fillStyle = '#3b82f6'
  }

  // 绘制下行线
  ctx.beginPath()
  ctx.strokeStyle = '#42b983'
  ctx.lineWidth = 2
  firstPoint = true
  for (let i = 0; i < outgoingData.value.length; i++) {
    const x = leftMargin + i * stepX
    const y = topMargin + chartHeight - (outgoingData.value[i] * scaleY)
    if (firstPoint) {
      ctx.moveTo(x, y)
      firstPoint = false
    } else {
      ctx.lineTo(x, y)
    }
  }
  ctx.stroke()

  // 绘制下行数据点
  ctx.fillStyle = '#42b983'
  for (let i = 0; i < outgoingData.value.length; i++) {
    const x = leftMargin + i * stepX
    const y = topMargin + chartHeight - (outgoingData.value[i] * scaleY)
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, 2 * Math.PI)
    ctx.fill()
    ctx.fillStyle = 'white'
    ctx.beginPath()
    ctx.arc(x, y, 1.5, 0, 2 * Math.PI)
    ctx.fill()
    ctx.fillStyle = '#42b983'
  }

  // 绘制X轴标签
  ctx.fillStyle = '#64748b'
  ctx.font = '10px sans-serif'
  ctx.textAlign = 'center'
  for (let i = 0; i < timeLabels.value.length; i++) {
    const x = leftMargin + i * stepX
    ctx.fillText(timeLabels.value[i], x, height - bottomMargin + 15)
  }

  ctx.restore()
}

// 刷新所有数据
const refreshAllData = async () => {
  await Promise.all([
    fetchRealtimeBandwidth(),
    fetchTodayTraffic(),
    fetchDeviceRank()
  ])
}

// 设备切换处理
const onDeviceChange = () => {
  fetchDeviceRank()
  fetchBandwidthHistory()
}

// 应用策略
const applyPolicy = async () => {
  policyLoading.value = true
  try {
    const res = await applyPolicyApi({
      autoDisconnect: autoDisconnect.value,
      disconnectMinutes: parseInt(disconnectMinutes.value),
      nightDegrade: nightDegrade.value,
      nightStart: nightStart.value,
      nightEnd: nightEnd.value,
      nightBitrate: parseInt(nightBitrate.value),
      bandwidthLimit: bandwidthLimit.value,
      maxBandwidth: parseInt(maxBandwidth.value)
    })

    if (res.code === 200 || res.success) {
      showToast('策略已应用')
    } else {
      showToast(res.message || '应用失败', true)
    }
  } catch (error) {
    console.error('应用策略失败:', error)
    showToast('应用失败，请重试', true)
  } finally {
    policyLoading.value = false
  }
}

// 限流设备
const limitDevice = async (device) => {
  if (!device.online) {
    showToast(`设备 ${device.name} 离线，无法限流`, true)
    return
  }

  limitingDeviceId.value = device.id
  try {
    const res = await limitDeviceApi(device.id, {
      limitBandwidth: 10,
      reason: '手动限流'
    })

    if (res.code === 200 || res.success) {
      showToast(`已对设备 ${device.name} 应用限流策略`)
      await fetchDeviceRank()
    } else {
      showToast(res.message || '限流失败', true)
    }
  } catch (error) {
    console.error('限流失败:', error)
    showToast('限流失败，请重试', true)
  } finally {
    limitingDeviceId.value = null
  }
}

// 窗口大小变化处理
const handleResize = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  animationId = requestAnimationFrame(() => {
    drawChart()
  })
}

// 初始化
onMounted(async () => {
  await fetchDeviceList()
  await refreshAllData()
  await fetchBandwidthHistory()
  await fetchCurrentPolicy()

  pollingTimer = setInterval(() => {
    fetchRealtimeBandwidth()
    fetchTodayTraffic()
    fetchDeviceRank()
  }, 5000)

  window.addEventListener('resize', handleResize)
  setTimeout(() => {
    drawChart()
  }, 200)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.traffic-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.device-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
}

.device-selector select {
  padding: 4px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  font-size: 13px;
  outline: none;
  cursor: pointer;
}

.device-selector select:hover {
  border-color: #42b983;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #1a2c3e;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #6c7a8e;
  font-size: 14px;
  margin: 0;
}

.live-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(66, 185, 131, 0.1);
  border-radius: 20px;
  font-size: 12px;
  color: #42b983;
}

.live-dot {
  width: 8px;
  height: 8px;
  background: #42b983;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.update-time {
  font-size: 11px;
  color: #6c7a8e;
  margin-left: 8px;
}

.device-stats {
  font-size: 11px;
  color: #42b983;
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid #e2e8f0;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.metric-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e8ecf2;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.metric-icon.blue { background: rgba(59, 130, 246, 0.1); }
.metric-icon.green { background: rgba(66, 185, 131, 0.1); }
.metric-icon.orange { background: rgba(245, 158, 11, 0.1); }
.metric-icon.purple { background: rgba(139, 92, 246, 0.1); }

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 13px;
  color: #6c7a8e;
  display: block;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a2c3e;
}

.metric-value small {
  font-size: 12px;
  font-weight: normal;
  margin-left: 4px;
}

.metric-trend {
  font-size: 12px;
  display: inline-block;
  margin-top: 4px;
}

.metric-trend.up { color: #42b983; }
.metric-trend.down { color: #ef4444; }

.metric-sub {
  font-size: 11px;
  color: #6c7a8e;
  display: block;
  margin-top: 4px;
}

.progress-bar {
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
  margin-top: 8px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #42b983;
  border-radius: 2px;
}

.chart-card, .policy-card, .rank-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #e8ecf2;
  margin-bottom: 24px;
  overflow: hidden;
}

.chart-header, .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf2;
  font-weight: 600;
}

.chart-legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  font-weight: normal;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
}

.legend-dot.incoming { background: #3b82f6; }
.legend-dot.outgoing { background: #42b983; }

.chart-container {
  padding: 20px;
}

.policy-body {
  padding: 20px;
}

.policy-item {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f4f9;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-bottom: 12px;
  font-weight: 500;
}

.policy-control {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 24px;
  flex-wrap: wrap;
}

.policy-control select, .policy-control input {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.policy-control input {
  width: 70px;
}

.policy-hint {
  font-size: 12px;
  font-weight: normal;
  color: #6c7a8e;
}

.apply-btn {
  width: 100%;
  padding: 10px;
  background: #42b983;
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  cursor: pointer;
  margin-top: 8px;
}

.apply-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.rank-list {
  padding: 0 20px 20px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f4f9;
}

.rank-number {
  width: 30px;
  font-size: 16px;
  font-weight: 600;
  color: #6c7a8e;
}

.rank-info {
  width: 180px;
}

.rank-name {
  font-weight: 500;
  font-size: 14px;
}

.rank-ip {
  font-size: 11px;
  color: #6c7a8e;
}

.rank-bar {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.rank-fill {
  height: 100%;
  background: linear-gradient(90deg, #42b983, #2d8f64);
  border-radius: 4px;
}

.rank-value {
  width: 80px;
  font-size: 13px;
  font-weight: 500;
}

.limit-btn {
  padding: 4px 12px;
  background: #f59e0b;
  border: none;
  border-radius: 4px;
  color: white;
  cursor: pointer;
  font-size: 11px;
}

.limit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #94a3b8;
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

.toast.toast-error {
  background: #ef4444;
}

@keyframes fadeOut {
  0% { opacity: 1; }
  70% { opacity: 1; }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}
</style>
