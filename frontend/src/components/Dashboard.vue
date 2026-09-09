<template>
  <el-container style="height: 100vh;">
    <el-header style="background-color: #409eff; color: white; display: flex; justify-content: space-between; align-items: center;">
      <h2>通用在线考试系统</h2>
      <div>
        <span>欢迎，{{ store.currentUser.name }} ({{ store.currentUser.role }})</span>
        <el-button link style="color: white; margin-left: 20px;" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background-color: white;">
        <el-menu :default-active="activeMenu" @select="handleSelect">
          <!-- 考生菜单 -->
          <template v-if="store.currentUser.role === 'student'">
            <el-menu-item index="my-exam">我的待考</el-menu-item>
            <el-menu-item index="my-score">成绩查询</el-menu-item>
            <el-menu-item index="profile">个人中心</el-menu-item>
          </template>
          <!-- 阅卷人菜单 -->
          <template v-if="store.currentUser.role === 'teacher'">
            <el-menu-item index="grade">人工阅卷</el-menu-item>
            <el-menu-item index="profile">个人中心</el-menu-item>
          </template>
          <!-- 管理员菜单 -->
          <template v-if="store.currentUser.role === 'admin'">
            <el-sub-menu index="user-mgt">
              <template #title>用户与权限管理</template>
              <el-menu-item index="user-list">考生管理</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="question-mgt">
              <template #title>题库管理</template>
              <el-menu-item index="subject-list">科目分类</el-menu-item>
              <el-menu-item index="question-list">题目录入与管理</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="paper-mgt">
              <template #title>试卷管理</template>
              <el-menu-item index="paper-list">试卷列表</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="exam-mgt">
              <template #title>考试管理</template>
              <el-menu-item index="exam-list">考试列表</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="grade-mgt">
              <template #title>阅卷评分</template>
              <el-menu-item index="grade">人工阅卷</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="score-mgt">
              <template #title>成绩管理</template>
              <el-menu-item index="score-list">成绩查询与审核</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="sys-mgt">
              <template #title>系统管理</template>
              <el-menu-item index="sys-log">操作日志</el-menu-item>
              <el-menu-item index="sys-config">基础配置</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="profile">个人中心</el-menu-item>
          </template>
        </el-menu>
      </el-aside>
      <el-main>
        <!-- 动态组件渲染 -->
        <component :is="currentComponent" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '../store'
import { ElMessage } from 'element-plus'

import Profile from './pages/Profile.vue'
import UserList from './pages/UserList.vue'
import SubjectList from './pages/SubjectList.vue'
import QuestionList from './pages/QuestionList.vue'
import PaperList from './pages/PaperList.vue'
import ExamList from './pages/ExamList.vue'
import MyExam from './pages/MyExam.vue'
import Grade from './pages/Grade.vue'
import ScoreList from './pages/ScoreList.vue'
import MyScore from './pages/MyScore.vue'
import SysLog from './pages/SysLog.vue'
import SysConfig from './pages/SysConfig.vue'

const store = useAppStore()

const activeMenu = ref('profile')
if(store.currentUser.role === 'student') activeMenu.value = 'my-exam'
if(store.currentUser.role === 'teacher') activeMenu.value = 'grade'
if(store.currentUser.role === 'admin') activeMenu.value = 'user-list'

const handleSelect = (index: string) => {
  activeMenu.value = index
}

const handleLogout = () => {
  store.logout()
  ElMessage.success('已退出登录')
}

const currentComponent = computed(() => {
  switch (activeMenu.value) {
    case 'profile': return Profile
    case 'user-list': return UserList
    case 'subject-list': return SubjectList
    case 'question-list': return QuestionList
    case 'paper-list': return PaperList
    case 'exam-list': return ExamList
    case 'my-exam': return MyExam
    case 'grade': return Grade
    case 'score-list': return ScoreList
    case 'my-score': return MyScore
    case 'sys-log': return SysLog
    case 'sys-config': return SysConfig
    default: return Profile
  }
})
</script>
