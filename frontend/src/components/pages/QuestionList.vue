<template>
  <div>
    <el-button type="primary" style="margin-bottom: 20px;" @click="openAddDialog">新增题目</el-button>
    <el-table :data="questions" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="subjectId" label="科目ID" width="80" />
      <el-table-column prop="type" label="题型">
        <template #default="scope">
          <el-tag v-if="scope.row.type === 1" type="success">单选</el-tag>
          <el-tag v-else-if="scope.row.type === 2" type="warning">多选</el-tag>
          <el-tag v-else-if="scope.row.type === 3" type="info">判断</el-tag>
          <el-tag v-else-if="scope.row.type === 4">填空</el-tag>
          <el-tag v-else type="danger">简答</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="scope">
          <span v-if="scope.row.difficulty === 1">简单</span>
          <span v-else-if="scope.row.difficulty === 2">中等</span>
          <span v-else>困难</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="题干" show-overflow-tooltip />
      <el-table-column prop="score" label="分值" width="80" />
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '新增题目'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="科目ID">
          <el-input-number v-model="form.subjectId" :min="1" />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="form.type">
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
            <el-option label="填空题" :value="4" />
            <el-option label="简答题" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.content" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="选项(JSON)" v-if="[1, 2, 3].includes(form.type)">
          <el-input v-model="form.options" type="textarea" placeholder='例如: {"A":"选项1","B":"选项2"}' />
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="form.answer" placeholder="多选题请用逗号分隔，例如 A,B,C" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" placeholder="填写答案解析..." />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="form.score" :min="1" />
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

const questions = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: undefined,
  subjectId: 1,
  type: 1,
  difficulty: 1,
  content: '',
  options: '{}',
  answer: '',
  score: 10,
  analysis: ''
})

const resetForm = () => {
  form.value = {
    id: undefined,
    subjectId: 1,
    type: 1,
    difficulty: 1,
    content: '',
    options: '{}',
    answer: '',
    score: 10,
    analysis: ''
  }
}

const openAddDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const fetchQuestions = async () => {
  const res = await request.get('/admin/questions')
  if (res.data.code === 200) {
    questions.value = res.data.data
  }
}

const handleSave = async () => {
  const res = await request.post('/admin/question', form.value)
  if (res.data.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchQuestions()
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除?', '提示', { type: 'warning' })
  const res = await request.delete(`/admin/question/${id}`)
  if (res.data.code === 200) {
    ElMessage.success('删除成功')
    fetchQuestions()
  }
}

onMounted(() => {
  fetchQuestions()
})
</script>
