<template>
  <div class="user-center-container">
    <div class="user-center-card">
      <div class="card-header">
        <span class="header-icon">👤</span>
        <h3>个人中心</h3>
      </div>

      <!-- 头像区域 - 可点击上传 -->
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <div class="avatar" @click="triggerFileUpload">
            <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" />
            <span v-else class="avatar-emoji">{{ avatarEmoji }}</span>
            <div class="avatar-overlay">
              <span>📷</span>
            </div>
          </div>
          <input
              type="file"
              ref="fileInput"
              style="display: none"
              accept="image/jpeg,image/png,image/gif"
              @change="handleFileUpload"
          />
          <button class="change-avatar-btn" @click="triggerFileUpload">更换头像</button>
        </div>
      </div>

      <div class="info-section">
        <div class="info-item">
          <label>用户名</label>
          <div class="info-value">
            <span>{{ userInfo.username }}</span>
            <span class="role-badge" :class="userInfo.role === 'admin' ? 'admin' : 'user'">
              {{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}
            </span>
          </div>
        </div>

        <div class="info-item">
          <label>昵称</label>
          <div class="info-value editable">
            <input v-model="userInfo.nickname" placeholder="请输入昵称" />
            <button class="save-btn-small" @click="saveNickname">保存</button>
          </div>
        </div>

        <div class="info-item">
          <label>手机号</label>
          <div class="info-value editable">
            <input v-model="userInfo.phone" placeholder="请输入手机号" />
            <button class="save-btn-small" @click="savePhone">保存</button>
          </div>
        </div>

        <div class="info-item">
          <label>邮箱</label>
          <div class="info-value editable">
            <input v-model="userInfo.email" placeholder="请输入邮箱" />
            <button class="save-btn-small" @click="saveEmail">保存</button>
          </div>
        </div>

        <div class="info-item">
          <label>最后登录</label>
          <div class="info-value">{{ userInfo.lastLogin || '2026-04-14 10:30:00' }}</div>
        </div>

        <div class="info-item">
          <label>注册时间</label>
          <div class="info-value">{{ userInfo.createTime || '2026-04-01 00:00:00' }}</div>
        </div>
      </div>

      <div class="password-section">
        <div class="section-title">修改密码</div>
        <div class="password-form">
          <div class="password-item">
            <input type="password" v-model="passwordForm.oldPassword" placeholder="当前密码" />
          </div>
          <div class="password-item">
            <input type="password" v-model="passwordForm.newPassword" placeholder="新密码" />
          </div>
          <div class="password-item">
            <input type="password" v-model="passwordForm.confirmPassword" placeholder="确认新密码" />
          </div>
          <button class="change-pwd-btn" @click="changePassword">修改密码</button>
        </div>
      </div>

      <div class="card-footer">
        <button class="back-btn" @click="goBack">返回</button>
      </div>
    </div>

    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const toastMsg = ref('')
const fileInput = ref(null)

// 头像相关
const avatarUrl = ref('')
const avatarEmoji = ref('👨‍💻')

const userInfo = ref({
  username: 'admin',
  nickname: '管理员',
  phone: '13800138000',
  email: 'admin@healingstream.com',
  role: 'admin',
  lastLogin: '2026-04-14 10:30:00',
  createTime: '2026-04-01 00:00:00'
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载保存的头像
const loadAvatar = () => {
  const savedAvatar = localStorage.getItem('userAvatar')
  if (savedAvatar) {
    avatarUrl.value = savedAvatar
    avatarEmoji.value = ''
  } else {
    // 根据用户名生成默认头像emoji
    const emojiMap = {
      'admin': '👨‍💻',
      'user1': '👩‍💻',
      'operator': '👨‍🔧'
    }
    avatarEmoji.value = emojiMap[userInfo.value.username] || '👤'
  }
}

// 触发文件选择
const triggerFileUpload = () => {
  fileInput.value.click()
}

// 处理文件上传
const handleFileUpload = (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件')
    return
  }

  // 检查文件大小（限制2MB）
  if (file.size > 2 * 1024 * 1024) {
    showToast('图片大小不能超过2MB')
    return
  }

  // 预览图片
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarUrl.value = e.target.result
    avatarEmoji.value = ''
    // 保存到本地存储（临时方案，后续对接后端）
    localStorage.setItem('userAvatar', e.target.result)
    showToast('头像更新成功')
  }
  reader.readAsDataURL(file)

  // 清空input，允许重新上传同一个文件
  event.target.value = ''
}

const showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 2000)
}

