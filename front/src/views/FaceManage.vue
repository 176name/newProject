<template>
  <div class="face-manage-container">
    <div class="header">
      <h2>人脸管理</h2>
      <button class="back-btn" @click="$router.back()">返回</button>
    </div>

    <!-- 注册新人脸 -->
    <div class="register-section">
      <h3>注册新人脸</h3>
      <div class="form">
        <input v-model="newName" placeholder="姓名" class="input" />
        <input type="file" ref="fileInput" accept="image/*" @change="onFileChange" class="file-input" />
        <button @click="registerFace" class="register-btn">注册</button>
      </div>
      <div v-if="registerMsg" class="msg">{{ registerMsg }}</div>
    </div>

    <!-- 已注册人脸列表 -->
    <div class="list-section">
      <h3>已注册人脸</h3>
      <div class="face-list">
        <div v-for="face in faceList" :key="face.id" class="face-item">
          <div class="face-avatar">👤</div>
          <div class="face-name">{{ face.name }}</div>
          <div class="face-id">ID: {{ face.id }}</div>
          <button @click="deleteFace(face.id)" class="delete-btn">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const PYTHON_AI_URL = 'http://localhost:5001'

const newName = ref('')
const fileInput = ref(null)
const selectedFile = ref(null)
const registerMsg = ref('')
const faceList = ref([])

const onFileChange = (e) => {
  selectedFile.value = e.target.files[0]
}

const registerFace = async () => {
  if (!newName.value || !selectedFile.value) {
    registerMsg.value = '请填写姓名并选择照片'
    return
  }
  const formData = new FormData()
  formData.append('name', newName.value)
  formData.append('file', selectedFile.value)

  try {
    const res = await axios.post(`${PYTHON_AI_URL}/face/register`, formData)
    if (res.data.code === 200) {
      registerMsg.value = '注册成功！'
      newName.value = ''
      selectedFile.value = null
      fileInput.value.value = ''
      loadFaces()
    } else {
      registerMsg.value = res.data.msg || '注册失败'
    }
  } catch (err) {
    registerMsg.value = '注册失败：' + err.message
  }
}

const loadFaces = async () => {
  try {
    const res = await axios.get(`${PYTHON_AI_URL}/face/list`)
    if (res.data.code === 200) {
      faceList.value = res.data.data
    }
  } catch (err) {
    console.error('加载人脸列表失败', err)
  }
}

const deleteFace = async (faceId) => {
  try {
    const res = await axios.delete(`${PYTHON_AI_URL}/face/${faceId}`)
    if (res.data.code === 200) {
      loadFaces()
    }
  } catch (err) {
    console.error('删除失败', err)
  }
}

onMounted(() => {
  loadFaces()
})
</script>

<style scoped>
.face-manage-container { padding: 20px; max-width: 800px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; }
.back-btn { background: #6c7a8e; color: white; border: none; padding: 6px 12px; border-radius: 8px; cursor: pointer; }
.register-section, .list-section { background: white; border-radius: 16px; padding: 20px; margin-top: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.form { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.input { padding: 8px 12px; border: 1px solid #ddd; border-radius: 8px; flex: 1; }
.file-input { flex: 1; }
.register-btn { background: #42b983; color: white; border: none; padding: 8px 16px; border-radius: 8px; cursor: pointer; }
.msg { margin-top: 10px; color: #42b983; }
.face-list { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 16px; }
.face-item { background: #f8fafd; border-radius: 12px; padding: 12px; width: 120px; text-align: center; }
.face-avatar { font-size: 48px; }
.face-name { font-weight: bold; margin-top: 8px; }
.face-id { font-size: 10px; color: #8a99aa; }
.delete-btn { background: #ef4444; color: white; border: none; padding: 4px 8px; border-radius: 6px; margin-top: 8px; cursor: pointer; font-size: 12px; }
</style>
