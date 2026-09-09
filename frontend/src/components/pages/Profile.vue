<template>
  <div class="profile">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人中心 (User Profile)</span>
          <el-button type="primary" @click="generateAIProfile">
            <el-icon><User /></el-icon> AI 生成用户画像
          </el-button>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ user?.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ user?.name }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag>{{ user?.role }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ user?.dept }}</el-descriptions-item>
      </el-descriptions>

      <!-- AI 分析结果展示区 -->
      <div v-if="aiTags.length > 0" class="ai-box">
        <h4><el-icon><MagicStick /></el-icon> AI 智能画像标签：</h4>
        <div class="tags-container">
          <el-tag 
            v-for="(tag, index) in aiTags" 
            :key="index" 
            :type="tag.type"
            effect="dark"
            style="margin-right: 10px; margin-bottom: 10px;"
          >
            {{ tag.label }}
          </el-tag>
        </div>
        <p class="ai-desc">{{ aiDesc }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '../../store'
import { User, MagicStick } from '@element-plus/icons-vue'

const store = useAppStore()
const user = computed(() => store.currentUser)

const aiTags = ref<any[]>([])
const aiDesc = ref('')

// AI生成用户画像算法替代：基于用户角色、部门和基础规则的标签生成器
const generateAIProfile = () => {
  if (!user.value) return

  const tags = []
  let desc = ''

  if (user.value.role === 'admin') {
    tags.push({ label: '系统掌控者', type: 'danger' })
    tags.push({ label: '全知全能', type: 'warning' })
    tags.push({ label: '高频活跃', type: 'success' })
    desc = 'AI分析结果：作为系统管理员，您掌握着系统的核心命脉。建议定期查看系统日志和配置，确保系统安全稳定运行。'
  } else if (user.value.role === 'teacher') {
    tags.push({ label: '辛勤园丁', type: 'success' })
    tags.push({ label: '严谨治学', type: 'primary' })
    if (user.value.dept === '教务处') {
      tags.push({ label: '教务核心', type: 'warning' })
    }
    desc = 'AI分析结果：作为光荣的人民教师，您近期在教学管理上投入了大量精力。AI建议您多关注班级成绩的波动，使用成绩分析功能来优化教学方案。'
  } else {
    tags.push({ label: '潜力无限', type: 'info' })
    tags.push({ label: '求知若渴', type: 'primary' })
    if (user.value.dept === '计算机系') {
      tags.push({ label: '代码极客', type: 'success' })
    }
    desc = 'AI分析结果：作为一名朝气蓬勃的学生，您的学习状态良好。AI建议您多关注“我的成绩”中的学习建议，针对性地提升自己的薄弱环节。'
  }

  aiTags.value = tags
  aiDesc.value = desc
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ai-box {
  margin-top: 20px;
  padding: 15px;
  background-color: #ecf5ff;
  border-radius: 4px;
  border-left: 5px solid #409eff;
}
.tags-container {
  margin: 10px 0;
}
.ai-desc {
  margin: 10px 0 0 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}
</style>
