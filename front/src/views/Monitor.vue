<template>
  <div class="monitor-container">
    <!-- 左侧设备列表 -->
    <div class="device-panel">
      <div class="panel-header">
        <div class="header-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <rect x="2" y="7" width="20" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/>
            <path d="M16 21V19C16 17.9391 15.5786 16.9217 14.8284 16.1716C14.0783 15.4214 13.0609 15 12 15C10.9391 15 9.92172 15.4214 9.17157 16.1716C8.42143 16.9217 8 17.9391 8 19V21" stroke="currentColor" stroke-width="1.5"/>
            <circle cx="12" cy="11" r="3" stroke="currentColor" stroke-width="1.5"/>
          </svg>
          <span>设备列表</span>
        </div>
        <div class="device-count">
          <span class="count-number">{{ devices.length }}</span>
          <span>个设备</span>
          <button class="add-btn" @click="goToDeviceManage">+</button>
        </div>
      </div>
      <div class="device-list">
        <div
            v-for="device in sortedDevices"
            :key="device.id"
            class="device-item"
            :class="{ active: currentCamera === device.ip }"
            @click="switchCamera(device)"
        >
          <div class="device-status">
            <span class="status-dot" :class="device.status === 1 ? 'online' : 'offline'"></span>
          </div>
          <div class="device-info">
            <div class="device-name">{{ device.name }}</div>
            <div class="device-ip">{{ device.ip }}</div>
          </div>
        </div>
      </div>
      <div class="panel-footer">
        <button class="add-device-btn" @click="goToDeviceManage">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5V19M5 12H19"/>
          </svg>
          添加设备
        </button>
      </div>
    </div>

    <!-- 中间播放器 -->
    <div class="player-panel">
      <div class="video-header">
        <div class="live-badge">
          <span class="live-dot"></span>
          <span>LIVE</span>
        </div>
        <div class="video-info">
          <span class="camera-name">{{ currentDeviceName }}</span>
          <span class="resolution">1920x1080</span>
        </div>
        <div class="video-actions">
          <button @click="toggleRecording" class="action-btn" :class="{ recording: isRecording }" :title="isRecording ? '停止录制' : '开始录制'">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="8" v-if="!isRecording"/>
              <rect x="8" y="8" width="8" height="8" v-if="isRecording"/>
            </svg>
          </button>
          <button @click="toggleSound" class="action-btn" :title="soundEnabled ? '静音' : '取消静音'">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M11 5L6 9H2V15H6L11 19V5Z"/>
              <path v-if="!soundEnabled" d="M23 9L17 15M17 9L23 15"/>
            </svg>
          </button>
          <button @click="refreshVideo" class="action-btn" title="刷新">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M23 4V10H17M1 20V14H7M3.51 9C4.15817 7.13126 5.38724 5.51804 7.00933 4.38923C8.63143 3.26041 10.5579 2.67253 12.5278 2.69942C14.4977 2.72631 16.4077 3.36663 18 4.55M20.49 15C19.8418 16.8687 18.6128 18.482 16.9907 19.6108C15.3686 20.7396 13.4421 21.3275 11.4722 21.3006C9.5023 21.2737 7.59228 20.6334 6 19.45"/>
            </svg>
          </button>
          <button @click="toggleFullscreen" class="action-btn" title="全屏">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M8 3H5C3.89543 3 3 3.89543 3 5V8M21 8V5C21 3.89543 20.1046 3 19 3H16M16 21H19C20.1046 21 21 20.1046 21 19V16M3 16V19C3 20.1046 3.89543 21 5 21H8"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="video-wrapper">
        <video
            ref="videoRef"
            id="videoElement"
            autoplay
            muted
            playsinline
            class="video-style"
        ></video>
        <div v-if="loading" class="video-loader">
          <div class="spinner"></div>
          <p>正在尝试连接流媒体服务器...</p>
        </div>
        <div v-if="recordingStatusMsg" class="recording-status" :class="recordingStatusClass">
          {{ recordingStatusMsg }}
        </div>
      </div>
      <div class="video-footer">
        <div class="fps-info">
          <span>📡 实时流 (HTTP-FLV)</span>
          <span style="color: #42b983">⚡ AI 智能检测已开启</span>
        </div>
      </div>
    </div>

    <!-- 右侧控制面板 -->
    <div class="control-panel">
      <div class="panel-header">
        <div class="header-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5"/>
            <path d="M12 8V12L14 14" stroke="currentColor" stroke-width="1.5"/>
          </svg>
          <span>云台控制</span>
        </div>
      </div>

      <!-- AI 检测状态显示（无开关） -->
      <div class="ai-section">
        <div class="ai-header">
          <div class="header-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="9"/>
              <path d="M12 8V12L15 15"/>
            </svg>
            <span>🤖 AI 智能检测</span>
          </div>
          <div class="ai-badge" :class="aiReady ? 'online' : 'offline'">
            {{ aiReady ? '已就绪' : '连接中' }}
          </div>
        </div>

        <div class="ai-status">
          <div class="ai-stats">
            <div class="ai-stat">
              <span class="ai-stat-label">陌生人</span>
              <span class="ai-stat-value person-count">{{ strangerCount }} 人</span>
            </div>
            <div class="ai-stat">
              <span class="ai-stat-label">摔倒</span>
              <span class="ai-stat-value" :class="fallenDetected ? 'fall' : ''">
                {{ fallenDetected ? '⚠️ 是' : '正常' }}
              </span>
            </div>
          </div>
          <div v-if="lastDetectTime" class="ai-time">
            📷 最后检测: {{ lastDetectTime }}
          </div>
          <div v-if="showAlert" class="ai-alert">
            ⚠️ {{ alertMessage }}
          </div>
          <button class="view-screenshots-btn" @click="goToScreenshots">
            📸 查看告警截图
          </button>
          <button class="view-screenshots-btn" @click="goToFaceManage" style="margin-top: 8px;">
            😀 人脸管理
          </button>
        </div>
      </div>

      <div class="ptz-controls">
        <div class="d-pad">
          <button @mousedown="startPtz('UP')" @mouseup="stopPtz" @mouseleave="stopPtz" class="ptz-btn top">▲</button>
          <div class="mid-row">
            <button @mousedown="startPtz('LEFT')" @mouseup="stopPtz" @mouseleave="stopPtz" class="ptz-btn left">◀</button>
            <div class="center-btn">●</div>
            <button @mousedown="startPtz('RIGHT')" @mouseup="stopPtz" @mouseleave="stopPtz" class="ptz-btn right">▶</button>
          </div>
          <button @mousedown="startPtz('DOWN')" @mouseup="stopPtz" @mouseleave="stopPtz" class="ptz-btn bottom">▼</button>
        </div>
        <div class="control-group">
          <label>变焦控制</label>
          <div class="control-buttons">
            <button @mousedown="startPtz('ZOOM_IN')" @mouseup="stopPtz" @mouseleave="stopPtz" class="ctrl-btn">🔍 + 放大</button>
            <button @mousedown="startPtz('ZOOM_OUT')" @mouseup="stopPtz" @mouseleave="stopPtz" class="ctrl-btn">🔍 - 缩小</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import mpegts from 'mpegts.js'
