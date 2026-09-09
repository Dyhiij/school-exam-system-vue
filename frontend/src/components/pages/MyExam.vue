<template>
  <div style="padding: 20px; text-align: center;">
    <h2>在线考试系统入口</h2>
    <p style="margin-bottom: 20px; color: #666;">请点击下方按钮进入正式考试环境（已连接真实数据库服务器）。</p>
    <el-button type="primary" size="large" @click="goToRealExam">进入我的待考列表</el-button>
  </div>
</template>

<script setup lang="ts">
const goToRealExam = () => {
  window.location.href = '/exam/list'
}
</script>
