<template>
  <div class="container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>企业级在线考试系统 (Portable Edition)</span>
        </div>
      </template>
      <div class="text item">
        <el-descriptions title="系统状态" :column="1" border>
          <el-descriptions-item label="后端服务">
            <el-tag :type="backendStatus ? 'success' : 'danger'">
              {{ backendStatus ? '运行中' : '连接失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本">{{ version }}</el-descriptions-item>
          <el-descriptions-item label="服务器时间">{{ serverTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div style="margin-top: 20px; text-align: center;">
            <el-button type="primary" size="large" @click="startExam">进入考试系统</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const backendStatus = ref(false)
const version = ref('Loading...')
const serverTime = ref('Loading...')

const checkStatus = async () => {
  try {
    const res = await request.get('/system/status')
    if (res.data.code === 200) {
      backendStatus.value = true
      version.value = res.data.version
      serverTime.value = new Date(res.data.time).toLocaleString()
    }
  } catch (error) {
    console.error(error)
    backendStatus.value = false
  }
}

const startExam = () => {
    router.push('/exam/list')
}

onMounted(() => {
  checkStatus()
})
</script>

<style scoped>
.container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.box-card {
  width: 480px;
}
.card-header {
  font-weight: bold;
  font-size: 18px;
}
</style>
