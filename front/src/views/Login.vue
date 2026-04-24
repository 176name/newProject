<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-gradient"></div>
    <div class="bg-blob-1"></div>
    <div class="bg-blob-2"></div>

    <div class="login-wrapper">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="#42b983" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="brand-text">
          <h1>HEALING STREAM</h1>
          <p>流媒体智能管理平台</p>
        </div>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>超低延迟实时传输</span>
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>AI智能云台控制</span>
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>多设备统一管理</span>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-section">
        <div class="login-card">
          <div class="login-header">
            <h2>欢迎回来</h2>
            <p>请登录您的账户以继续</p>
          </div>

          <div class="login-form">
            <div class="input-group">
              <label>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M20 21V19C20 16.8 18.2 15 16 15H8C5.8 15 4 16.8 4 19V21" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/>
                </svg>
                账号
              </label>
              <input v-model="form.username" type="text" placeholder="请输入管理员账号" @keyup.enter="handleLogin" />
            </div>

            <div class="input-group">
              <label>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <rect x="5" y="11" width="14" height="11" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M7 11V8C7 5.2 9.2 3 12 3C14.8 3 17 5.2 17 8V11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                密码
              </label>
              <input v-model="form.password" type="password" placeholder="请输入安全密码" @keyup.enter="handleLogin" />
            </div>

            <div class="form-options">
              <label class="checkbox">
                <input type="checkbox" v-model="rememberMe">
                <span>记住密码</span>
              </label>
              <a href="#" class="forgot-link" @click.prevent>忘记密码？</a>
            </div>

            <button class="login-btn" @click="handleLogin">
              <span>登 录</span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M5 12H19M19 12L13 6M19 12L13 18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <div class="demo-account">
              <div class="demo-title">演示账号</div>
              <div class="demo-info">
                <div class="demo-item">
                  <span>账号：</span>
                  <code>admin</code>
                </div>
                <div class="demo-item">
                  <span>密码：</span>
                  <code>123456</code>
                </div>
              </div>
            </div>
          </div>

          <div class="login-footer">
            <p>© 2026 HealingStream Media Group. All rights reserved.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = reactive({
  username: '',
  password: ''
})
const rememberMe = ref(false)

const checkRemembered = () => {
  const savedUsername = localStorage.getItem('savedUsername')
  const savedPassword = localStorage.getItem('savedPassword')
  if (savedUsername && savedPassword) {
    form.username = savedUsername
    form.password = savedPassword
    rememberMe.value = true
  }
}

const handleLogin = () => {
  if (form.username === 'admin' && form.password === '123456') {
    localStorage.setItem('isLogin', 'true')
    localStorage.setItem('userName', form.username)

    if (rememberMe.value) {
      localStorage.setItem('savedUsername', form.username)
      localStorage.setItem('savedPassword', form.password)
    } else {
      localStorage.removeItem('savedUsername')
      localStorage.removeItem('savedPassword')
    }

    router.push('/monitor')
  } else {
    alert('❌ 账号或密码错误\n\n演示账号：admin\n演示密码：123456')
  }
}

onMounted(() => {
  checkRemembered()
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f0fe 0%, #d4e4fc 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 80%, rgba(66, 185, 131, 0.08) 0%, transparent 50%),
  radial-gradient(circle at 80% 20%, rgba(66, 185, 131, 0.05) 0%, transparent 50%);
}

.bg-blob-1 {
  position: absolute;
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(66, 185, 131, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  animation: float 8s ease-in-out infinite;
}

.bg-blob-2 {
  position: absolute;
  bottom: -100px;
  left: -100px;
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(66, 185, 131, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  animation: float 10s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}

.login-wrapper {
  display: flex;
  max-width: 1100px;
  width: 90%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 32px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  z-index: 10;
}

.brand-section {
  flex: 1;
  background: linear-gradient(135deg, rgba(66, 185, 131, 0.95), rgba(45, 143, 100, 0.95));
  padding: 48px 40px;
  color: white;
  display: flex;
  flex-direction: column;
}

.brand-icon {
  margin-bottom: 24px;
}

.brand-text h1 {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.brand-text p {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 48px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}

.feature-dot {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
}

.login-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1a2c3e;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #6c7a8e;
}

.input-group {
  margin-bottom: 20px;
}

.input-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #4a5a6e;
  margin-bottom: 8px;
}

.input-group label svg {
  color: #42b983;
}

.input-group input {
  width: 100%;
  padding: 12px 16px;
  background: #f8fafd;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  color: #1a2c3e;
  transition: all 0.3s;
  outline: none;
}

.input-group input:focus {
  border-color: #42b983;
  background: white;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #6c7a8e;
}

.checkbox input {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #42b983;
}

.forgot-link {
  font-size: 13px;
  color: #42b983;
  text-decoration: none;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(66, 185, 131, 0.4);
}

.demo-account {
  margin-top: 24px;
  padding: 16px;
  background: #f8fafd;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.demo-title {
  font-size: 12px;
  font-weight: 600;
  color: #6c7a8e;
  text-transform: uppercase;
  text-align: center;
  margin-bottom: 12px;
}

.demo-info {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.demo-item code {
  background: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 12px;
  color: #42b983;
  font-weight: 600;
}

.login-footer {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
  margin-top: 24px;
}

.login-footer p {
  font-size: 11px;
  color: #a0aec0;
}

@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    width: 95%;
  }
}
</style>
