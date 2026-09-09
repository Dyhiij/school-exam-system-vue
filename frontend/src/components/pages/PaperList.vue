<template>
  <div>
    <el-button type="primary" style="margin-bottom: 20px;" @click="openCreate">新增试卷</el-button>
    <el-table :data="papers" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="试卷名称" min-width="160" />
      <el-table-column prop="type" label="组卷方式" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.type === 1 ? '' : 'warning'">
            {{ scope.row.type === 1 ? '固定组卷' : '随机组卷' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="passScore" label="及格分" width="80" />
      <el-table-column prop="duration" label="时长(分钟)" width="110" />
      <el-table-column label="题目数" width="90">
        <template #default="scope">
          <el-tag size="small" type="info">{{ boundCount(scope.row.id) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openConfig(scope.row)">配置题目</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增试卷 -->
    <el-dialog v-model="dialogVisible" title="新增试卷" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="试卷名称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="组卷方式">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">固定组卷(人工挑题)</el-radio>
            <el-radio :label="2">随机组卷(策略抽题)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="form.totalScore" :min="1" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="form.passScore" :min="1" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="form.duration" :min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" inactive-text="草稿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 配置题目(固定组卷的题目来源) -->
    <el-dialog v-model="configVisible" :title="`配置题目: ${configPaper.title || ''}`" width="760px" top="5vh">
      <el-alert
        title="勾选此试卷包含的题目并保存; 考生作答与判分将只针对这些题(固定组卷)。随机组卷则从中随机抽题。"
        type="info" :closable="false" style="margin-bottom: 12px;" />
      <div style="margin-bottom: 10px;">
        <el-tag style="margin-right: 6px;">已选 {{ selectedIds.length }} 题</el-tag>
        <el-tag type="success" v-if="selectedTotal === Number(configPaper.totalScore)" style="margin-right: 6px;">总分恰好 {{ selectedTotal }}</el-tag>
        <el-tag type="warning" v-else-if="selectedIds.length > 0">当前合计 {{ selectedTotal }} 分(试卷总分 {{ configPaper.totalScore }})</el-tag>
      </div>
      <el-table
        ref="tableRef"
        :data="questions"
        height="420"
        border
        style="width: 100%"
        @selection-change="onSelectionChange"
        row-key="id"
        :row-class-name="rowClassName"
      >
        <el-table-column type="selection" width="45" :reserve-selection="true" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="类型" width="80">
          <template #default="scope">{{ typeText(scope.row.type) }}</template>
        </el-table-column>
        <el-table-column prop="content" label="题目" min-width="240" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="科目" width="90">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.subjectId }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span>
          <el-button @click="configVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingConfig" @click="saveConfig">保存配置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const papers = ref<any[]>([])
const dialogVisible = ref(false)
const form = ref({
  title: '', type: 1, totalScore: 100, passScore: 60, duration: 60, status: 1,
})

// 组卷配置状态
const configVisible = ref(false)
const configPaper = ref<any>({})
const questions = ref<any[]>([])
const selectedRows = ref<any[]>([])
const savingConfig = ref(false)
const boundMap = ref<Record<number, number>>({}) // paperId -> 题目数
const tableRef = ref<any>(null)

const typeText = (t: number) => {
  const map: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空', 5: '简答' }
  return map[t] || '未知'
}
const rowClassName = ({ row }: any) => {
  if (row.type === 5) return 'row-subjective'
  return ''
}
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
const selectedTotal = computed(() => selectedRows.value.reduce((s: number, r: any) => s + Number(r.score || 0), 0))

const fetchPapers = async () => {
  const res = await request.get('/admin/papers')
  if (res.data.code === 200) papers.value = res.data.data
  // 加载每张试卷已绑定的题目数
  for (const p of papers.value) {
    try {
      const r = await request.get(`/admin/paper/${p.id}/questions`)
      if (r.data.code === 200) boundMap.value[p.id] = r.data.data.questionCount || 0
    } catch (e) { boundMap.value[p.id] = 0 }
  }
}
const boundCount = (paperId: number) => boundMap.value[paperId] ?? '?'

const openCreate = () => {
  form.value = { title: '', type: 1, totalScore: 100, passScore: 60, duration: 60, status: 1 }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title) { ElMessage.warning('请填写试卷名称'); return }
  const res = await request.post('/admin/paper', form.value)
  if (res.data.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchPapers()
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除? (试卷绑定的题目配置将一并删除)', '提示', { type: 'warning' })
  const res = await request.delete(`/admin/paper/${id}`)
  if (res.data.code === 200) {
    ElMessage.success('删除成功')
    fetchPapers()
  }
}

// 打开配置题目弹窗
const openConfig = async (row: any) => {
  configPaper.value = row
  configVisible.value = true
  try {
    const [qRes, bRes] = await Promise.all([
      request.get('/admin/questions'),
      request.get(`/admin/paper/${row.id}/questions`),
    ])
    questions.value = (qRes.data.data || []).map((q: any) => ({
      id: q.id, type: q.type, content: q.content, score: q.score, subjectId: q.subjectId,
    }))
    // 回显已绑定题目勾选
    const bound: number[] = bRes.data.code === 200 ? (bRes.data.data.boundQuestionIds || []) : []
    await nextTick()
    tableRef.value?.clearSelection()
    questions.value.forEach((q: any) => {
      if (bound.includes(q.id)) tableRef.value?.toggleRowSelection(q, true)
    })
  } catch (e) {
    ElMessage.error('加载题目失败')
  }
}
const onSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

const saveConfig = async () => {
  savingConfig.value = true
  try {
    const ids = selectedRows.value.map((r: any) => r.id)
    const res = await request.post(`/admin/paper/${configPaper.value.id}/questions`, { questionIds: ids })
    if (res.data.code === 200) {
      ElMessage.success(res.data.msg || '保存成功')
      configVisible.value = false
      fetchPapers()
    } else {
      ElMessage.error(res.data.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  } finally {
    savingConfig.value = false
  }
}

onMounted(fetchPapers)
</script>

<style>
.row-subjective {
  background-color: #fdf6ec;
}
</style>
