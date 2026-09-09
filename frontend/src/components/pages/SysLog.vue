<template>
  <div class="sys-log">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统日志 (System Logs)</span>
          <el-button type="warning" @click="runAIAudit">
            <el-icon><Monitor /></el-icon> AI 智能异常审计
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="time" label="操作时间" width="180" />
        <el-table-column prop="user" label="操作人" width="120" />
        <el-table-column prop="type" label="日志类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === '登录' ? 'success' : 'info'">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="结果" />
        
        <!-- AI 审计结果列 -->
        <el-table-column label="AI 审计状态" width="150" v-if="aiAudited">
          <template #default="{ row }">
            <el-tag v-if="row.aiRisk === 'High'" type="danger">高风险 (深夜/失败)</el-tag>
            <el-tag v-else-if="row.aiRisk === 'Medium'" type="warning">中风险 (异常行为)</el-tag>
            <el-tag v-else type="success">安全正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '../../store'
import { ElMessage } from 'element-plus'
import { Monitor } from '@element-plus/icons-vue'

const store = useAppStore()
const aiAudited = ref(false)

const tableData = computed(() => {
  return store.logs.map(log => ({
    ...log,
    // 如果还没审计，给个默认值
    aiRisk: log.aiRisk || 'Unknown'
  })).reverse()
})

// AI智能异常检测算法替代：基于规则和时间分布判定
const runAIAudit = () => {
  let riskCount = 0
  store.logs.forEach(log => {
    let risk = 'Low'
    
    // 1. 时间维度分析：深夜登录判定为高风险
    const hour = new Date(log.time).getHours()
    if (hour >= 0 && hour <= 5) {
      risk = 'High'
    }
    
    // 2. 行为结果分析：如果包含“失败”或“删除”等敏感词
    if (log.result.includes('失败') || log.type.includes('删除')) {
      risk = risk === 'High' ? 'High' : 'Medium'
    }
    
    log.aiRisk = risk
    if (risk !== 'Low') riskCount++
  })
  
  aiAudited.value = true
  if (riskCount > 0) {
    ElMessage.warning(`AI审计完成，共发现 ${riskCount} 条潜在风险记录，请注意防范。`)
  } else {
    ElMessage.success('AI审计完成，系统当前运行安全，无异常风险。')
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
