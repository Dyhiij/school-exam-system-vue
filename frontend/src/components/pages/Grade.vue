<template>
  <div class="grade">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>人工阅卷 (待阅简答题)</span>
          <el-button type="success" @click="fetchList">
            <el-icon><Refresh /></el-icon> 刷新列表
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" border style="width: 100%">
        <el-table-column prop="paperTitle" label="考试名称" min-width="180" />
        <el-table-column prop="batchTitle" label="考试批次" min-width="150" />
        <el-table-column prop="studentName" label="考生" width="110" />
        <el-table-column prop="objectiveScore" label="客观题得分" width="110" />
        <el-table-column prop="submitTime" label="交卷时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openGrade(row)">批阅简答题</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无待阅卷记录" />
    </el-card>

    <!-- 批阅弹窗 -->
    <el-dialog v-model="gradeDialog" :title="`批阅: ${grading.studentName || ''} - ${grading.paperTitle || ''}`" width="720px" top="6vh">
      <div v-if="detail" style="max-height: 60vh; overflow-y: auto;">
        <el-alert
          :title="`客观题已自动判分: ${detail.objectiveScore} 分(满分 ${detail.paperTotal})。以下为简答题, 请人工给分:`"
          type="info" :closable="false" style="margin-bottom: 12px;" />
        <div v-for="(q, i) in subjectiveQuestions" :key="q.questionId" class="subj-item">
          <p><b>简答{{ i + 1 }}（{{ q.fullScore }}分）</b></p>
          <p class="q-content">{{ q.questionContent }}</p>
          <div class="q-block">
            <p class="q-label">考生答案:</p>
            <div class="answer-box">{{ q.userAnswer || '(未作答)' }}</div>
          </div>
          <div v-if="q.referenceAnswer" class="q-block">
            <p class="q-label">参考答案:</p>
            <div class="answer-box ref">{{ q.referenceAnswer }}</div>
          </div>
          <div class="q-block score-row">
            <span class="q-label">得分:</span>
            <el-input-number v-model="q.gotScore" :min="0" :max="q.fullScore" :step="1" size="small" />
            <span style="margin-left: 8px; color: #999;">/ {{ q.fullScore }} 分</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="gradeDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitGrade">提交评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const rows = ref<any[]>([])
const gradeDialog = ref(false)
const submitting = ref(false)
const grading = ref<any>({})
const detail = ref<any>(null)
const subjectiveQuestions = ref<any[]>([])

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/review/list')
    if (res.data.code === 200) {
      rows.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  } finally {
    loading.value = false
  }
}

// 打开批阅: 拉取该考生本场考试全部作答明细
const openGrade = async (row: any) => {
  grading.value = row
  gradeDialog.value = true
  detail.value = null
  subjectiveQuestions.value = []
  try {
    const res = await request.get(`/exam/detail?batchId=${row.batchId}&userId=${row.userId}`)
    if (res.data.code === 200) {
      detail.value = res.data.data
      const list: any[] = (res.data.data.answers || []).filter((a: any) => Number(a.questionType) === 5)
      subjectiveQuestions.value = list.map((a: any) => ({
        questionId: a.questionId,
        questionContent: a.questionContent,
        userAnswer: a.userAnswer,
        referenceAnswer: a.referenceAnswer,
        fullScore: Number(a.fullScore) || 0,
        gotScore: Number(a.gotScore) || 0,
      }))
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  }
}

const submitGrade = async () => {
  if (subjectiveQuestions.value.length === 0) {
    gradeDialog.value = false
    return
  }
  submitting.value = true
  try {
    const items = subjectiveQuestions.value.map(q => ({
      questionId: q.questionId,
      score: q.gotScore || 0,
    }))
    const res = await request.post('/admin/review/grade', {
      batchId: grading.value.batchId,
      userId: grading.value.userId,
      items,
    })
    if (res.data.code === 200) {
      ElMessage.success(res.data.msg || '评分成功')
      gradeDialog.value = false
      fetchList()
    } else {
      ElMessage.error(res.data.msg || '提交失败')
    }
  } catch (e) {
    ElMessage.error('网络请求失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.subj-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fafafa;
}
.q-content {
  font-weight: 500;
  margin: 4px 0 10px;
}
.q-block {
  margin: 8px 0;
}
.q-label {
  font-size: 13px;
  color: #909399;
  margin: 4px 0;
}
.answer-box {
  background: #fff;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  padding: 8px 10px;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
}
.answer-box.ref {
  color: #67c23a;
}
.score-row {
  display: flex;
  align-items: center;
}
</style>
