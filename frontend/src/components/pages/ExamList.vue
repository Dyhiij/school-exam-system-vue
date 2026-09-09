<template>
  <div>
    <el-button type="primary" style="margin-bottom: 20px;" @click="dialogVisible = true">发布考试</el-button>
    <el-table :data="batches" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="考试名称" />
      <el-table-column prop="paperId" label="试卷ID" width="80" />
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column prop="endTime" label="结束时间" width="180" />
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : (scope.row.status === 0 ? 'info' : 'warning')">
            {{ scope.row.status === 1 ? '进行中' : (scope.row.status === 0 ? '未开始' : '已结束') }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="发布考试">
      <el-form :model="form" label-width="100px">
        <el-form-item label="考试名称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="试卷ID">
          <el-input-number v-model="form.paperId" :min="1" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
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
import { ElMessage } from 'element-plus'

const batches = ref([])
const dialogVisible = ref(false)
const form = ref({
  title: '',
  paperId: 1,
  startTime: '',
  endTime: '',
  status: 1
})

const fetchBatches = async () => {
  const res = await request.get('/admin/batches')
  if (res.data.code === 200) {
    batches.value = res.data.data
  }
}

const handleSave = async () => {
  const res = await request.post('/admin/batch', form.value)
  if (res.data.code === 200) {
    ElMessage.success('发布成功')
    dialogVisible.value = false
    fetchBatches()
  }
}

onMounted(() => {
  fetchBatches()
})
</script>
