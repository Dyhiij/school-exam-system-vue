<template>
  <div class="my-score">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的成绩 (My Scores)</span>
          <el-button size="small" @click="fetchScores">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="myScores" border style="width: 100%">
        <el-table-column prop="paperTitle" label="考试名称" min-width="200" />
        <el-table-column prop="totalScore" label="客观题得分" width="110">
          <template #default="{ row }">
            <b>{{ row.totalScore }}</b> / {{ row.totalScoreLimit }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
        <el-table-column label="💡 学习指导" min-width="220">
          <template #default="{ row }">
            <span style="font-size: 13px; color: #e6a23c; line-height: 1.5;">
              {{ generateAIAdvice(row) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && myScores.length === 0" description="暂无成绩记录, 快去参加考试吧" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const myScores = ref<any[]>([])

// 状态: 1已交卷(客观已出, 简答待阅) 2已阅卷(总分已出) 3已发布
const statusText = (row: any) => {
  const st = Number(row.status)
  if (st >= 2) return (Number(row.totalScore) >= Number(row.passScore)) ? '通过' : '未通过'
  return '待人工阅卷'
}
const statusTagType = (st: any) => {
  const s = Number(st)
  if (s >= 2) return 'success'
  return 'warning'
}

const fetchScores = async () => {
  loading.value = true
  try {
    const userInfo = localStorage.getItem('user_info')
    const userId = userInfo ? JSON.parse(userInfo).id : null
    if (!userId) {
      ElMessage.warning('登录状态失效, 请重新登录')
      return
    }
    const res = await request.get(`/exam/my-scores?userId=${userId}`)
    if (res.data.code === 200) {
      myScores.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

// 规则引擎学习建议(基于得分/及格线比值)
const generateAIAdvice = (row: any) => {
  const score = Number(row.totalScore)
  const pass = Number(row.passScore)
  const st = Number(row.status)
  if (st < 2) return '【阅卷中】客观题已出分, 简答题待老师批阅, 请耐心等待。'
  const ratio = score / (pass || 60)
  if (ratio < 0.6) {
    return '【严重预警】基础概念缺失明显, 建议立即向老师求助, 重看课程回放。'
  } else if (ratio < 1.0) {
    return '【加油提醒】距及格线一步之遥, 重点复习错题, 加强针对性练习。'
  } else if (ratio <= 1.2) {
    return '【稳步前行】基础达标! 但掌握还不够牢固, 建议多做综合性拓展题。'
  } else {
    return '【表现优异】知识点掌握扎实! 建议预习下一章或尝试进阶项目。'
  }
}

onMounted(fetchScores)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