import axios from 'axios'
import request from '../api/request.js'

const router = useRouter()
const loading = ref(true)
const soundEnabled = ref(false)
const currentCamera = ref('')
const currentDeviceId = ref(null)
const videoRef = ref(null)
const toastMsg = ref('')
const devices = ref([])
const currentDeviceName = ref('')
let player = null

// 录制相关
const isRecording = ref(false)
const recordingStatusMsg = ref('')
const recordingStatusClass = ref('')
const currentRecordingId = ref(null)
const currentStreamId = ref(null)
let recordingStartTime = null

// AI 检测相关（始终开启）
const aiReady = ref(false)
const strangerCount = ref(0)
const fallenDetected = ref(false)
const lastDetectTime = ref('')
const showAlert = ref(false)
const alertMessage = ref('')
let aiInterval = null
let alertTimeout = null

// Python AI 地址（不需要 JWT）
const PYTHON_AI_URL = 'http://localhost:5001'

const MIN_RECORD_SECONDS = 10

const sortedDevices = computed(() => {
  return [...devices.value].sort((a, b) => {
    if (a.status !== b.status) return b.status - a.status
    return (a.name || '').localeCompare(b.name || '', 'zh-CN')
  })
})

const showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 2000)
}

const showRecordingStatus = (msg, isError = false) => {
  recordingStatusMsg.value = msg
  recordingStatusClass.value = isError ? 'status-error' : 'status-normal'
  setTimeout(() => {
    if (recordingStatusMsg.value === msg) {
      recordingStatusMsg.value = ''
    }
  }, 3000)
}

