<template>
  <div class="ai-analysis-container">
    <!-- 页面头部 + 设备选择器 -->
    <div class="page-header">
      <div>
        <h1>🤖 AI 流量分析平台</h1>
        <p class="subtitle">基于千问大模型的摄像头流量数据分析</p>
      </div>
      <div class="header-actions">
        <div class="device-selector">
          <span class="selector-label">分析设备：</span>
          <select v-model="selectedDevice" class="device-select" @change="onDeviceChange">
            <option value="all">📊 全部摄像头（整体分析）</option>
            <option v-for="cam in cameraList" :key="cam.id" :value="cam.id">
              📹 {{ cam.name }}（{{ cam.location }}）
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📡</div>
        <div class="stat-content">
          <div class="stat-value">{{ currentStats.totalTraffic }}<small>GB</small></div>
          <div class="stat-label">今日总流量</div>
          <div class="stat-trend up">↑ {{ currentStats.trafficChange }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">💰</div>
        <div class="stat-content">
          <div class="stat-value">{{ currentStats.todayCost }}<small>元</small></div>
          <div class="stat-label">今日费用</div>
          <div class="stat-trend up">↑ {{ currentStats.costChange }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">📊</div>
        <div class="stat-content">
          <div class="stat-value">{{ currentStats.avgBitrate }}<small>kbps</small></div>
          <div class="stat-label">平均码率</div>
          <div class="stat-trend">{{ currentStats.bitrateStatus }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">⏰</div>
        <div class="stat-content">
          <div class="stat-value">{{ currentStats.peakTime }}</div>
          <div class="stat-label">流量峰值时段</div>
        </div>
      </div>
    </div>

    <!-- 摄像头流量排行 -->
    <div class="rank-card">
      <div class="card-header">
        <span v-if="selectedDevice === 'all'">📊 摄像头流量排行（今日）</span>
        <span v-else>📹 设备详情：{{ selectedCamera?.name }}</span>
        <span class="total-traffic">总流量: {{ currentStats.totalTraffic }} GB | 预估费用: {{ currentStats.todayCost }} 元</span>
      </div>

      <div class="rank-list" v-if="selectedDevice === 'all'">
        <div class="rank-item" v-for="(item, idx) in cameraList" :key="item.id">
          <div class="rank-number">{{ idx + 1 }}</div>
          <div class="rank-info">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-location">{{ item.location }}</div>
          </div>
          <div class="rank-bar">
            <div class="rank-fill" :style="{ width: item.percent + '%' }"></div>
          </div>
          <div class="rank-value">{{ item.traffic }} GB</div>
          <div class="rank-bitrate">{{ item.bitrate }} kbps</div>
          <div class="rank-cost">{{ item.cost }} 元</div>
          <button class="analyze-single-btn" @click="selectSingleDevice(item.id)">单独分析</button>
        </div>
      </div>

      <div class="single-device-detail" v-else-if="selectedCamera">
        <div class="detail-card">
          <div class="detail-row"><span class="detail-label">设备名称</span><span class="detail-value">{{ selectedCamera.name }}</span></div>
          <div class="detail-row"><span class="detail-label">安装位置</span><span class="detail-value">{{ selectedCamera.location }}</span></div>
          <div class="detail-row"><span class="detail-label">今日流量</span><span class="detail-value highlight">{{ selectedCamera.traffic }} GB</span></div>
          <div class="detail-row"><span class="detail-label">今日费用</span><span class="detail-value">{{ selectedCamera.cost }} 元</span></div>
          <div class="detail-row"><span class="detail-label">当前码率</span><span class="detail-value" :class="getBitrateClass(selectedCamera.bitrate)">{{ selectedCamera.bitrate }} kbps</span></div>
          <div class="detail-row"><span class="detail-label">建议码率</span><span class="detail-value">{{ getSuggestBitrate(selectedCamera.bitrate) }} kbps</span></div>
          <div class="detail-row"><span class="detail-label">预计节省</span><span class="detail-value save-amount">{{ getSaveAmount(selectedCamera.bitrate) }} 元/月</span></div>
          <button class="back-to-all" @click="selectedDevice = 'all'">← 返回全部设备</button>
        </div>
      </div>
    </div>

    <!-- ECharts 流量趋势图 -->
    <div class="trend-card">
      <div class="card-header">
        <span>📈 流量趋势（最近24小时）</span>
        <div class="chart-controls">
          <button class="chart-btn" :class="{ active: chartRange === '6h' }" @click="setChartRange('6h')">6小时</button>
          <button class="chart-btn" :class="{ active: chartRange === '24h' }" @click="setChartRange('24h')">24小时</button>
          <button class="chart-btn" :class="{ active: chartRange === '7d' }" @click="setChartRange('7d')">7天</button>
        </div>
      </div>
      <div class="chart-container">
        <div ref="trafficChartRef" class="echarts-box"></div>
      </div>
      <div class="chart-stats">
        <div class="chart-stat"><span class="stat-label">峰值</span><span class="stat-value">{{ peakBandwidth }} Mbps</span></div>
        <div class="chart-stat"><span class="stat-label">均值</span><span class="stat-value">{{ avgBandwidth }} Mbps</span></div>
        <div class="chart-stat"><span class="stat-label">峰值时间</span><span class="stat-value">{{ peakTimeLabel }}</span></div>
        <div class="chart-stat"><span class="stat-label">95分位</span><span class="stat-value">{{ p95Bandwidth }} Mbps</span></div>
      </div>
    </div>

    <!-- 饼图 -->
    <div class="pie-card" v-if="selectedDevice === 'all'">
      <div class="card-header"><span>🥧 摄像头流量分布</span></div>
      <div ref="pieChartRef" class="echarts-pie"></div>
    </div>

    <!-- AI 分析报告 + 优化建议 -->
    <div class="two-columns">
      <div class="report-card">
        <div class="card-header">
          <span>📋 AI 流量分析报告</span>
          <span class="report-date">{{ todayDate }}</span>
          <span class="analyze-target" v-if="selectedDevice !== 'all'">正在分析：{{ selectedCamera?.name }}</span>
          <button class="analyze-btn" @click="runAnalysis" :disabled="analyzing">
            <span v-if="analyzing">⏳ 分析中...</span>
            <span v-else>🔍 立即分析</span>
          </button>
        </div>
        <div class="report-content">
          <div v-if="analyzing" class="loading-state"><div class="loading-spinner"></div><p>AI 正在分析流量数据，请稍候...</p></div>
          <div v-else-if="analysisError" class="error-state">
            <span>❌</span>
            <p>AI分析失败</p>
            <p class="error-detail">{{ analysisError }}</p>
          </div>
          <div v-else-if="analysisResult" class="report-result">
            <div class="report-section"><div class="section-title">【整体流量评估】</div><div class="section-text">{{ analysisResult.overview }}</div></div>
            <div class="report-section"><div class="section-title">【高流量摄像头分析】</div><div class="section-text">{{ analysisResult.highTraffic }}</div></div>
            <div class="report-section"><div class="section-title">【码率分析】</div><div class="section-text">{{ analysisResult.bitrateAnalysis }}</div></div>
          </div>
          <div v-else class="report-placeholder"><span>🤖</span><p>点击"立即分析"按钮，AI将分析流量数据</p></div>
        </div>
      </div>

      <div class="suggest-card">
        <div class="card-header"><span>💡 流量优化建议</span></div>
        <div class="suggest-content">
          <div v-if="analyzing" class="loading-placeholder"><div class="loading-spinner-small"></div><p>分析完成后自动生成建议...</p></div>
          <div v-else-if="suggestions.length > 0">
            <div class="suggest-item" v-for="(item, idx) in suggestions" :key="idx"><span class="suggest-icon">✓</span><span class="suggest-text">{{ item }}</span></div>
          </div>
          <div v-else class="suggest-placeholder"><span>💡</span><p>暂无建议，请先执行AI分析</p></div>
        </div>
      </div>
    </div>

    <!-- 预警信息 -->
    <div class="alert-card" v-if="alertMsg && !analyzing">
      <div class="card-header"><span>⚠️ 预警信息</span></div>
      <div class="alert-content"><div class="alert-box"><span class="alert-icon">⚠️</span><span class="alert-text">{{ alertMsg }}</span></div></div>
    </div>

    <!-- 历史记录 -->
    <div class="history-card">
      <div class="card-header">
        <span>📋 历史分析记录</span>
        <div class="history-actions">
          <input type="text" v-model="searchKeyword" placeholder="搜索..." class="search-input" />
          <button class="refresh-btn" @click="loadHistory">🔄 刷新</button>
        </div>
      </div>
      <div class="history-table">
        <div class="table-header"><span>日期</span><span>设备</span><span>总流量</span><span>费用</span><span>平均码率</span><span>报告摘要</span><span>操作</span></div>
        <div class="table-body">
          <div class="table-row" v-for="record in filteredHistory" :key="record.id">
            <span>{{ record.date }}</span><span>{{ record.deviceName || '全部' }}</span><span>{{ record.totalTraffic }} GB</span>
            <span>{{ record.cost }} 元</span><span>{{ record.avgBitrate }} kbps</span><span class="summary">{{ record.summary }}</span>
            <span><button class="detail-btn" @click="viewDetail(record)">详情</button></span>
          </div>
          <div v-if="filteredHistory.length === 0" class="empty-row"><span>暂无历史记录</span></div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header"><h3>流量分析报告详情 · {{ selectedRecord?.date }}</h3><button class="modal-close" @click="closeDetailModal">×</button></div>
        <div class="modal-body">
          <div class="detail-stats">
            <div class="detail-stat"><label>设备</label><span>{{ selectedRecord?.deviceName || '全部' }}</span></div>
            <div class="detail-stat"><label>总流量</label><span>{{ selectedRecord?.totalTraffic }} GB</span></div>
            <div class="detail-stat"><label>费用</label><span>{{ selectedRecord?.cost }} 元</span></div>
            <div class="detail-stat"><label>平均码率</label><span>{{ selectedRecord?.avgBitrate }} kbps</span></div>
            <div class="detail-stat"><label>峰值带宽</label><span>{{ selectedRecord?.peakBandwidth }} Mbps</span></div>
            <div class="detail-stat"><label>峰值时段</label><span>{{ selectedRecord?.peakTime }}</span></div>
          </div>
          <div class="detail-report"><div class="detail-title">AI 分析报告</div><div class="detail-text">{{ selectedRecord?.fullReport || selectedRecord?.summary }}</div></div>
          <div class="detail-suggest" v-if="selectedRecord?.suggestions"><div class="detail-title">优化建议</div><ul><li v-for="(s, idx) in selectedRecord.suggestions" :key="idx">{{ s }}</li></ul></div>
        </div>
      </div>
    </div>

    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import request from '../api/request.js'

// ========== 数据状态 ==========
const analyzing = ref(false)
const todayDate = ref('')
const searchKeyword = ref('')
const showDetailModal = ref(false)
const selectedRecord = ref(null)
const toastMsg = ref('')
const chartRange = ref('24h')
const selectedDevice = ref('all')

// 摄像头列表（从后端加载）
const cameraList = ref([])

// 当前统计数据（从后端计算）
const currentStats = ref({
  totalTraffic: '0',
  trafficChange: 0,
  todayCost: '0',
  costChange: 0,
  avgBitrate: 0,
  bitrateStatus: '正常',
  peakTime: '--:--'
})

// 选中的摄像头
const selectedCamera = computed(() => {
  if (selectedDevice.value === 'all') return null
  return cameraList.value.find(c => c.id === selectedDevice.value)
})

// 图表数据
const trafficData24h = ref({
  times: [],
  values: []
})
const peakBandwidth = ref('0')
const avgBandwidth = ref('0')
const peakTimeLabel = ref('--:--')
const p95Bandwidth = ref('0')

// AI 分析结果
const analysisResult = ref(null)
const suggestions = ref([])
const alertMsg = ref('')
const analysisError = ref('')
const historyList = ref([])

// ECharts 实例
const trafficChartRef = ref(null)
const pieChartRef = ref(null)
let trafficChart = null
let pieChart = null

// ========== 辅助函数 ==========
const showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 2000)
}

const getBitrateClass = (bitrate) => {
  if (bitrate > 3000) return 'bitrate-high'
  if (bitrate > 2000) return 'bitrate-normal'
  return 'bitrate-low'
}

const getSuggestBitrate = (bitrate) => {
  if (bitrate > 3000) return 2048
  if (bitrate > 2000) return 1536
  return bitrate
}

const getSaveAmount = (bitrate) => {
  if (bitrate > 3000) return Math.round((bitrate - 2048) / 2048 * 30)
  if (bitrate > 2000) return Math.round((bitrate - 1536) / 1536 * 20)
  return 0
}

const filteredHistory = computed(() => {
  if (!searchKeyword.value) return historyList.value
  const kw = searchKeyword.value.toLowerCase()
  return historyList.value.filter(h => h.date.includes(kw) || h.summary?.toLowerCase().includes(kw) || h.deviceName?.toLowerCase().includes(kw))
})

const onDeviceChange = () => {
  analysisResult.value = null
  suggestions.value = []
  alertMsg.value = ''
}

const selectSingleDevice = (id) => {
  selectedDevice.value = id
  onDeviceChange()
}

// ========== 从后端加载数据 ==========

// 加载设备列表
const loadDevices = async () => {
  try {
    const res = await request.get('/api/video/devices')
    if (res.data.code === 200) {
      const devices = res.data.data || []
      cameraList.value = devices.map(device => ({
        id: device.id,
        name: device.name,
        location: device.ip || '未知位置',
        traffic: 0,
        bitrate: 2048, // 默认码率
        cost: 0,
        percent: 0,
        ip: device.ip
      }))
      await loadTrafficData()
      await loadRealTimeBitrate()
    }
  } catch (err) {
    console.error('加载设备列表失败:', err)
    showToast('加载设备列表失败')
  }
}

// 加载流量排名数据
const loadTrafficData = async () => {
  try {
    const res = await request.get('/api/traffic/ranking', { params: { period: 'day' } })
    if (res.data.code === 200) {
      const rankings = res.data.data || []

      for (const ranking of rankings) {
        const device = cameraList.value.find(d => d.id === ranking.id)
        if (device) {
          const trafficMB = ranking.total_traffic_mb || 0
          device.traffic = (trafficMB / 1024).toFixed(1)
          device.cost = (device.traffic * 0.5).toFixed(2)
        }
      }

      // 计算百分比和更新统计
      const maxTraffic = Math.max(...cameraList.value.map(d => parseFloat(d.traffic) || 0), 0)
      cameraList.value.forEach(device => {
        const trafficNum = parseFloat(device.traffic) || 0
        device.percent = maxTraffic > 0 ? Math.round(trafficNum / maxTraffic * 100) : 0
      })

      updateCurrentStats()
      updatePieChart()
    }
  } catch (err) {
    console.error('加载流量排名失败:', err)
  }
}

// 加载实时码率（从 ZLM 获取）
const loadRealTimeBitrate = async () => {
  try {
    const res = await request.get('/api/traffic/realtime')
    if (res.data.code === 200) {
      const streams = res.data.data?.streams || []

      for (const device of cameraList.value) {
        const streamName = `camera_${device.ip?.replace(/\./g, '_')}`
        const stream = streams.find(s => s.stream === streamName)
        if (stream && stream.bitrate) {
          device.bitrate = stream.bitrate
        }
      }

      updateCurrentStats()
    }
  } catch (err) {
    console.error('加载实时码率失败:', err)
  }
}

// 加载流量趋势
const loadTrafficTrend = async () => {
  try {
    const res = await request.get('/api/traffic/trend', { params: { days: 7 } })
    if (res.data.code === 200) {
      const trend = res.data.data || []
      const times = []
      const values = []

      trend.forEach(item => {
        times.push(item.date?.substring(5) || '')
        values.push((item.total_traffic_mb || 0) / 1024 / 24)
      })

      trafficData24h.value = { times, values }

      // 更新图表统计
      if (values.length > 0) {
        const maxVal = Math.max(...values)
        const avgVal = values.reduce((a, b) => a + b, 0) / values.length
        peakBandwidth.value = maxVal.toFixed(1)
        avgBandwidth.value = avgVal.toFixed(1)

        const maxIndex = values.indexOf(maxVal)
        if (maxIndex >= 0 && times[maxIndex]) {
          peakTimeLabel.value = times[maxIndex]
        }

        // 计算 95 分位
        const sorted = [...values].sort((a, b) => a - b)
        const p95Index = Math.floor(sorted.length * 0.95)
        p95Bandwidth.value = sorted[p95Index]?.toFixed(1) || '0'
      }

      updateTrafficChart()
    }
  } catch (err) {
    console.error('加载流量趋势失败:', err)
  }
}

// 更新当前统计
const updateCurrentStats = () => {
  const totalTraffic = cameraList.value.reduce((sum, d) => sum + (parseFloat(d.traffic) || 0), 0)
  const totalCost = cameraList.value.reduce((sum, d) => sum + (parseFloat(d.cost) || 0), 0)
  const avgBitrate = cameraList.value.length > 0
      ? Math.round(cameraList.value.reduce((sum, d) => sum + (d.bitrate || 0), 0) / cameraList.value.length)
      : 0

  let bitrateStatus = '正常'
  if (avgBitrate > 3000) bitrateStatus = '偏高'
  else if (avgBitrate < 1000) bitrateStatus = '偏低'

  currentStats.value = {
    totalTraffic: totalTraffic.toFixed(1),
    trafficChange: 0,
    todayCost: totalCost.toFixed(2),
    costChange: 0,
    avgBitrate: avgBitrate,
    bitrateStatus: bitrateStatus,
    peakTime: peakTimeLabel.value
  }
}

// ========== 图表初始化 ==========
const initTrafficChart = () => {
  if (!trafficChartRef.value) return
  if (trafficChart) trafficChart.dispose()
  trafficChart = echarts.init(trafficChartRef.value)
  updateTrafficChart()
}

const updateTrafficChart = () => {
  if (!trafficChart) return

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trafficData24h.value.times, axisLabel: { fontSize: 11 }, axisLine: { lineStyle: { color: '#6c7a8e' } } },
    yAxis: { type: 'value', name: 'Mbps', nameTextStyle: { color: '#6c7a8e', fontSize: 11 }, splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } } },
    series: [{ name: '流量', type: 'line', data: trafficData24h.value.values, smooth: true, symbol: 'circle', symbolSize: 8, lineStyle: { width: 3, color: '#42b983' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(66, 185, 131, 0.3)' }, { offset: 1, color: 'rgba(66, 185, 131, 0.05)' }]) }, itemStyle: { color: '#42b983', borderColor: '#fff', borderWidth: 2 } }]
  }
  trafficChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)
  updatePieChart()
}

