<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 style="text-align: center;">通用在线考试系统</h2>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号 (admin)" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码 (123456)" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%;" @click="handleLogin" :loading="loading">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAppStore } from '../store'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const store = useAppStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/user/login', form)
    if (res.data.code === 200) {
      // 缺陷修复: 登录成功即持久化登录态(localStorage), 刷新页面不掉线
      // 同时归一化字段: 后端返回 realName, 页面模板多处用 name
      const u = res.data.data
      if (u && u.name === undefined) {
        u.name = u.realName || u.username || '用户'
      }
      store.setCurrentUser(u)
      ElMessage.success('登录成功')
    } else {
      ElMessage.error(res.data.msg || '账号或密码错误')
    }
  } catch (error) {
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.login-box {
  width: 400px;
}
</style>