const refreshRecordList = () => {
  window.dispatchEvent(new CustomEvent('refresh-records'))
}

// 录制相关函数
const startRecording = async () => {
  if (!currentDeviceId.value) {
    showToast('请先选择设备')
    return
  }
  try {
    const userId = 1
    const res = await request.post('/api/video/record/start', null, {
      params: { deviceId: currentDeviceId.value, userId: userId }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      currentRecordingId.value = data.historyId
      currentStreamId.value = data.streamId
      isRecording.value = true
      recordingStartTime = Date.now()
      showRecordingStatus('🔴 正在录制...')
      showToast('开始录制成功')
    } else {
      showToast(res.data.msg || '开始录制失败')
    }
  } catch (err) {
    console.error('开始录制失败:', err)
    showToast('开始录制失败: ' + (err.response?.data?.msg || err.message))
  }
}

const stopRecording = async () => {
  if (!currentRecordingId.value) {
    showToast('没有正在录制的任务')
    return
  }
  const duration = (Date.now() - recordingStartTime) / 1000
  const tooShort = duration < MIN_RECORD_SECONDS

  try {
    const res = await request.post('/api/video/record/stop', null, {
      params: {
        recordingId: currentRecordingId.value,
        streamId: currentStreamId.value
      }
    })
    isRecording.value = false
    currentRecordingId.value = null
    currentStreamId.value = null
    recordingStartTime = null

    if (tooShort) {
      showRecordingStatus(`❌ 录制取消（仅${duration.toFixed(1)}秒）`)
      showToast(`录制时间太短（${duration.toFixed(1)}秒），至少需要 ${MIN_RECORD_SECONDS} 秒才能保存`)
      return
    }

    if (res.data.code === 200) {
      if (res.data.data && res.data.data.filePath) {
        showRecordingStatus('✅ 录制完成，已保存')
        showToast(`录制成功！时长：${duration.toFixed(1)}秒`)
        refreshRecordList()
      } else {
        showRecordingStatus('⚠️ 录制完成，文件生成中')
        showToast('录制完成，请稍后查看回放')
      }
    } else {
      showToast(res.data.msg || '停止录制失败')
    }
  } catch (err) {
    console.error('停止录制失败:', err)
    showToast('停止录制失败: ' + (err.response?.data?.msg || err.message))
    isRecording.value = false
    currentRecordingId.value = null
    currentStreamId.value = null
    recordingStartTime = null
  }
}

const toggleRecording = () => {
  if (isRecording.value) {
    stopRecording()
  } else {
    startRecording()
  }
}

// 检查 Python AI 服务状态
const checkAIStatus = async () => {
  try {
    const res = await axios.get(`${PYTHON_AI_URL}/health`)
    if (res.data.status === 'ok') {
      aiReady.value = true
      console.log('Python AI 服务已就绪')
    } else {
      aiReady.value = false
    }
  } catch (err) {
    console.error('Python AI 服务连接失败:', err)
    aiReady.value = false
  }
}

// 截图并调用 Python AI 检测
const captureAndDetect = async () => {
  const video = videoRef.value
  if (!video || video.paused || video.ended || video.readyState < 2) return

  try {
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

    canvas.toBlob(async (blob) => {
      const formData = new FormData()
      formData.append('file', blob, 'frame.jpg')

      try {
        const res = await axios.post(`${PYTHON_AI_URL}/detect`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 5000
        })

        if (res.data.code === 200 && res.data.data) {
          const data = res.data.data
          const hasStranger = data.has_stranger
          const sCount = data.stranger_count || 0
          const fallen = data.fallen

          strangerCount.value = sCount
          fallenDetected.value = fallen
          lastDetectTime.value = new Date().toLocaleTimeString()

          // 陌生人告警
          if (hasStranger && !showAlert.value) {
            showAlert.value = true
            alertMessage.value = `⚠️ 检测到 ${sCount} 个陌生人！`
            if (alertTimeout) clearTimeout(alertTimeout)
            alertTimeout = setTimeout(() => {
              showAlert.value = false
            }, 5000)
          }

          // 摔倒告警
          if (fallen && !showAlert.value) {
            showAlert.value = true
            alertMessage.value = `⚠️ 检测到摔倒！`
            if (alertTimeout) clearTimeout(alertTimeout)
            alertTimeout = setTimeout(() => {
              showAlert.value = false
            }, 5000)
          }
        }
      } catch (err) {
        console.error('AI 检测失败:', err)
      }
    }, 'image/jpeg', 0.7)
  } catch (err) {
    console.error('截图失败:', err)
  }
}