const saveNickname = () => {
  localStorage.setItem('userNickname', userInfo.value.nickname)
  showToast('昵称修改成功')
}

const savePhone = () => {
  localStorage.setItem('userPhone', userInfo.value.phone)
  showToast('手机号修改成功')
}

const saveEmail = () => {
  localStorage.setItem('userEmail', userInfo.value.email)
  showToast('邮箱修改成功')
}

const changePassword = () => {
  if (!passwordForm.value.oldPassword) {
    showToast('请输入当前密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    showToast('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast('两次输入的新密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    showToast('新密码长度不能少于6位')
    return
  }
  showToast('密码修改成功，请重新登录')
  setTimeout(() => {
    localStorage.removeItem('isLogin')
    router.push('/login')
  }, 2000)
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  const savedName = localStorage.getItem('userName')
  if (savedName) {
    userInfo.value.username = savedName
  }

  // 加载保存的昵称、手机、邮箱
  const savedNickname = localStorage.getItem('userNickname')
  if (savedNickname) userInfo.value.nickname = savedNickname

  const savedPhone = localStorage.getItem('userPhone')
  if (savedPhone) userInfo.value.phone = savedPhone

  const savedEmail = localStorage.getItem('userEmail')
  if (savedEmail) userInfo.value.email = savedEmail

  loadAvatar()
})
</script>

<style scoped>
.user-center-container {
  min-height: 100%;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: transparent;
}

.user-center-card {
  max-width: 600px;
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 24px;
}

.card-header h3 {
  margin: 0;
  color: #1a2c3e;
}

.avatar-section {
  display: flex;
  justify-content: center;
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.avatar-wrapper {
  text-align: center;
}

.avatar {
  position: relative;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
}

.avatar:hover .avatar-overlay {
  opacity: 1;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-emoji {
  font-size: 48px;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-size: 24px;
}

.change-avatar-btn {
  background: none;
  border: 1px solid #42b983;
  color: #42b983;
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.change-avatar-btn:hover {
  background: #42b983;
  color: white;
}

.info-section {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.info-item {
  display: flex;
  margin-bottom: 20px;
}

.info-item label {
  width: 80px;
  font-weight: 600;
  color: #4a5a6e;
  padding-top: 8px;
}

.info-value {
  flex: 1;
  padding: 8px 0;
  color: #1a2c3e;
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-value.editable {
  padding: 0;
}

.info-value input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}

.role-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
}

.role-badge.admin {
  background: rgba(66, 185, 131, 0.1);
  color: #42b983;
}

.role-badge.user {
  background: rgba(100, 116, 139, 0.1);
  color: #64748b;
}

.save-btn-small {
  background: #42b983;
  border: none;
  color: white;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.save-btn-small:hover {
  background: #2d8f64;
}

.password-section {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.section-title {
  font-weight: 600;
  color: #1a2c3e;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #42b983;
  display: inline-block;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.password-item input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}

.change-pwd-btn {
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  color: white;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 8px;
  font-size: 14px;
  transition: all 0.2s;
}

.change-pwd-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(66, 185, 131, 0.3);
}

.card-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
}

.back-btn {
  background: none;
  border: 1px solid #e2e8f0;
  padding: 8px 24px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #f0f4f9;
}

.toast {
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
}

@keyframes fadeOut {
  0% { opacity: 1; transform: translateX(-50%) translateY(0); }
  70% { opacity: 1; }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}
</style>
