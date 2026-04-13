<template>
  <div class="monitor-layout">
    <!-- 顶部导航栏 -->
    <div class="header-bar">
      <div class="brand">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">HEALING STREAM</span>
          <span class="brand-version">V3.0 Pro</span>
        </div>
      </div>
      <div class="user-panel">
        <div class="status-indicator">
          <span class="status-dot online"></span>
          <span>系统正常</span>
        </div>
        <div class="user-info">
          <span class="user-avatar">👨‍💻</span>
          <span>管理员: admin</span>
        </div>
        <button @click="logout" class="logout-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M9 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H9" stroke="currentColor" stroke-width="2"/>
            <path d="M16 17L21 12L16 7" stroke="currentColor" stroke-width="2"/>
            <path d="M21 12H9" stroke="currentColor" stroke-width="2"/>
          </svg>
          退出系统
        </button>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧设备列表 -->
      <div class="device-panel">
        <div class="panel-header">
          <div class="header-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <rect x="2" y="7" width="20" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/>
              <path d="M16 21V19C16 17.9391 15.5786 16.9217 14.8284 16.1716C14.0783 15.4214 13.0609 15 12 15C10.9391 15 9.92172 15.4214 9.17157 16.1716C8.42143 16.9217 8 17.9391 8 19V21" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="12" cy="11" r="3" stroke="currentColor" stroke-width="1.5"/>
            </svg>
            <span>设备资源管理</span>
          </div>
          <div class="device-count">
            <span class="count-number">{{ devices.length }}</span>
            <span>个设备</span>
            <button class="add-device-btn-small" @click="openAddDeviceModal">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="device-list">
          <div
              v-for="device in devices"
              :key="device.id"
              class="device-item"
              :class="{ active: currentCamera === device.ip }"
              @click="switchCamera(device)"
          >
            <div class="device-status">
              <span class="status-dot" :class="device.status"></span>
            </div>
            <div class="device-info">
              <div class="device-name">{{ device.name }}</div>
              <div class="device-ip">{{ device.ip }}</div>
            </div>
            <div class="device-actions">
              <button class="device-action-btn" @click.stop="viewDeviceDetail(device)" title="查看详情">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M22 12C22 12 19 18 12 18C5 18 2 12 2 12C2 12 5 6 12 6C19 6 22 12 22 12Z" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </button>
              <button class="device-action-btn" @click.stop="openEditDeviceModal(device)" title="编辑">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M17 3L21 7M7 21H3V17L17 3L21 7L7 21Z" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </button>
              <button class="device-action-btn delete" @click.stop="deleteDevice(device.id)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M4 7H20M10 11V16M14 11V16M5 7L6 19C6 19.5304 6.21071 20.0391 6.58579 20.4142C6.96086 20.7893 7.46957 21 8 21H16C16.5304 21 17.0391 20.7893 17.4142 20.4142C17.7893 20.0391 18 19.5304 18 19L19 7M9 7V4C9 3.73478 9.10536 3.48043 9.29289 3.29289C9.48043 3.10536 9.73478 3 10 3H14C14.2652 3 14.5196 3.10536 14.7071 3.29289C14.8946 3.48043 15 3.73478 15 4V7" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="panel-footer">
          <button class="add-device-btn" @click="openAddDeviceModal">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2"/>
            </svg>
            添加设备
          </button>
        </div>
      </div>

      <!-- 中间视频播放器 -->
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
            <button @click="toggleSound" class="action-btn" :title="soundEnabled ? '静音' : '取消静音'">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M11 5L6 9H2V15H6L11 19V5Z" stroke="currentColor" stroke-width="1.5"/>
                <path v-if="!soundEnabled" d="M23 9L17 15M17 9L23 15" stroke="currentColor" stroke-width="1.5"/>
              </svg>
            </button>
            <button @click="refreshVideo" class="action-btn" title="刷新">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M23 4V10H17M1 20V14H7M3.51 9C4.15817 7.13126 5.38724 5.51804 7.00933 4.38923C8.63143 3.26041 10.5579 2.67253 12.5278 2.69942C14.4977 2.72631 16.4077 3.36663 18 4.55M20.49 15C19.8418 16.8687 18.6128 18.482 16.9907 19.6108C15.3686 20.7396 13.4421 21.3275 11.4722 21.3006C9.5023 21.2737 7.59228 20.6334 6 19.45" stroke="currentColor" stroke-width="1.5"/>
              </svg>
            </button>
            <button @click="toggleFullscreen" class="action-btn" title="全屏">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M8 3H5C3.89543 3 3 3.89543 3 5V8M21 8V5C21 3.89543 20.1046 3 19 3H16M16 21H19C20.1046 21 21 20.1046 21 19V16M3 16V19C3 20.1046 3.89543 21 5 21H8" stroke="currentColor" stroke-width="1.5"/>
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
            <p>正在初始化视频通道...</p>
            <p class="loading-tip">首次加载可能需要3-5秒</p>
          </div>
        </div>
        <div class="video-footer">
          <div class="fps-info">
            <span>📡 实时流 (HTTP-FLV)</span>
            <span>⚡ 低延迟渲染模式</span>
          </div>
        </div>
      </div>

      <!-- 右侧云台控制 -->
      <div class="control-panel">
        <div class="panel-header">
          <div class="header-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5"/>
              <path d="M12 8V12L14 14" stroke="currentColor" stroke-width="1.5"/>
            </svg>
            <span>云台控制中心</span>
          </div>
        </div>

        <div class="ptz-controls">
          <div class="d-pad">
            <button @click="ptzCtrl('UP')" class="ptz-btn top" title="UP">▲</button>
            <div class="mid-row">
              <button @click="ptzCtrl('LEFT')" class="ptz-btn left" title="LEFT">◀</button>
              <div class="center-btn">●</div>
              <button @click="ptzCtrl('RIGHT')" class="ptz-btn right" title="RIGHT">▶</button>
            </div>
            <button @click="ptzCtrl('DOWN')" class="ptz-btn bottom" title="DOWN">▼</button>
          </div>

          <div class="zoom-controls">
            <div class="control-group">
              <label>变焦控制</label>
              <div class="control-buttons">
                <button @click="ptzCtrl('ZOOM_IN')" class="ctrl-btn">🔍 + 放大</button>
                <button @click="ptzCtrl('ZOOM_OUT')" class="ctrl-btn">🔍 - 缩小</button>
              </div>
            </div>
          </div>

          <div class="preset-controls">
            <div class="control-group">
              <label>预设位</label>
              <div class="preset-buttons">
                <button v-for="i in 4" :key="i" @click="ptzCtrl(`PRESET_${i}`)" class="preset-btn">
                  {{ i }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设备详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>设备详细信息</h3>
          <button class="modal-close" @click="closeDetailModal">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-item">
            <label>设备名称：</label>
            <span>{{ selectedDevice?.name }}</span>
          </div>
          <div class="detail-item">
            <label>设备IP：</label>
            <span>{{ selectedDevice?.ip }}</span>
          </div>
          <div class="detail-item">
            <label>设备类型：</label>
            <span>{{ selectedDevice?.type }}</span>
          </div>
          <div class="detail-item">
            <label>设备品牌：</label>
            <span>{{ selectedDevice?.brand || '海康威视' }}</span>
          </div>
          <div class="detail-item">
            <label>状态：</label>
            <span class="status-badge" :class="selectedDevice?.status">{{ selectedDevice?.status === 'online' ? '在线' : '离线' }}</span>
          </div>
          <div class="detail-item">
            <label>最后在线：</label>
            <span>{{ selectedDevice?.lastSeen || '刚刚' }}</span>
          </div>
          <div class="detail-item">
            <label>RTSP地址：</label>
            <span class="rtsp-url">rtsp://{{ selectedDevice?.username || 'admin' }}:****@{{ selectedDevice?.ip }}:554/Streaming/Channels/101</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑设备弹窗 -->
    <div v-if="showDeviceModal" class="modal-overlay" @click="closeDeviceModal">
      <div class="modal-content device-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑设备' : '添加设备' }}</h3>
          <button class="modal-close" @click="closeDeviceModal">×</button>
        </div>
        <div class="modal-body">
          <div class="input-group-modal">
            <label>设备名称 *</label>
            <input v-model="deviceForm.name" type="text" placeholder="例如：海康摄像头_主出入口">
          </div>
          <div class="input-group-modal">
            <label>设备IP *</label>
            <input v-model="deviceForm.ip" type="text" placeholder="例如：192.168.1.100">
          </div>
          <div class="input-group-modal">
            <label>设备类型</label>
            <select v-model="deviceForm.type">
              <option value="IPC">IPC (网络摄像机)</option>
              <option value="NVR">NVR (网络录像机)</option>
              <option value="DVR">DVR (数字录像机)</option>
            </select>
          </div>
          <div class="input-group-modal">
            <label>设备品牌</label>
            <select v-model="deviceForm.brand">
              <option value="海康威视">海康威视</option>
              <option value="大华">大华</option>
              <option value="宇视">宇视</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div class="input-group-modal">
            <label>RTSP用户名</label>
            <input v-model="deviceForm.username" type="text" placeholder="默认：admin">
          </div>
          <div class="input-group-modal">
            <label>RTSP密码</label>
            <input v-model="deviceForm.password" type="password" placeholder="摄像头登录密码">
          </div>
          <div class="input-group-modal">
            <label>通道号</label>
            <input v-model.number="deviceForm.channel" type="number" placeholder="默认：101">
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeDeviceModal">取消</button>
          <button class="save-btn" @click="saveDevice">{{ isEditing ? '保存修改' : '添加设备' }}</button>
        </div>
      </div>
    </div>

    <!-- 云台提示 -->
    <div v-if="ptzToast" class="ptz-toast">{{ ptzToast }}</div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import mpegts from 'mpegts.js';
import { getPlayUrl } from '../api/video';

const router = useRouter();
const loading = ref(true);
const soundEnabled = ref(false);
const currentCamera = ref('');
const videoRef = ref(null);
const ptzToast = ref('');
let player = null;

// 设备列表
const devices = ref([]);

// 弹窗状态
const showDetailModal = ref(false);
const showDeviceModal = ref(false);
const isEditing = ref(false);
const selectedDevice = ref(null);
const deviceForm = ref({
  id: null,
  name: '',
  ip: '',
  type: 'IPC',
  brand: '海康威视',
  username: 'admin',
  password: '',
  channel: 101,
  status: 'online'
});

const currentDeviceName = ref('');

// 从本地存储加载设备
const loadDevices = () => {
  const saved = localStorage.getItem('devices');
  if (saved) {
    devices.value = JSON.parse(saved);
  } else {
    devices.value = [
      { id: 1, name: '海康摄像头_主出入口', ip: '192.168.137.100', type: 'IPC', brand: '海康威视', username: 'admin', password: 'TDKb2101357', channel: 101, status: 'online', lastSeen: new Date().toLocaleString() },
      { id: 2, name: '大华摄像头_停车场', ip: '192.168.137.101', type: 'IPC', brand: '大华', username: 'admin', password: '123456', channel: 101, status: 'offline', lastSeen: '2026-04-11 18:30:00' }
    ];
    saveDevices();
  }

  const firstOnline = devices.value.find(d => d.status === 'online');
  if (firstOnline) {
    currentCamera.value = firstOnline.ip;
    currentDeviceName.value = firstOnline.name;
  }
};

const saveDevices = () => {
  localStorage.setItem('devices', JSON.stringify(devices.value));
};

// 切换摄像头
const switchCamera = (device) => {
  if (currentCamera.value === device.ip) return;
  currentCamera.value = device.ip;
  currentDeviceName.value = device.name;
  initMonitor();
};

// 查看详情
const viewDeviceDetail = (device) => {
  selectedDevice.value = device;
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedDevice.value = null;
};

// 添加/编辑设备
const openAddDeviceModal = () => {
  isEditing.value = false;
  deviceForm.value = { id: null, name: '', ip: '', type: 'IPC', brand: '海康威视', username: 'admin', password: '', channel: 101, status: 'online' };
  showDeviceModal.value = true;
};

const openEditDeviceModal = (device) => {
  isEditing.value = true;
  deviceForm.value = { ...device };
  showDeviceModal.value = true;
};

const closeDeviceModal = () => {
  showDeviceModal.value = false;
};

const saveDevice = () => {
  if (!deviceForm.value.name || !deviceForm.value.ip) {
    alert('请填写设备名称和IP地址');
    return;
  }

  if (isEditing.value) {
    const index = devices.value.findIndex(d => d.id === deviceForm.value.id);
    if (index !== -1) {
      devices.value[index] = { ...deviceForm.value, lastSeen: devices.value[index].lastSeen };
      saveDevices();
    }
  } else {
    const newId = Math.max(...devices.value.map(d => d.id), 0) + 1;
    devices.value.push({ ...deviceForm.value, id: newId, status: 'offline', lastSeen: '未连接' });
    saveDevices();
  }
  closeDeviceModal();
};

// 删除设备
const deleteDevice = (id) => {
  if (confirm('确定要删除这个设备吗？')) {
    const device = devices.value.find(d => d.id === id);
    devices.value = devices.value.filter(d => d.id !== id);
    saveDevices();

    if (currentCamera.value === device?.ip) {
      const firstOnline = devices.value.find(d => d.status === 'online');
      if (firstOnline) switchCamera(firstOnline);
      else if (devices.value.length > 0) switchCamera(devices.value[0]);
    }
  }
};

// 初始化播放器
const initMonitor = async () => {
  if (!currentCamera.value) return;
  loading.value = true;

  try {
    const streamId = `camera_${currentCamera.value.replace(/\./g, '_')}`;
    const finalUrl = `http://192.168.6.132:9999/live/${streamId}.live.flv`;
    console.log("📺 播放地址:", finalUrl);

    await nextTick();

    if (player) {
      player.unload();
      player.detachMediaElement();
      player.destroy();
      player = null;
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
        lazyLoad: false
      });

      player.attachMediaElement(videoRef.value);
      player.load();

      player.play().then(() => {
        loading.value = false;
      }).catch(err => {
        console.warn("自动播放被拦截:", err);
        loading.value = false;
      });

      player.on(mpegts.Events.ERROR, (type, detail) => {
        console.error("播放错误:", type, detail);
        loading.value = false;
      });
    } else {
      console.error("浏览器不支持 MSE");
      loading.value = false;
    }
  } catch (err) {
    console.error("初始化失败:", err);
    loading.value = false;
  }
};

