import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/exam/list',
    name: 'ExamList',
    component: () => import('../views/ExamList.vue')
  },
  {
    path: '/exam/room/:id',
    name: 'ExamRoom',
    component: () => import('../views/ExamRoom.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
