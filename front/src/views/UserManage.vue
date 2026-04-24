<template>
  <div class="user-container">
    <div class="tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'current' }" @click="activeTab = 'current'">👥 当前用户</button>
      <button class="tab-btn" :class="{ active: activeTab === 'deleted' }" @click="activeTab = 'deleted'">🗑️ 已禁用用户</button>
    </div>

    <div v-show="activeTab === 'current'" class="user-content">
      <div class="toolbar">
        <button class="add-btn" @click="openAddModal">+ 添加用户</button>
        <div class="search-box">
          <input type="text" v-model="searchKeyword" placeholder="搜索用户名或角色" />
          <span class="search-icon">🔍</span>
        </div>
      </div>

      <div class="user-table">
        <table>
          <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>状态</th>
            <th>最后登录</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>
              <span class="user-name">{{ user.username }}</span>
              <button class="detail-btn" @click="viewDetail(user)">详情</button>
            </td>
            <td>
                <span class="role-badge" :class="user.role === 'admin' ? 'admin' : 'user'">
                  {{ user.role === 'admin' ? '管理员' : '普通用户' }}
                </span>
            </td>
            <td>
                <span class="status-badge" :class="user.status === 1 ? 'enabled' : 'disabled'">
                  {{ user.status === 1 ? '● 启用' : '○ 禁用' }}
                </span>
            </td>
            <td>{{ user.last_login || '--' }}</td>
            <td>{{ user.create_time || '--' }}</td>
            <td>
              <button class="edit-btn" @click="openEditModal(user)">编辑</button>
              <button v-if="user.id !== 1" class="delete-btn" @click="deleteUser(user.id)">禁用</button>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="7" class="empty-row">暂无用户数据</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-show="activeTab === 'deleted'" class="user-content">
      <div class="user-table">
        <table>
          <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>禁用时间</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="user in deletedUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.role === 'admin' ? '管理员' : '普通用户' }}</td>
            <td>{{ user.deleted_at || '--' }}</td>
            <td>
              <button class="restore-btn" @click="restoreUser(user.id)">恢复</button>
              <button class="permanent-btn" @click="permanentDelete(user.id)">彻底删除</button>
            </td>
          </tr>
          <tr v-if="deletedUsers.length === 0">
            <td colspan="5" class="empty-row">暂无禁用用户</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 用户详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>用户详细信息</h3>
          <button class="modal-close" @click="closeDetailModal">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-item"><label>用户名：</label><span>{{ currentUser?.username }}</span></div>
          <div class="detail-item"><label>角色：</label><span>{{ currentUser?.role === 'admin' ? '管理员' : '普通用户' }}</span></div>
          <div class="detail-item"><label>状态：</label><span :class="currentUser?.status === 1 ? 'online-text' : 'offline-text'">{{ currentUser?.status === 1 ? '启用' : '禁用' }}</span></div>
          <div class="detail-item"><label>最后登录：</label><span>{{ currentUser?.last_login || '--' }}</span></div>
          <div class="detail-item"><label>创建时间：</label><span>{{ currentUser?.create_time || '--' }}</span></div>
          <div class="detail-item"><label>备注：</label><span>{{ currentUser?.remark || '--' }}</span></div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑用户弹窗 -->
    <div v-if="showUserModal" class="modal-overlay" @click="closeUserModal">
      <div class="modal-content user-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑用户' : '添加用户' }}</h3>
          <button class="modal-close" @click="closeUserModal">×</button>
        </div>
        <div class="modal-body">
          <div class="input-item"><label>用户名 *</label><input v-model="form.username" placeholder="请输入用户名" /></div>
          <div class="input-item"><label>密码 *</label><input type="password" v-model="form.password" :placeholder="isEditing ? '留空则不修改' : '请输入密码'" /></div>
          <div class="input-item"><label>确认密码</label><input type="password" v-model="form.confirmPassword" :placeholder="isEditing ? '留空则不修改' : '请再次输入密码'" /></div>
          <div class="input-item"><label>角色</label><select v-model="form.role"><option value="user">普通用户</option><option value="admin">管理员</option></select></div>
          <div class="input-item"><label>状态</label><select v-model="form.status"><option :value="1">启用</option><option :value="0">禁用</option></select></div>
          <div class="input-item"><label>备注</label><textarea v-model="form.remark" rows="3" placeholder="备注信息"></textarea></div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeUserModal">取消</button>
          <button class="save-btn" @click="saveUser">{{ isEditing ? '保存修改' : '添加用户' }}</button>
        </div>
      </div>
    </div>

    <!-- 提示 -->
    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const activeTab = ref('current')
const users = ref([])
const deletedUsers = ref([])
const searchKeyword = ref('')
const showDetailModal = ref(false)
const showUserModal = ref(false)
const isEditing = ref(false)
const currentUser = ref(null)
const toastMsg = ref('')

const form = ref({
  id: null,
  username: '',
  password: '',
  confirmPassword: '',
  role: 'user',
  status: 1,
  remark: ''
})

const filteredUsers = computed(() => {
  if (!searchKeyword.value) return users.value
  const kw = searchKeyword.value.toLowerCase()
  return users.value.filter(u => u.username?.toLowerCase().includes(kw) || u.role?.includes(kw))
})

const showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 2000)
}

// 加载用户列表（模拟数据，后续对接后端）
const loadUsers = () => {
  // TODO: 对接后端API
  users.value = [
    { id: 1, username: 'admin', role: 'admin', status: 1, last_login: '2026-04-14 10:30:00', create_time: '2026-04-01 00:00:00', remark: '超级管理员' },
    { id: 2, username: 'user1', role: 'user', status: 1, last_login: '2026-04-13 15:20:00', create_time: '2026-04-05 10:00:00', remark: '普通用户' },
    { id: 3, username: 'operator', role: 'user', status: 0, last_login: '2026-04-10 09:00:00', create_time: '2026-04-08 14:30:00', remark: '已禁用' }
  ]
  deletedUsers.value = []
}

const viewDetail = (user) => {
  currentUser.value = user
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  currentUser.value = null
}

const openAddModal = () => {
  isEditing.value = false
  form.value = { id: null, username: '', password: '', confirmPassword: '', role: 'user', status: 1, remark: '' }
  showUserModal.value = true
}

const openEditModal = (user) => {
  isEditing.value = true
  form.value = { ...user, password: '', confirmPassword: '' }
  showUserModal.value = true
}

const closeUserModal = () => {
  showUserModal.value = false
}

const saveUser = () => {
  if (!form.value.username) {
    showToast('请填写用户名')
    return
  }
  if (!isEditing.value && !form.value.password) {
    showToast('请填写密码')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }

  // TODO: 对接后端API
  if (isEditing.value) {
    const index = users.value.findIndex(u => u.id === form.value.id)
    if (index !== -1) {
      users.value[index] = { ...users.value[index], ...form.value }
      delete users.value[index].password
      delete users.value[index].confirmPassword
    }
    showToast('用户修改成功')
  } else {
    const newId = Math.max(...users.value.map(u => u.id), 0) + 1
    const newUser = {
      id: newId,
      username: form.value.username,
      role: form.value.role,
      status: form.value.status,
      remark: form.value.remark,
      create_time: new Date().toLocaleString()
    }
    users.value.push(newUser)
    showToast('用户添加成功')
  }
  closeUserModal()
}

const deleteUser = (id) => {
  if (id === 1) {
    showToast('不能禁用超级管理员')
    return
  }
  if (confirm('确定要禁用该用户吗？')) {
    // TODO: 对接后端API
    const user = users.value.find(u => u.id === id)
    if (user) {
      user.status = 0
      showToast('用户已禁用')
    }
  }
}

const restoreUser = (id) => {
  showToast('恢复功能开发中...')
}

const permanentDelete = (id) => {
  if (confirm('彻底删除后无法恢复，确定吗？')) {
    showToast('彻底删除功能开发中...')
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.tabs {
  display: flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  padding: 6px;
}

.tab-btn {
  flex: 1;
  padding: 10px;
  background: none;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn.active {
  background: #42b983;
  color: white;
}

.user-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(66, 185, 131, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toolbar {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
}

.search-box {
  position: relative;
}

.search-box input {
  padding: 8px 32px 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  width: 200px;
}

.search-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.5;
}

.user-table {
  flex: 1;
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

th {
  background: #f8fafd;
  font-weight: 600;
  color: #1a2c3e;
}

.user-name {
  display: inline-block;
  margin-right: 8px;
}

.detail-btn {
  background: none;
  border: 1px solid #42b983;
  color: #42b983;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
}

.role-badge {
  display: inline-block;
  padding: 2px 8px;
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

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
}

.status-badge.enabled {
  background: rgba(66, 185, 131, 0.1);
  color: #42b983;
}

.status-badge.disabled {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.edit-btn, .delete-btn, .restore-btn, .permanent-btn {
  padding: 4px 10px;
  margin-right: 6px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
}

.edit-btn, .restore-btn {
  background: #e8f4ef;
  color: #42b983;
}

.delete-btn, .permanent-btn {
  background: #fee2e2;
  color: #ef4444;
}

.empty-row {
  text-align: center;
  padding: 40px;
  color: #6c7a8e;
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
  width: 450px;
  max-width: 90%;
  overflow: hidden;
}

.user-modal {
  width: 500px;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.detail-item {
  display: flex;
  margin-bottom: 12px;
}

.detail-item label {
  width: 100px;
  font-weight: 600;
  color: #4a5a6e;
}

.input-item {
  margin-bottom: 16px;
}

.input-item label {
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
  color: #4a5a6e;
}

.input-item input, .input-item select, .input-item textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-family: inherit;
}

.cancel-btn, .save-btn {
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
}

.cancel-btn {
  background: #f0f4f9;
  border: 1px solid #e2e8f0;
}

.save-btn {
  background: linear-gradient(135deg, #42b983, #2d8f64);
  border: none;
  color: white;
}

.online-text { color: #42b983; }
.offline-text { color: #ef4444; }

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
  z-index: 9999;
  animation: fadeOut 2s ease forwards;
}

@keyframes fadeOut {
  0% { opacity: 1; }
  70% { opacity: 1; }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}
</style>
