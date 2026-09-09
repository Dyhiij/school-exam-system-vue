<template>
  <div class="exam-list-container">
    <h2>可用考试列表</h2>
    <el-table :data="paperList" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="试卷ID" width="100" />
      <el-table-column prop="title" label="试卷名称" />
      <el-table-column prop="totalScore" label="总分" width="120" />
      <el-table-column prop="duration" label="时长(分钟)" width="120" />
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button type="primary" size="small" @click="enterExam(scope.row.id)">开始考试</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 20px;">
        <el-button @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const paperList = ref([])
const loading = ref(false)

const fetchPapers = async () => {
  loading.value = true
  try {
    const res = await request.get('/exam/list')
    if (res.data.code === 200) {
      paperList.value = res.data.data
    } else {
      ElMessage.error(res.data.msg || '获取试卷失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

const enterExam = (id: number) => {
  router.push(`/exam/room/${id}`)
}

onMounted(() => {
  fetchPapers()
})
</script>

<style scoped>
.exam-list-container {
  padding: 40px;
  max-width: 1000px;
  margin: 0 auto;
}
</style>