// 云台控制 - 海康摄像头 HTTP API
const ptzCtrl = async (direction) => {
  console.log("🎮 云台指令:", direction);

  // 获取当前选中的设备
  const device = devices.value.find(d => d.ip === currentCamera.value);
  if (!device) {
    console.error("未找到设备");
    showToast("请先选择设备");
    return;
  }

  // 按钮点击反馈
  const btn = document.querySelector(`.ptz-btn.${direction.toLowerCase()}`) ||
      Array.from(document.querySelectorAll('.ptz-btn, .ctrl-btn, .preset-btn'))
          .find(b => b.title === direction);
  if (btn) {
    btn.style.transform = 'scale(0.9)';
    setTimeout(() => btn.style.transform = '', 150);
  }

  // 海康云台控制 API 映射
  const ptzApiMap = {
    'UP': { cmd: 'continuous', pan: 0, tilt: 20, zoom: 0 },
    'DOWN': { cmd: 'continuous', pan: 0, tilt: -20, zoom: 0 },
    'LEFT': { cmd: 'continuous', pan: -20, tilt: 0, zoom: 0 },
    'RIGHT': { cmd: 'continuous', pan: 20, tilt: 0, zoom: 0 },
    'ZOOM_IN': { cmd: 'continuous', pan: 0, tilt: 0, zoom: 20 },
    'ZOOM_OUT': { cmd: 'continuous', pan: 0, tilt: 0, zoom: -20 },
    'PRESET_1': { cmd: 'preset', presetId: 1, action: 'goto' },
    'PRESET_2': { cmd: 'preset', presetId: 2, action: 'goto' },
    'PRESET_3': { cmd: 'preset', presetId: 3, action: 'goto' },
    'PRESET_4': { cmd: 'preset', presetId: 4, action: 'goto' }
  };

  const config = ptzApiMap[direction];
  if (!config) {
    console.log("不支持的指令:", direction);
    return;
  }

  // 构建请求 URL
  const baseUrl = `http://${device.ip}:80`;
  const auth = btoa(`${device.username || 'admin'}:${device.password || ''}`);

  try {
    let url = '';
    let requestBody = '';

    if (config.cmd === 'continuous') {
      url = `${baseUrl}/ISAPI/PTZCtrl/channels/1/continuous`;
      requestBody = `<?xml version="1.0" encoding="UTF-8"?>
<PTZData>
  <pan>${config.pan}</pan>
  <tilt>${config.tilt}</tilt>
  <zoom>${config.zoom}</zoom>
</PTZData>`;

      const response = await fetch(url, {
        method: 'PUT',
        headers: {
          'Authorization': `Basic ${auth}`,
          'Content-Type': 'application/xml'
        },
        body: requestBody
      });

      if (response.ok) {
        showToast(`✓ ${direction} 控制成功`);
        console.log(`✅ ${direction} 控制成功`);
      } else if (response.status === 401) {
        showToast("摄像头认证失败，请检查用户名和密码");
      } else {
        showToast(`云台控制失败: ${response.status}`);
      }
    } else if (config.cmd === 'preset') {
      url = `${baseUrl}/ISAPI/PTZCtrl/channels/1/presets/${config.presetId}/goto`;

      const response = await fetch(url, {
        method: 'PUT',
        headers: {
          'Authorization': `Basic ${auth}`,
          'Content-Type': 'application/xml'
        }
      });

      if (response.ok) {
        showToast(`✓ 预置位 ${config.presetId} 调用成功`);
      } else if (response.status === 401) {
        showToast("摄像头认证失败，请检查用户名和密码");
      } else {
        showToast(`预置位调用失败`);
      }
    }
  } catch (err) {
    console.error("云台控制请求失败:", err);
    showToast(`云台控制失败: ${err.message}`);
  }
};