const updatePieChart = () => {
  if (!pieChart) return

  const colors = ['#42b983', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec489a', '#06b6d4', '#84cc16']
  const pieData = cameraList.value.map((item, idx) => ({
    name: item.name,
    value: parseFloat(item.traffic) || 0,
    itemStyle: { color: colors[idx % colors.length] }
  }))

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {d}% ({c} GB)' },
    legend: { orient: 'vertical', left: 'left', textStyle: { color: '#4a5a6e', fontSize: 12 } },
    series: [{ name: '流量分布', type: 'pie', radius: '55%', center: ['50%', '50%'], data: pieData, emphasis: { scale: true }, label: { show: true, formatter: '{b}: {d}%', fontSize: 11 }, itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 } }]
  }
  pieChart.setOption(option)
}

const setChartRange = (range) => {
  chartRange.value = range
  // 这里可以根据 range 重新加载不同周期的数据
  loadTrafficTrend()
}

// ========== AI 分析 ==========
const runAnalysis = async () => {
  analyzing.value = true
  analysisResult.value = null
  suggestions.value = []
  alertMsg.value = ''
  analysisError.value = ''

  try {
    const trendData = trafficData24h.value.times.map((time, idx) => ({
      time: time,
      value: trafficData24h.value.values[idx] || 0
    }))

    const cameraData = selectedDevice.value === 'all'
        ? cameraList.value.map(c => ({
          name: c.name,
          location: c.location,
          traffic: parseFloat(c.traffic) || 0,
          bitrate: c.bitrate,
          cost: parseFloat(c.cost) || 0
        }))
        : [{
          name: selectedCamera.value?.name || '',
          location: selectedCamera.value?.location || '',
          traffic: parseFloat(selectedCamera.value?.traffic) || 0,
          bitrate: selectedCamera.value?.bitrate || 0,
          cost: parseFloat(selectedCamera.value?.cost) || 0
        }]

    const requestData = {
      totalTraffic: parseFloat(currentStats.value.totalTraffic) || 0,
      todayCost: parseFloat(currentStats.value.todayCost) || 0,
      avgBitrate: currentStats.value.avgBitrate || 0,
      peakBandwidth: parseFloat(peakBandwidth.value) || 0,
      peakTime: currentStats.value.peakTime,
      cameraRank: cameraData,
      trafficTrend: trendData
    }

    const res = await request.post('/api/ai/analyze-traffic', requestData)

    if (res.data.code === 200) {
      const data = res.data.data
      analysisResult.value = {
        overview: data.overview || '分析完成',
        highTraffic: data.highTraffic || '暂无高流量分析',
        bitrateAnalysis: data.bitrateAnalysis || '暂无码率分析'
      }
      suggestions.value = data.suggestions || []
      alertMsg.value = data.alert || ''
      showToast('AI分析完成')
    } else {
      analysisError.value = res.data.msg || 'AI分析返回错误'
      showToast('AI分析失败')
    }
  } catch (err) {
    console.error('AI分析失败:', err)
    const errMsg = err.response?.data?.msg || err.message || '网络请求失败'
    analysisError.value = errMsg
    showToast('AI分析失败: ' + errMsg)
  } finally {
    analyzing.value = false
  }
}