// 启动 AI 检测（始终运行）
const startAI = async () => {
  await checkAIStatus()
  if (aiReady.value) {
    if (aiInterval) clearInterval(aiInterval)
    aiInterval = setInterval(captureAndDetect, 1000)
    console.log('AI 智能检测已启动')
  } else {
    console.warn('AI 服务未就绪，10秒后重试')
    setTimeout(startAI, 10000)
  }
}

// 跳转函数
const goToDeviceManage = () => router.push('/devices')
const goToScreenshots = () => router.push('/screenshots')
const goToFaceManage = () => router.push('/face-manage')

// 云台控制 - 开始转动
const startPtz = async (direction) => {
  if (!currentCamera.value) return
  try {
    await request.post('/api/ptz/control', null, {
      params: { deviceIp: currentCamera.value, action: direction }
    })
  } catch (err) {
    console.error('云台控制失败:', err)
  }
}

// 云台控制 - 停止转动
const stopPtz = async () => {
  if (!currentCamera.value) return
  try {
    await request.post('/api/ptz/control', null, {
      params: { deviceIp: currentCamera.value, action: 'STOP' }
    })
  } catch (err) {
    console.error('停止云台失败:', err)
  }
}

// 加载设备列表
const loadDevices = async () => {
  try {
    const res = await request.get('/api/video/devices')
    if (res.data.code === 200) {
      devices.value = res.data.data || []
      if (devices.value.length > 0 && !currentCamera.value) {
        const firstOnline = devices.value.find(d => d.status === 1) || devices.value[0]
        currentCamera.value = firstOnline.ip
        currentDeviceId.value = firstOnline.id
        currentDeviceName.value = firstOnline.name
        initMonitor()
      }
    }
  } catch (err) {
    console.error('加载设备失败:', err)
    showToast('无法连接到后端服务器，请检查后端 8081 是否开启')
  }
}

const initMonitor = async () => {
  if (!currentCamera.value) {
    loading.value = false
    return
  }
  loading.value = true

  try {
    const res = await request.get('/api/video/play', { params: { cameraIp: currentCamera.value } })
    if (res.data.code === 200) {
      let finalUrl = res.data.data
      console.log('后端返回原始地址:', finalUrl)

      await nextTick()
      if (player) {
        player.unload()
        player.detachMediaElement()
        player.destroy()
        player = null
      }

      if (mpegts.getFeatureList().mseLivePlayback) {
        player = mpegts.createPlayer({
          type: 'flv',
          isLive: true,
          url: finalUrl,
          hasAudio: false
        }, {
          enableStashBuffer: false,
          stashInitialSize: 128,
          lazyLoad: false,
          autoCleanupSourceBuffer: true
        })

        player.attachMediaElement(videoRef.value)
        player.load()
        player.play().then(() => {
          loading.value = false
          console.log('播放成功，当前源:', finalUrl)
        }).catch((e) => {
          loading.value = false
          console.error('播放失败:', e)
        })

        player.on(mpegts.Events.ERROR, (type, detail) => {
          console.error('播放器错误类型:', type, detail)
          loading.value = false
        })
      }
    }
  } catch (err) {
    console.error('初始化视频流失败:', err)
    loading.value = false
  }
}

