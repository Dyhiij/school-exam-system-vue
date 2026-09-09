<template>
  <div>
    <el-button type="primary" style="margin-bottom: 20px;" @click="openAddDialog">新增用户</el-button>
    <el-table :data="users" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号(工号)" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="department" label="部门" />
      <el-table-column prop="role" label="角色">
        <template #default="scope">
          <el-tag v-if="scope.row.role === 'admin'" type="danger">系统管理员</el-tag>
          <el-tag v-else-if="scope.row.role === 'teacher'" type="warning">考试管理员</el-tag>
          <el-tag v-else-if="scope.row.role === 'marker'" type="info">阅卷人</el-tag>
          <el-tag v-else type="success">考生</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)" :disabled="scope.row.username === 'admin'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.department" placeholder="选择部门" style="width: 100%">
            <el-option label="总办" value="总办" />
            <el-option label="技术部" value="技术部" />
            <el-option label="市场部" value="市场部" />
            <el-option label="人事部" value="人事部" />
            <el-option label="财务部" value="财务部" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="分配角色" style="width: 100%" :disabled="form.username === 'admin'">
            <el-option label="系统管理员" value="admin" />
            <el-option label="考试管理员" value="teacher" />
            <el-option label="阅卷人" value="marker" />
            <el-option label="考生" value="student" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: undefined, username: '', password: '', realName: '', department: '', role: 'student' })

const fetchUsers = async () => {
  const res = await request.get('/admin/users')
  if (res.data.code === 200) {
    users.value = res.data.data
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = { id: undefined, username: '', password: '', realName: '', department: '', role: 'student' }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

const handleSave = async () => {
  const res = await request.post('/admin/user', form.value)
  if (res.data.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchUsers()
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除?', '提示', { type: 'warning' })
  const res = await request.delete(`/admin/user/${id}`)
  if (res.data.code === 200) {
    ElMessage.success('删除成功')
    fetchUsers()
  }
}

onMounted(() => {
  fetchUsers()
})
</script>