// ========== 历史记录 ==========
const loadHistory = async () => {
  historyList.value = [
    { id: 1, date: '2026-04-23', deviceName: '全部', totalTraffic: currentStats.value.totalTraffic, cost: currentStats.value.todayCost, avgBitrate: currentStats.value.avgBitrate, peakBandwidth: peakBandwidth.value, peakTime: peakTimeLabel.value, summary: 'AI分析完成', fullReport: analysisResult.value?.overview || '暂无报告' }
  ]
}

const viewDetail = (record) => { selectedRecord.value = record; showDetailModal.value = true }
const closeDetailModal = () => { showDetailModal.value = false; selectedRecord.value = null }

const handleResize = () => {
  if (trafficChart) trafficChart.resize()
  if (pieChart) pieChart.resize()
}

// 监听设备列表变化，更新饼图
watch(cameraList, () => {
  updatePieChart()
}, { deep: true })

// ========== 生命周期 ==========
onMounted(() => {
  const now = new Date()
  todayDate.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

  loadDevices()
  loadTrafficTrend()
  loadHistory()

  setTimeout(() => {
    initTrafficChart()
    initPieChart()
  }, 100)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (trafficChart) trafficChart.dispose()
  if (pieChart) pieChart.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 样式完全保持不变，与你原有的一样 */
.ai-analysis-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
.page-header h1 { font-size: 24px; font-weight: 600; color: #1a2c3e; margin: 0 0 8px 0; }
.subtitle { color: #6c7a8e; font-size: 14px; margin: 0; }
.header-actions { display: flex; align-items: center; }
.device-selector { display: flex; align-items: center; gap: 12px; background: white; padding: 8px 16px; border-radius: 12px; border: 1px solid #e8ecf2; }
.selector-label { font-size: 13px; color: #6c7a8e; }
.device-select { padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; min-width: 200px; background: white; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.stat-card { background: white; border-radius: 16px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); border: 1px solid #e8ecf2; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.stat-icon.blue { background: rgba(59, 130, 246, 0.1); }
.stat-icon.green { background: rgba(66, 185, 131, 0.1); }
.stat-icon.orange { background: rgba(245, 158, 11, 0.1); }
.stat-icon.purple { background: rgba(139, 92, 246, 0.1); }
.stat-value { font-size: 32px; font-weight: 700; color: #1a2c3e; line-height: 1; }
.stat-value small { font-size: 14px; font-weight: normal; margin-left: 4px; }
.stat-label { font-size: 13px; color: #6c7a8e; margin-top: 4px; }
.stat-trend { font-size: 12px; margin-top: 6px; }
.stat-trend.up { color: #42b983; }
.rank-card, .trend-card, .pie-card, .report-card, .suggest-card, .alert-card, .history-card { background: white; border-radius: 16px; border: 1px solid #e8ecf2; margin-bottom: 24px; overflow: hidden; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e8ecf2; font-weight: 600; flex-wrap: wrap; gap: 12px; }
.total-traffic { font-size: 13px; font-weight: normal; color: #42b983; }
.rank-list { padding: 0 20px 20px; }
.rank-item { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid #f0f4f9; }
.rank-number { width: 30px; font-size: 16px; font-weight: 600; color: #6c7a8e; }
.rank-info { width: 160px; }
.rank-name { font-weight: 500; font-size: 14px; }
.rank-location { font-size: 11px; color: #6c7a8e; }
.rank-bar { flex: 1; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
.rank-fill { height: 100%; background: linear-gradient(90deg, #42b983, #2d8f64); border-radius: 4px; }
.rank-value { width: 70px; font-size: 13px; font-weight: 500; }
.rank-bitrate { width: 80px; font-size: 12px; color: #6c7a8e; }
.rank-cost { width: 70px; font-size: 12px; font-weight: 500; color: #f59e0b; }
.analyze-single-btn { padding: 4px 12px; background: #42b983; border: none; border-radius: 6px; color: white; cursor: pointer; font-size: 11px; }
.single-device-detail { padding: 20px; }
.detail-card { background: #f8fafd; border-radius: 12px; padding: 20px; }
.detail-row { display: flex; padding: 10px 0; border-bottom: 1px solid #e8ecf2; }
.detail-row:last-child { border-bottom: none; }
.detail-label { width: 100px; color: #6c7a8e; font-size: 13px; }
.detail-value { flex: 1; font-weight: 500; color: #1a2c3e; }
.detail-value.highlight { font-size: 20px; color: #42b983; }
.detail-value.save-amount { color: #f59e0b; }
.bitrate-high { color: #ef4444; }
.bitrate-normal { color: #f59e0b; }
.bitrate-low { color: #42b983; }
.back-to-all { margin-top: 16px; padding: 8px 16px; background: none; border: 1px solid #42b983; color: #42b983; border-radius: 8px; cursor: pointer; width: 100%; }
.chart-controls { display: flex; gap: 8px; }
.chart-btn { padding: 4px 12px; background: none; border: 1px solid #e2e8f0; border-radius: 6px; cursor: pointer; font-size: 12px; transition: all 0.2s; }
.chart-btn.active { background: #42b983; border-color: #42b983; color: white; }
.chart-container { padding: 20px; }
.echarts-box { width: 100%; height: 300px; }
.echarts-pie { width: 100%; height: 280px; }
.chart-stats { display: flex; justify-content: space-around; padding: 12px 20px 20px; border-top: 1px solid #e8ecf2; background: #f8fafd; }
.chart-stat { text-align: center; }
.chart-stat .stat-label { display: block; font-size: 11px; color: #6c7a8e; margin-bottom: 4px; }
.chart-stat .stat-value { font-size: 16px; font-weight: 600; color: #1a2c3e; }
.two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
.report-card .card-header { display: flex; justify-content: space-between; align-items: center; }
.report-date { font-size: 13px; color: #6c7a8e; font-weight: normal; }
.analyze-target { font-size: 12px; color: #42b983; background: rgba(66, 185, 131, 0.1); padding: 4px 10px; border-radius: 16px; }
.analyze-btn { padding: 6px 16px; background: linear-gradient(135deg, #8b5cf6, #6d28d9); border: none; border-radius: 20px; color: white; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.3s; }
.analyze-btn:hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(139, 92, 246, 0.4); }
.analyze-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.report-content, .suggest-content, .alert-content { padding: 20px; min-height: 280px; }
.report-section { margin-bottom: 20px; }
.section-title { font-weight: 600; color: #1a2c3e; margin-bottom: 8px; padding-left: 8px; border-left: 3px solid #42b983; }
.section-text { color: #4a5a6e; line-height: 1.6; font-size: 14px; padding-left: 12px; }
.report-placeholder, .suggest-placeholder { text-align: center; padding: 60px 20px; color: #6c7a8e; }
.report-placeholder span, .suggest-placeholder span { font-size: 48px; display: block; margin-bottom: 16px; }
.error-state { text-align: center; padding: 40px 20px; color: #ef4444; }
.error-state span { font-size: 48px; display: block; margin-bottom: 12px; }
.error-detail { font-size: 12px; color: #94a3b8; margin-top: 8px; word-break: break-all; }
.loading-state { text-align: center; padding: 60px 20px; }
.loading-spinner { width: 40px; height: 40px; border: 3px solid #e2e8f0; border-top-color: #8b5cf6; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 16px; }
.loading-spinner-small { width: 24px; height: 24px; border: 2px solid #e2e8f0; border-top-color: #8b5cf6; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 12px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-placeholder { text-align: center; padding: 60px 20px; color: #6c7a8e; }
.suggest-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px 0; border-bottom: 1px solid #f0f4f9; }
.suggest-icon { width: 24px; height: 24px; background: rgba(66, 185, 131, 0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #42b983; font-size: 12px; font-weight: bold; flex-shrink: 0; }
.suggest-text { flex: 1; color: #4a5a6e; font-size: 13px; line-height: 1.5; }
.alert-box { padding: 16px; background: rgba(239, 68, 68, 0.05); border-radius: 8px; border-left: 3px solid #ef4444; display: flex; align-items: center; gap: 10px; }
.alert-icon { font-size: 20px; }
.alert-text { color: #ef4444; font-size: 14px; }
.history-actions { display: flex; gap: 12px; }
.search-input { padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 6px; width: 160px; }
.refresh-btn { padding: 6px 12px; background: none; border: 1px solid #42b983; color: #42b983; border-radius: 6px; cursor: pointer; }
.history-table { padding: 0 20px 20px; }
.table-header, .table-row { display: grid; grid-template-columns: 90px 100px 80px 70px 90px 1fr 70px; gap: 12px; padding: 12px 0; }
.table-header { color: #6c7a8e; font-size: 12px; font-weight: 500; border-bottom: 1px solid #e8ecf2; }
.table-row { font-size: 13px; border-bottom: 1px solid #f0f4f9; }
.summary { color: #6c7a8e; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.detail-btn { padding: 4px 12px; background: none; border: 1px solid #42b983; color: #42b983; border-radius: 4px; cursor: pointer; font-size: 11px; }
.empty-row { text-align: center; padding: 40px; color: #6c7a8e; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: white; border-radius: 16px; width: 550px; max-width: 90%; max-height: 80vh; overflow: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e8ecf2; }
.modal-close { background: none; border: none; font-size: 24px; cursor: pointer; }
.modal-body { padding: 20px; }
.detail-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #e8ecf2; }
.detail-stat label { display: block; font-size: 11px; color: #6c7a8e; margin-bottom: 4px; }
.detail-stat span { font-size: 16px; font-weight: 600; color: #1a2c3e; }
.detail-title { font-weight: 600; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 1px solid #e8ecf2; }
.detail-text { line-height: 1.6; color: #4a5a6e; margin-bottom: 16px; }
.detail-suggest ul { margin: 0; padding-left: 20px; }
.detail-suggest li { margin-bottom: 8px; color: #4a5a6e; }
.toast { position: fixed; bottom: 30px; left: 50%; transform: translateX(-50%); background: #1a2c3e; color: white; padding: 8px 16px; border-radius: 8px; font-size: 13px; z-index: 1001; animation: fadeOut 2s ease forwards; }
@keyframes fadeOut { 0% { opacity: 1; } 70% { opacity: 1; } 100% { opacity: 0; transform: translateX(-50%) translateY(-20px); } }
</style>