const switchCamera = async (device) => {
  if (currentCamera.value === device.ip) return

  if (isRecording.value) {
    await stopRecording()
  }

  currentCamera.value = device.ip
  currentDeviceId.value = device.id
  currentDeviceName.value = device.name
  initMonitor()
}

const refreshVideo = () => initMonitor()
const toggleSound = () => {
  soundEnabled.value = !soundEnabled.value
  if (videoRef.value) videoRef.value.muted = !soundEnabled.value
}
const toggleFullscreen = () => {
  const elem = document.querySelector('.player-panel')
  if (elem?.requestFullscreen) elem.requestFullscreen()
}

onMounted(() => {
  loadDevices()
  // 自动启动 AI 检测
  startAI()
})

onUnmounted(() => {
  if (player) player.destroy()
  if (aiInterval) clearInterval(aiInterval)
  if (alertTimeout) clearTimeout(alertTimeout)
})
</script>

<style scoped>
/* 所有原有样式保持不变，添加 AI 状态徽章样式 */
.monitor-container { height: 100%; display: flex; gap: 16px; overflow: hidden; }
.device-panel { width: 280px; background: rgba(255,255,255,0.95); backdrop-filter: blur(10px); border-radius: 16px; border: 1px solid rgba(66,185,131,0.2); display: flex; flex-direction: column; overflow: hidden; }
.panel-header { padding: 16px; border-bottom: 1px solid rgba(0,0,0,0.08); display: flex; justify-content: space-between; align-items: center; }
.header-title { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #1a2c3e; }
.device-count { display: flex; align-items: baseline; gap: 4px; font-size: 12px; color: #6c7a8e; }
.count-number { font-size: 18px; font-weight: bold; color: #42b983; }
.add-btn { background: #42b983; border: none; width: 22px; height: 22px; border-radius: 6px; cursor: pointer; color: white; font-size: 16px; margin-left: 6px; }
.device-list { flex: 1; padding: 12px; overflow-y: auto; }
.device-item { display: flex; align-items: center; gap: 12px; padding: 12px; margin-bottom: 8px; background: #f8fafd; border-radius: 12px; cursor: pointer; transition: all 0.3s; border: 1px solid transparent; }
.device-item:hover { background: #e8f4ef; transform: translateX(4px); }
.device-item.active { background: linear-gradient(135deg, rgba(66,185,131,0.1), rgba(66,185,131,0.05)); border-color: #42b983; }
.device-status .status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.status-dot.online { background: #42b983; box-shadow: 0 0 6px #42b983; }
.status-dot.offline { background: #ef4444; }
.device-info { flex: 1; }
.device-name { font-weight: 600; font-size: 14px; color: #1a2c3e; }
.device-ip { font-size: 11px; color: #6c7a8e; }
.panel-footer { padding: 12px; border-top: 1px solid rgba(0,0,0,0.08); }
.add-device-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 8px; background: linear-gradient(135deg, #42b983, #2d8f64); border: none; border-radius: 10px; color: white; font-weight: 500; cursor: pointer; }
.player-panel { flex: 1; background: rgba(255,255,255,0.95); backdrop-filter: blur(10px); border-radius: 16px; border: 1px solid rgba(66,185,131,0.2); display: flex; flex-direction: column; overflow: hidden; }
.video-header { padding: 12px 16px; border-bottom: 1px solid rgba(0,0,0,0.08); display: flex; align-items: center; gap: 16px; }
.live-badge { display: flex; align-items: center; gap: 6px; padding: 4px 10px; background: rgba(239,68,68,0.1); border-radius: 20px; color: #ef4444; font-size: 12px; font-weight: bold; }
.live-dot { width: 8px; height: 8px; background: #ef4444; border-radius: 50%; animation: blink 1s infinite; }
@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
.video-info { flex: 1; }
.camera-name { font-weight: 600; font-size: 14px; }
.resolution { font-size: 11px; color: #6c7a8e; margin-left: 8px; }
.video-actions { display: flex; gap: 6px; }
.action-btn { width: 30px; height: 30px; background: #f0f4f9; border: none; border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.action-btn.recording { background: #ef4444; color: white; }
.action-btn.recording svg { stroke: white; }
.action-btn:hover { background: #42b983; color: white; }
.video-wrapper { flex: 1; background: #000; position: relative; }
.video-style { width: 100%; height: 100%; object-fit: contain; }
.video-loader { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.9); display: flex; flex-direction: column; align-items: center; justify-content: center; color: white; }
.spinner { width: 40px; height: 40px; border: 3px solid rgba(66,185,131,0.2); border-top-color: #42b983; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.recording-status { position: absolute; top: 16px; right: 16px; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; animation: pulse 1.5s infinite; }
.recording-status.status-normal { background: rgba(239,68,68,0.9); color: white; }
.recording-status.status-error { background: rgba(0,0,0,0.8); color: #ffaa00; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.7; } }
.video-footer { padding: 8px 16px; border-top: 1px solid rgba(0,0,0,0.08); background: #f8fafd; }
.fps-info { display: flex; gap: 16px; font-size: 11px; color: #6c7a8e; }
.control-panel { width: 280px; background: rgba(255,255,255,0.95); backdrop-filter: blur(10px); border-radius: 16px; border: 1px solid rgba(66,185,131,0.2); display: flex; flex-direction: column; overflow: hidden; }

/* AI 检测样式 */
.ai-section { padding: 16px; border-bottom: 1px solid rgba(0,0,0,0.08); }
.ai-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.ai-badge { padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: bold; }
.ai-badge.online { background: rgba(66,185,131,0.2); color: #42b983; }
.ai-badge.offline { background: rgba(255,77,79,0.2); color: #ff4d4f; animation: pulse 1s infinite; }
.ai-status { margin-top: 12px; }
.ai-stats { display: flex; gap: 16px; margin-bottom: 8px; }
.ai-stat { flex: 1; }
.ai-stat-label { display: block; font-size: 10px; color: #8a99aa; margin-bottom: 4px; }
.ai-stat-value { font-size: 20px; font-weight: bold; }
.person-count { color: #ff4d4f; }
.ai-stat-value.fall { color: #ff4d4f; animation: pulse 1s infinite; }
.ai-time { font-size: 11px; color: #8a99aa; margin-bottom: 8px; }
.ai-alert { padding: 8px 12px; background: rgba(255,77,79,0.15); border-left: 3px solid #ff4d4f; border-radius: 6px; font-size: 12px; color: #ff6b6b; margin-bottom: 10px; animation: pulse 1s infinite; }
.view-screenshots-btn { width: 100%; padding: 8px; background: linear-gradient(135deg, #42b983, #2d8f64); border: none; border-radius: 8px; color: white; font-weight: 500; cursor: pointer; font-size: 12px; }
.view-screenshots-btn:hover { opacity: 0.9; }

.ptz-controls { flex: 1; padding: 20px; }
.d-pad { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; }
.ptz-btn { width: 50px; height: 50px; background: #f0f4f9; border: 2px solid rgba(66,185,131,0.3); border-radius: 12px; cursor: pointer; font-size: 20px; color: #42b983; transition: all 0.2s; }
.ptz-btn:hover { background: #42b983; color: white; }
.top { margin-bottom: 10px; }
.bottom { margin-top: 10px; }
.mid-row { display: flex; gap: 16px; align-items: center; }
.center-btn { width: 40px; height: 40px; background: rgba(66,185,131,0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #42b983; }
.control-group { margin-bottom: 20px; }
.control-group label { display: block; font-size: 11px; font-weight: 600; color: #6c7a8e; margin-bottom: 8px; text-transform: uppercase; }
.control-buttons { display: flex; gap: 10px; }
.ctrl-btn { flex: 1; padding: 8px; background: #f0f4f9; border: 1px solid rgba(66,185,131,0.3); border-radius: 8px; cursor: pointer; font-size: 11px; transition: all 0.2s; }
.ctrl-btn:hover { background: #42b983; color: white; }
.toast { position: fixed; bottom: 30px; left: 50%; transform: translateX(-50%); background: #1a2c3e; color: white; padding: 8px 16px; border-radius: 8px; font-size: 13px; z-index: 9999; animation: fadeOut 2s ease forwards; }
@keyframes fadeOut { 0% { opacity: 1; } 70% { opacity: 1; } 100% { opacity: 0; transform: translateX(-50%) translateY(-20px); } }
</style>
