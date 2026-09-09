<template>
  <div class="score-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学生成绩列表 (Score List)</span>
          <el-button type="primary" @click="runAIScoreAnalysis" :disabled="rows.length === 0">
            <el-icon><DataLine /></el-icon> 学情统计分析
          </el-button>
        </div>
      </template>

      <!-- 学情分析结果区 -->
      <div v-if="aiAnalysis" class="ai-box">
        <h4><el-icon><Histogram /></el-icon> 考试成绩智能分析：</h4>
        <p><strong>整体表现：</strong> {{ aiAnalysis.summary }}</p>
        <p><strong>数据指标：</strong> 平均分 {{ aiAnalysis.avg }} 分 | 最高分 {{ aiAnalysis.max }} 分 | 最低分 {{ aiAnalysis.min }} 分 | 及格率 {{ aiAnalysis.passRate }}%</p>
        <p><strong>教学建议：</strong> {{ aiAnalysis.advice }}</p>
      </div>

      <el-table v-loading="loading" :data="rows" border style="width: 100%; margin-top: 20px">
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="paperTitle" label="考试名称" min-width="200" />
        <el-table-column prop="totalScore" label="成绩" width="120">
          <template #default="{ row }">
            <span :style="{ color: pass(row) ? 'green' : 'red', fontWeight: 'bold' }">
              {{ row.totalScore }} / {{ row.totalScoreLimit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="pass(row) ? 'success' : 'warning'">{{ statusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无成绩记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { DataLine, Histogram } from '@element-plus/icons-vue'

const loading = ref(false)
const rows = ref<any[]>([])
const aiAnalysis = ref<any>(null)

const pass = (row: any) => Number(row.status) >= 2 && Number(row.totalScore) >= Number(row.passScore)
const statusText = (row: any) => {
  const st = Number(row.status)
  if (st >= 2) return pass(row) ? '通过' : '未通过'
  return '待人工阅卷'
}

const fetchScores = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/scores')
    if (res.data.code === 200) {
      rows.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

// 基于真实成绩的统计分析(平均值/极值/及格率), 替代原"AI"包装
const runAIScoreAnalysis = () => {
  const graded = rows.value.filter(r => Number(r.status) >= 2)
  if (graded.length === 0) {
    ElMessage.warning('暂无已阅卷成绩数据, 无法分析。')
    return
  }
  let total = 0, max = -1, min = Infinity, passCount = 0
  graded.forEach(r => {
    const s = Number(r.totalScore)
    total += s
    if (s > max) max = s
    if (s < min) min = s
    if (s >= Number(r.passScore)) passCount++
  })
  const avg = (total / graded.length).toFixed(1)
  const passRate = ((passCount / graded.length) * 100).toFixed(0)

  let summary = '', advice = ''
  const rate = passCount / graded.length
  if (rate >= 0.8) {
    summary = '整体掌握情况良好, 大部分考生已达标。'
    advice = '建议引入拔高题目, 激发潜力, 防止学习倦怠。'
  } else if (rate >= 0.5) {
    summary = '成绩呈两极分化, 部分考生基础薄弱。'
    advice = '建议开展结对帮扶, 对共性错题集中讲解。'
  } else {
    summary = '整体成绩不容乐观, 存在大面积不及格。'
    advice = '建议降低进度, 重新巩固基础知识点, 关注学习态度。'
  }
  aiAnalysis.value = { avg, max, min, passRate, summary, advice }
}

onMounted(fetchScores)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ai-box {
  margin-top: 10px;
  padding: 15px;
  background-color: #f4f4f5;
  border-radius: 4px;
  border-left: 5px solid #409eff;
}
.ai-box p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}
</style>