// 显示提示
const showToast = (msg) => {
  ptzToast.value = msg;
  setTimeout(() => {
    ptzToast.value = '';
  }, 2000);
};

const toggleSound = () => {
  soundEnabled.value = !soundEnabled.value;
  if (videoRef.value) videoRef.value.muted = !soundEnabled.value;
};

const refreshVideo = () => initMonitor();

const toggleFullscreen = () => {
  const elem = document.querySelector('.player-panel');
  if (elem?.requestFullscreen) elem.requestFullscreen();
};

const logout = () => {
  if (player) player.destroy();
  localStorage.removeItem('isLogin');
  router.push('/login');
};

onMounted(() => {
  loadDevices();
  initMonitor();
});

onUnmounted(() => {
  if (player) player.destroy();
});
</script>

<style scoped>
.monitor-layout {
  height: 100vh;
  background: linear-gradient(135deg, #e8f0fe 0%, #d4e4fc 100%);
  color: #1a2c3e;
  display: flex;
  flex-direction: column;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  overflow: hidden;
}

.header-bar {
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  border-bottom: 2px solid rgba(66, 185, 131, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.brand { display: flex; align-items: center; gap: 12px; }
.logo-icon { background: linear-gradient(135deg, #42b983 0%, #2d8f64 100%); border-radius: 10px; padding: 6px; display: flex; align-items: center; justify-content: center; }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 18px; font-weight: bold; color: #1a2c3e; letter-spacing: 1px; }
.brand-version { font-size: 10px; color: #42b983; font-weight: 500; }
.user-panel { display: flex; align-items: center; gap: 24px; }
.status-indicator { display: flex; align-items: center; gap: 8px; padding: 6px 12px; background: rgba(66, 185, 131, 0.1); border-radius: 20px; font-size: 13px; color: #2d8f64; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.online { background: #42b983; box-shadow: 0 0 8px #42b983; animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.user-info { display: flex; align-items: center; gap: 8px; padding: 6px 12px; background: #f0f4f9; border-radius: 20px; font-size: 13px; }
.logout-btn { display: flex; align-items: center; gap: 8px; background: none; border: 1px solid #ef4444; color: #ef4444; padding: 6px 16px; border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.3s; }
.logout-btn:hover { background: #ef4444; color: white; }

.main-content { flex: 1; display: flex; gap: 20px; padding: 20px; overflow: hidden; }
.device-panel { width: 340px; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); border-radius: 16px; border: 1px solid rgba(66, 185, 131, 0.2); display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08); }
.panel-header { padding: 20px; border-bottom: 1px solid rgba(0, 0, 0, 0.08); display: flex; justify-content: space-between; align-items: center; }
.header-title { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #1a2c3e; }
.device-count { display: flex; align-items: baseline; gap: 4px; font-size: 12px; color: #6c7a8e; }
.count-number { font-size: 20px; font-weight: bold; color: #42b983; }
.add-device-btn-small { background: #42b983; border: none; width: 24px; height: 24px; border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; margin-left: 8px; color: white; }
.device-list { flex: 1; padding: 12px; overflow-y: auto; }
.device-item { display: flex; align-items: center; gap: 12px; padding: 14px; margin-bottom: 8px; background: #f8fafd; border-radius: 12px; cursor: pointer; transition: all 0.3s; border: 1px solid transparent; }
.device-item:hover { background: #e8f4ef; transform: translateX(4px); }
.device-item.active { background: linear-gradient(135deg, rgba(66, 185, 131, 0.1), rgba(66, 185, 131, 0.05)); border-color: #42b983; }
.device-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.3s; }
.device-item:hover .device-actions { opacity: 1; }
.device-action-btn { background: rgba(255, 255, 255, 0.9); border: none; width: 28px; height: 28px; border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #6c7a8e; }
.device-action-btn:hover { background: #42b983; color: white; }
.device-action-btn.delete:hover { background: #ef4444; }
.device-name { font-weight: 600; color: #1a2c3e; font-size: 14px; }
.device-ip { font-size: 11px; color: #6c7a8e; }
.panel-footer { padding: 16px; border-top: 1px solid rgba(0, 0, 0, 0.08); }
.add-device-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px; background: linear-gradient(135deg, #42b983, #2d8f64); border: none; border-radius: 10px; color: white; font-weight: 500; cursor: pointer; }

.player-panel { flex: 1; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); border-radius: 16px; border: 1px solid rgba(66, 185, 131, 0.2); display: flex; flex-direction: column; overflow: hidden; }
.video-header { padding: 16px 20px; border-bottom: 1px solid rgba(0, 0, 0, 0.08); display: flex; align-items: center; gap: 16px; }
.live-badge { display: flex; align-items: center; gap: 6px; padding: 4px 10px; background: rgba(239, 68, 68, 0.1); border-radius: 20px; color: #ef4444; font-size: 12px; font-weight: bold; }
.live-dot { width: 8px; height: 8px; background: #ef4444; border-radius: 50%; animation: blink 1s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.video-info { flex: 1; display: flex; gap: 12px; }
.camera-name { font-weight: 600; color: #1a2c3e; }
.video-actions { display: flex; gap: 8px; }
.action-btn { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: #f0f4f9; border: none; border-radius: 8px; cursor: pointer; }
.video-wrapper { flex: 1; background: #000; position: relative; }
.video-style { width: 100%; height: 100%; object-fit: contain; }
.video-loader { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.9); display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 10; color: white; }
.spinner { width: 40px; height: 40px; border: 3px solid rgba(66, 185, 131, 0.2); border-top-color: #42b983; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-tip { font-size: 12px; color: #6c7a8e; margin-top: 8px; }
.video-footer { padding: 12px 20px; border-top: 1px solid rgba(0, 0, 0, 0.08); background: #f8fafd; }
.fps-info { display: flex; gap: 16px; font-size: 12px; color: #6c7a8e; }

.control-panel { width: 320px; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); border-radius: 16px; border: 1px solid rgba(66, 185, 131, 0.2); display: flex; flex-direction: column; overflow: hidden; }
.ptz-controls { flex: 1; padding: 24px; overflow-y: auto; }
.d-pad { display: flex; flex-direction: column; align-items: center; margin-bottom: 32px; }
.ptz-btn { width: 60px; height: 60px; background: #f0f4f9; border: 2px solid rgba(66, 185, 131, 0.3); border-radius: 16px; cursor: pointer; color: #42b983; display: flex; align-items: center; justify-content: center; font-size: 24px; transition: all 0.2s; }
.ptz-btn:hover { background: #42b983; color: white; }
.mid-row { display: flex; gap: 20px; align-items: center; }
.center-btn { width: 50px; height: 50px; background: rgba(66, 185, 131, 0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #42b983; }
.control-group { margin-bottom: 24px; text-align: left; }
.control-group label { display: block; font-size: 12px; font-weight: 600; color: #6c7a8e; margin-bottom: 12px; }
.control-buttons { display: flex; gap: 12px; }
.ctrl-btn { flex: 1; padding: 10px; background: #f0f4f9; border: 1px solid rgba(66, 185, 131, 0.3); border-radius: 10px; cursor: pointer; font-size: 12px; transition: all 0.2s; }
.ctrl-btn:hover { background: #42b983; color: white; }
.preset-buttons { display: flex; gap: 12px; }
.preset-btn { flex: 1; padding: 10px; background: #f0f4f9; border: 1px solid rgba(66, 185, 131, 0.3); border-radius: 10px; cursor: pointer; color: #42b983; transition: all 0.2s; }
.preset-btn:hover { background: #42b983; color: white; }

/* 弹窗样式 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: white; border-radius: 20px; width: 450px; max-width: 90%; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #e2e8f0; background: #f8fafd; }
.modal-header h3 { margin: 0; color: #1a2c3e; }
.modal-close { background: none; border: none; font-size: 28px; cursor: pointer; color: #6c7a8e; }
.modal-close:hover { color: #ef4444; }
.modal-body { padding: 24px; }
.modal-footer { padding: 16px 24px; border-top: 1px solid #e2e8f0; display: flex; justify-content: flex-end; gap: 12px; background: #f8fafd; }
.detail-item { display: flex; margin-bottom: 16px; font-size: 14px; }
.detail-item label { width: 90px; font-weight: 600; color: #4a5a6e; }
.detail-item span { color: #1a2c3e; }
.rtsp-url { font-family: monospace; font-size: 12px; background: #f0f4f9; padding: 4px 8px; border-radius: 6px; word-break: break-all; }
.status-badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; }
.status-badge.online { background: rgba(66, 185, 131, 0.1); color: #42b983; }
.status-badge.offline { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.input-group-modal { margin-bottom: 20px; }
.input-group-modal label { display: block; font-size: 13px; font-weight: 500; color: #4a5a6e; margin-bottom: 8px; }
.input-group-modal input, .input-group-modal select { width: 100%; padding: 10px 14px; border: 1.5px solid #e2e8f0; border-radius: 10px; font-size: 14px; outline: none; }
.input-group-modal input:focus, .input-group-modal select:focus { border-color: #42b983; }
.cancel-btn { padding: 8px 20px; background: #f0f4f9; border: 1px solid #e2e8f0; border-radius: 8px; cursor: pointer; }
.save-btn { padding: 8px 24px; background: linear-gradient(135deg, #42b983, #2d8f64); border: none; border-radius: 8px; cursor: pointer; color: white; font-weight: 500; }

/* 云台提示 */
.ptz-toast {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: #1a2c3e;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  animation: fadeOut 2s ease forwards;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

@keyframes fadeOut {
  0% { opacity: 1; transform: translateX(-50%) translateY(0); }
  70% { opacity: 1; }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: #f0f4f9; border-radius: 3px; }
::-webkit-scrollbar-thumb { background: #42b983; border-radius: 3px; }
</style>
