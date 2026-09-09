<template>
  <el-config-provider>
    <router-view v-if="$route.path !== '/'" />
    <div v-else>
      <div v-if="!store.currentUser">
        <Login />
      </div>
      <div v-else>
        <Dashboard />
      </div>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { useAppStore } from './store'
import Login from './components/Login.vue'
import Dashboard from './components/Dashboard.vue'
import { useRoute } from 'vue-router'

const store = useAppStore()
const $route = useRoute()
</script>

<style>
body {
  margin: 0;
  padding: 0;
  background-color: #f5f7fa;
}
#app {
  width: 100vw;
  height: 100vh;
}
</style>
