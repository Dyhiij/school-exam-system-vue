import { defineStore } from 'pinia'

// 登录态持久化: 刷新页面后仍保持登录 (缺陷修复: 此前仅存内存, 刷新即丢)
function loadStoredUser(): any {
  try {
    const raw = localStorage.getItem('user_info')
    if (raw) return JSON.parse(raw)
  } catch (e) { /* ignore */ }
  return null
}

export const useAppStore = defineStore('app', {
  state: () => ({
    currentUser: loadStoredUser() as any,
    users: [
      { id: 1, username: 'admin', password: '123', name: '管理员', role: 'admin', dept: '总办' },
      { id: 2, username: 'teacher1', password: '123', name: '张老师', role: 'teacher', dept: '教务处' },
      { id: 3, username: 'student1', password: '123', name: '李同学', role: 'student', dept: '计算机系' }
    ],
    subjects: ['Java基础', '市场营销'],
    questions: [
      { id: 1, subject: 'Java基础', type: '单选', content: 'Java基本数据类型有多少种？', options: { A: '6', B: '7', C: '8', D: '9' }, answer: 'C', score: 10, status: 1 },
      { id: 2, subject: 'Java基础', type: '判断', content: 'String是基本数据类型', answer: '错', score: 10, status: 1 },
      { id: 3, subject: 'Java基础', type: '简答', content: '简述面向对象的三个基本特征', answer: '封装、继承、多态', score: 20, status: 1 }
    ],
    papers: [
      { id: 1, name: '2025年Java基础考核卷', totalScore: 40, status: 1, questions: [1, 2, 3] }
    ],
    exams: [
      { id: 1, name: '第一次考核', startTime: '2025-01-01 00:00:00', endTime: '2025-12-31 23:59:59', duration: 60, passScore: 24, paperId: 1, students: [3], status: 1 } // status: 0未开始, 1进行中, 2已结束
    ],
    answers: [] as any[],
    scores: [
      { id: 1, userId: 3, examId: 1, score: 35, passScore: 24, status: '通过', submitTime: '2025-02-01 10:00:00' }
    ] as any[],
    logs: [
      { time: '2025-02-01 02:30:00', user: 'student1', type: '登录', result: '登录成功' }
    ] as any[],
    grades: [
      { id: 1, name: '计算机一班', teacher: '张老师', studentCount: 30 },
      { id: 2, name: '软件工程二班', teacher: '王老师', studentCount: 28 }
    ],
    config: {
      defaultPassScore: 60,
      defaultPassword: '123',
      minPasswordLength: 3
    }
  }),
  actions: {
    login(username: string, pass: string) {
      const u = this.users.find(u => u.username === username && u.password === pass)
      if (u) {
        this.currentUser = u
        this.addLog(u.name, '登录', '登录成功')
        return true
      }
      return false
    },
    // 真实后端登录成功后调用: 设置当前用户并持久化
    setCurrentUser(user: any) {
      this.currentUser = user
      try {
        localStorage.setItem('user_info', JSON.stringify(user))
      } catch (e) { /* ignore */ }
    },
    logout() {
      if (this.currentUser) {
        this.addLog(this.currentUser.name || this.currentUser.realName, '退出', '退出系统')
      }
      this.currentUser = null
      try {
        localStorage.removeItem('user_info')
      } catch (e) { /* ignore */ }
    },
    addLog(user: string, type: string, result: string) {
      this.logs.push({
        time: new Date().toLocaleString(),
        user,
        type,
        result
      })
    }
  }
})
