<template>
  <div class="sys-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统配置 (System Config)</span>
          <el-button type="primary" @click="runAIOptimization">
            <el-icon><MagicStick /></el-icon> AI 智能优化配置
          </el-button>
        </div>
      </template>
      <el-form label-width="150px" :model="store.config">
        <el-form-item label="默认及格分数">
          <el-input-number v-model="store.config.defaultPassScore" :min="0" :max="100" />
          <div class="form-tip">新考试的默认及格线</div>
        </el-form-item>
        <el-form-item label="新用户默认密码">
          <el-input v-model="store.config.defaultPassword" />
        </el-form-item>
        <el-form-item label="密码最小长度限制">
          <el-input-number v-model="store.config.minPasswordLength" :min="1" :max="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="saveConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- AI 分析结果展示区 -->
      <div v-if="aiResult" class="ai-box">
        <h4>AI 智能分析报告：</h4>
        <p v-for="(msg, index) in aiResult" :key="index">💡 {{ msg }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '../../store'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'

const store = useAppStore()
const aiResult = ref<string[]>([])

// AI功能替代算法：根据系统历史数据给出配置优化建议
const runAIOptimization = () => {
  const msgs = []
  let newPassScore = store.config.defaultPassScore
  let newMinLen = store.config.minPasswordLength

  // 1. 根据成绩计算通过率
  if (store.scores.length > 0) {
    const passCount = store.scores.filter(s => s.score >= s.passScore).length
    const passRate = passCount / store.scores.length
    if (passRate > 0.8) {
      newPassScore = Math.min(100, newPassScore + 5)
      msgs.push(`通过率较高 (${(passRate*100).toFixed(1)}%)，AI建议将及格线提高至 ${newPassScore} 分以增加挑战性。`)
    } else if (passRate < 0.4) {
      newPassScore = Math.max(0, newPassScore - 5)
      msgs.push(`通过率较低 (${(passRate*100).toFixed(1)}%)，AI建议将及格线降低至 ${newPassScore} 分以鼓励学生。`)
    } else {
      msgs.push(`当前通过率合理 (${(passRate*100).toFixed(1)}%)，及格线无需调整。`)
    }
  } else {
    msgs.push('暂无成绩数据，采用默认及格线推荐值 60 分。')
    newPassScore = 60
  }

  // 2. 根据日志分析安全风险
  const failedLogs = store.logs.filter(l => l.result && l.result.includes('失败'))
  if (failedLogs.length > 3) {
    newMinLen = Math.max(newMinLen, 8)
    msgs.push(`检测到近期有多次登录失败记录，存在暴力破解风险，AI建议将密码最小长度提升至 ${newMinLen} 位。`)
  } else {
    msgs.push('系统登录日志未发现异常，当前密码长度策略安全。')
  }

  // 应用AI建议
  store.config.defaultPassScore = newPassScore
  store.config.minPasswordLength = newMinLen
  aiResult.value = msgs
  ElMessage.success('AI已根据数据分析自动优化了系统配置！')
}

const saveConfig = () => {
  ElMessage.success('配置保存成功！')
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-tip {
  font-size: 12px;
  color: #888;
  margin-left: 10px;
}
.ai-box {
  margin-top: 20px;
  padding: 15px;
  background-color: #f0f9eb;
  border-radius: 4px;
  border-left: 5px solid #67c23a;
}
.ai-box p {
  margin: 5px 0;
  color: #333;
}
</style>
