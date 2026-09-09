<template>
  <div class="exam-room" v-if="paper">
    <!-- 试卷封皮/考试须知弹窗 -->
    <el-dialog
      v-model="showExamNotice"
      title="考试须知"
      width="560px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      center
    >
      <div class="notice-content">
        <h3 style="text-align:center; color:#1e3a8a; margin-top:0">《{{ paper.title }}》考试须知</h3>
        <ol class="notice-list">
          <li>本次考试时长 <b>{{ paper.duration }} 分钟</b>，总分 <b>{{ paper.totalScore }} 分</b>，及格分数 <b>{{ paper.passScore }} 分</b>。</li>
          <li>本卷共 <b>{{ questions.length }} 题</b>，请合理分配时间作答。</li>
          <li>考试期间系统将自动保存您的答题记录，请勿关闭浏览器或刷新页面。</li>
          <li>切屏、复制、粘贴、右键等行为将被系统记录，超过 <b>{{ MAX_CHEAT_COUNT }} 次</b> 将强制交卷。</li>
          <li>客观题（单选/多选/判断/填空）由系统自动评阅，主观题（简答）由教师人工批阅。</li>
          <li>到达考试时间系统将自动提交答卷，请注意屏幕右上角的倒计时。</li>
          <li>如遇设备或网络异常，请及时联系监考老师。</li>
        </ol>
        <el-checkbox v-model="noticeAccepted">
          我已仔细阅读并同意以上考试须知
        </el-checkbox>
      </div>
      <template #footer>
        <el-button type="primary" :disabled="!noticeAccepted" @click="startExam" size="large">
          {{ noticeAccepted ? '开始考试' : '请先勾选须知' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 顶部固定信息栏 - 正式试卷抬头 -->
    <div class="exam-header">
      <div class="header-left">
        <div class="exam-title-bar">
          <span class="exam-icon">📝</span>
          <h2 class="exam-title">{{ paper.title }}</h2>
          <el-tag size="small" type="danger" effect="dark" class="exam-status-tag">考试中</el-tag>
        </div>
        <div class="exam-meta">
          <span><el-icon><User /></el-icon> 考生：{{ candidateInfo.name }}</span>
          <span><el-icon><Postcard /></el-icon> 考号：{{ candidateInfo.examNo }}</span>
          <span><el-icon><Document /></el-icon> 共 {{ questions.length }} 题</span>
          <span><el-icon><Trophy /></el-icon> 总分 {{ paper.totalScore }} 分</span>
          <span><el-icon><Check /></el-icon> 及格 {{ paper.passScore }} 分</span>
          <span><el-icon><Histogram /></el-icon> 时长 {{ paper.duration }} 分钟</span>
        </div>
      </div>
      <div class="header-right">
        <div class="timer-box" :class="{ warning: timeLeft < 300, danger: timeLeft < 60 }">
          <el-icon class="timer-icon"><Clock /></el-icon>
          <div class="timer-text">
            <div class="timer-label">剩余时间</div>
            <div class="timer-value">{{ formatTime(timeLeft) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="exam-body">
      <!-- 左侧主答题区 - 试卷主体 -->
      <div class="main-area">
        <!-- 试卷头部信息 -->
        <div class="paper-cover">
          <div class="paper-info">
            <h1 class="paper-name">{{ paper.title }}</h1>
            <div class="paper-subtitle">在线考试试卷</div>
            <div class="paper-candidate">
              <div class="candidate-field">
                <span class="field-label">姓名：</span>
                <span class="field-value">{{ candidateInfo.name }}</span>
              </div>
              <div class="candidate-field">
                <span class="field-label">考号：</span>
                <span class="field-value">{{ candidateInfo.examNo }}</span>
              </div>
              <div class="candidate-field">
                <span class="field-label">开考时间：</span>
                <span class="field-value">{{ startTimeStr }}</span>
              </div>
              <div class="candidate-field">
                <span class="field-label">试卷编号：</span>
                <span class="field-value">P{{ String(paper.id).padStart(6, '0') }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 答题须知 -->
        <el-alert
          title="答题须知"
          type="info"
          :closable="false"
          class="exam-instructions"
          show-icon
        >
          <template #default>
            <ol class="instructions-list">
              <li>请使用鼠标点击选项前的字母进行作答，已作答的题目在答题卡中将显示为绿色。</li>
              <li>系统每 30 秒自动保存一次答题记录，无需手动操作。</li>
              <li>对暂不确定的题目可点击「标记题目」按钮标记，标记后该题会有醒目提示。</li>
              <li>答题卡中数字代表题号，点击可快速跳转到对应题目。</li>
            </ol>
          </template>
        </el-alert>

        <!-- 进度条 -->
        <div class="progress-section">
          <div class="progress-info">
            <div class="progress-stats">
              <span class="stat-item">
                <el-icon><EditPen /></el-icon>
                答题进度：<b>{{ answeredCount }}</b> / {{ questions.length }}
              </span>
              <span class="stat-item">
                <el-icon><Star /></el-icon>
                标记题目：<b>{{ markedQuestions.size }}</b>
              </span>
              <span class="stat-item">
                <el-icon><Warning /></el-icon>
                未答题：<b>{{ questions.length - answeredCount }}</b>
              </span>
            </div>
            <span class="save-status" :class="{ saved: savedAt }">
              <el-icon><CircleCheck v-if="savedAt" /><Loading v-else /></el-icon>
              {{ savedAt ? `已自动保存 ${formatSaveTime(savedAt)}` : '正在保存...' }}
            </span>
          </div>
          <el-progress
            :percentage="progressPercentage"
            :stroke-width="10"
            :color="progressColor"
            :format="(p: number) => `${p}%`"
          />
        </div>

        <!-- 题目列表 - 按题型分组 -->
        <div class="question-list" v-if="questions.length > 0">
          <div
            v-for="(group, gIndex) in questionGroups"
            :key="group.type"
            class="question-section"
          >
            <!-- 大题标题 -->
            <div class="section-header">
              <div class="section-title">
                <span class="section-num">{{ toChineseNum(gIndex + 1) }}、</span>
                <span class="section-name">{{ getTypeLabel(group.type) }}</span>
                <span class="section-stat">（共 {{ group.questions.length }} 题，合计 {{ group.totalScore }} 分）</span>
              </div>
              <div class="section-progress">
                <el-tag size="small" type="success" effect="plain" v-if="group.answeredCount > 0">
                  已答 {{ group.answeredCount }}/{{ group.questions.length }}
                </el-tag>
              </div>
            </div>

            <!-- 题目卡片 -->
            <el-card
              v-for="(q, qIndex) in group.questions"
              :key="q.id"
              class="question-card"
              :id="`q-${q.id}`"
              :class="{
                'is-current': currentQuestionId === q.id,
                'is-answered': isAnswered(q.id),
                'is-marked': markedQuestions.has(q.id)
              }"
              shadow="never"
            >
              <div class="q-header">
                <div class="q-num">
                  <span class="q-index">{{ getGlobalIndex(q.id) }}.</span>
                  <el-tag :type="getTypeTagType(q.type)" size="small" effect="light">
                    {{ getTypeLabel(q.type) }}
                  </el-tag>
                  <el-tag v-if="isAnswered(q.id)" type="success" size="small" effect="plain">
                    <el-icon><Check /></el-icon> 已作答
                  </el-tag>
                  <el-tag v-else type="info" size="small" effect="plain">
                    <el-icon><EditPen /></el-icon> 未作答
                  </el-tag>
                  <el-tag v-if="markedQuestions.has(q.id)" type="warning" size="small" effect="plain">
                    <el-icon><StarFilled /></el-icon> 已标记
                  </el-tag>
                </div>
                <div class="q-score">（本题 {{ q.score }} 分）</div>
              </div>

              <div class="q-content">
                <pre class="q-content-text">{{ q.content }}</pre>
              </div>

              <div class="q-options">
                <div v-if="q.type === 1 || q.type === 3" class="choice-list">
                  <div
                    v-for="(val, key) in getOptions(q.options)"
                    :key="key"
                    class="choice-item"
                    :class="{ selected: answers[q.id] === key }"
                    @click="setSingleAnswer(q.id, key)"
                  >
                    <span class="choice-label">{{ key }}.</span>
                    <span class="choice-text">{{ val }}</span>
                  </div>
                </div>

                <div v-else-if="q.type === 2" class="choice-list">
                  <div
                    v-for="(val, key) in getOptions(q.options)"
                    :key="key"
                    class="choice-item"
                    :class="{ selected: (answers[q.id] || []).includes(key) }"
                    @click="toggleMultiAnswer(q.id, key)"
                  >
                    <span class="choice-label">
                      <el-icon v-if="(answers[q.id] || []).includes(key)"><Check /></el-icon>
                      <span v-else>{{ key }}.</span>
                    </span>
                    <span class="choice-text">{{ val }}</span>
                  </div>
                  <div class="multi-hint">
                    <el-icon><InfoFilled /></el-icon> 多选题，请选择所有正确答案
                  </div>
                </div>

                <div v-else-if="q.type === 4" class="fill-blank">
                  <div class="blank-inputs">
                    <div
                      v-for="(_, bIdx) in getBlankCount(q)"
                      :key="bIdx"
                      class="blank-item"
                    >
                      <span class="blank-label">{{ bIdx + 1 }}.</span>
                      <el-input
                        v-model="blankAnswers[q.id][bIdx]"
                        placeholder="请输入答案"
                        @input="saveFillBlank(q.id)"
                        clearable
                      />
                    </div>
                  </div>
                </div>

                <div v-else class="text-answer">
                  <el-input
                    v-model="answers[q.id]"
                    type="textarea"
                    :rows="6"
                    placeholder="请在此输入您的答案..."
                    @input="saveAnswersLocal"
                    maxlength="2000"
                    show-word-limit
                  />
                </div>
              </div>

              <div class="q-footer">
                <el-button
                  :type="markedQuestions.has(q.id) ? 'warning' : 'default'"
                  size="small"
                  @click="toggleMark(q.id)"
                  plain
                >
                  <el-icon><StarFilled v-if="markedQuestions.has(q.id)" /><Star v-else /></el-icon>
                  {{ markedQuestions.has(q.id) ? '取消标记' : '标记题目' }}
                </el-button>
                <div class="q-nav-buttons">
                  <el-button
                    size="small"
                    :disabled="qIndex === 0 && gIndex === 0"
                    @click="prevQuestion(gIndex, qIndex)"
                  >
                    <el-icon><ArrowLeft /></el-icon> 上一题
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="isLastQuestion(gIndex, qIndex)"
                    @click="nextQuestion(gIndex, qIndex)"
                  >
                    下一题 <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <!-- 试卷底部 - 交卷区 -->
        <div class="paper-end">
          <div class="end-line">— — — — — — — — — — — — — — — — — — —</div>
          <div class="end-text">本试卷共 {{ questions.length }} 题，已答 {{ answeredCount }} 题</div>
          <div class="end-line">— — — — — — — — — — — — — — — — — — —</div>
        </div>

        <!-- 底部操作区 -->
        <div class="exam-footer">
          <el-button size="large" @click="quitExam">
            <el-icon><Close /></el-icon> 放弃考试
          </el-button>
          <el-button
            type="warning"
            size="large"
            @click="confirmSubmit"
            :disabled="answeredCount === 0"
          >
            <el-icon><Upload /></el-icon> 提交答卷
          </el-button>
        </div>
      </div>

      <!-- 右侧答题卡 -->
      <div class="side-area">
        <div class="answer-card">
          <div class="card-header">
            <h3>📋 答题卡</h3>
            <el-tag size="small" effect="plain" type="primary">
              {{ answeredCount }}/{{ questions.length }}
            </el-tag>
          </div>

          <div class="card-progress">
            <el-progress
              type="circle"
              :percentage="progressPercentage"
              :width="80"
              :stroke-width="8"
              :color="progressColor"
            />
            <div class="progress-text">完成度</div>
          </div>

          <div class="card-legend">
            <div class="legend-item">
              <span class="legend-dot answered"></span>
              <span>已答 <b>{{ answeredCount }}</b></span>
            </div>
            <div class="legend-item">
              <span class="legend-dot unanswered"></span>
              <span>未答 <b>{{ questions.length - answeredCount }}</b></span>
            </div>
            <div class="legend-item">
              <span class="legend-dot marked"></span>
              <span>标记 <b>{{ markedQuestions.size }}</b></span>
            </div>
          </div>

          <!-- 按题型分组的答题卡 -->
          <div class="card-sections">
            <div
              v-for="(group, gIndex) in questionGroups"
              :key="group.type"
              class="card-section"
            >
              <div class="card-section-title">
                <span>{{ toChineseNum(gIndex + 1) }}、{{ getTypeLabel(group.type) }}</span>
                <span class="section-mini-stat">
                  <el-tag
                    v-if="group.answeredCount === group.questions.length"
                    type="success"
                    size="small"
                    effect="plain"
                  >已完成</el-tag>
                  <el-tag
                    v-else-if="group.answeredCount > 0"
                    type="warning"
                    size="small"
                    effect="plain"
                  >进行中</el-tag>
                  <el-tag v-else type="info" size="small" effect="plain">未开始</el-tag>
                </span>
              </div>
              <div class="card-grid">
                <div
                  v-for="(q, qIndex) in group.questions"
                  :key="q.id"
                  class="card-cell"
                  :class="{
                    answered: isAnswered(q.id),
                    unanswered: !isAnswered(q.id),
                    marked: markedQuestions.has(q.id),
                    current: currentQuestionId === q.id
                  }"
                  :title="`第 ${getGlobalIndex(q.id)} 题`"
                  @click="scrollToQuestion(q.id)"
                >
                  {{ getGlobalIndex(q.id) }}
                </div>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="card-summary">
            <div class="summary-title">📊 答题统计</div>
            <div class="summary-row" v-for="(group, gIndex) in questionGroups" :key="group.type">
              <el-tag size="small" :type="getTypeTagType(group.type)" effect="plain">
                {{ getTypeLabel(group.type) }}
              </el-tag>
              <span class="summary-stat">
                <span class="num-answered">{{ group.answeredCount }}</span>
                <span class="num-divider">/</span>
                <span class="num-total">{{ group.questions.length }}</span>
              </span>
              <el-progress
                :percentage="group.questions.length === 0 ? 0 : Math.round((group.answeredCount / group.questions.length) * 100)"
                :stroke-width="4"
                :show-text="false"
                :color="getTypeTagColor(group.type)"
                style="flex: 1; margin-left: 8px;"
              />
            </div>
          </div>

          <div class="card-actions">
            <el-button type="primary" size="default" @click="confirmSubmit" style="width: 100%;">
              <el-icon><Upload /></el-icon> 立即交卷
            </el-button>
          </div>
        </div>

        <!-- 考试信息卡片 -->
        <div class="info-card">
          <div class="info-title">🛈 考试信息</div>
          <div class="info-row">
            <span class="info-label">开考时间</span>
            <span class="info-value">{{ startTimeStr }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">已用时间</span>
            <span class="info-value">{{ formatTime(elapsedTime) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">剩余时间</span>
            <span class="info-value" :class="{ danger: timeLeft < 300 }">
              {{ formatTime(timeLeft) }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">答题卡</span>
            <span class="info-value">{{ answeredCount }}/{{ questions.length }} 题</span>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else v-loading="true" class="loading-mask"></div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  Document, Trophy, Check, Clock, CircleCheck, Loading, EditPen,
  StarFilled, Star, ArrowLeft, ArrowRight, Close, Upload, User,
  Postcard, Histogram, Warning, InfoFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const paper = ref<any>(null)
const questions = ref<any[]>([])
const answers = ref<any>({})
const blankAnswers = ref<any>({})
// 缺陷修复: 保存后端返回的考试批次ID, 交卷时上报, 成绩才能按批次落库
const currentBatchId = ref<number | null>(null)
const markedQuestions = ref<Set<number>>(new Set())
const currentQuestionId = ref<number | null>(null)
const savedAt = ref<Date | null>(null)

// 考试须知
const showExamNotice = ref(true)
const noticeAccepted = ref(false)
const startTime = ref<Date | null>(null)
const startTimeStr = ref('')
const elapsedTime = ref(0)

// 考生信息（实际项目应从用户登录信息中获取）
const candidateInfo = ref({
  name: '考生',
  examNo: 'K20240001',
})

// 考试控制状态
const timeLeft = ref(0)
let timer: any = null
let saveTimer: any = null
let cheatCount = 0
const MAX_CHEAT_COUNT = 3
// 缺陷修复v3: 交卷成功后置位, 切屏监听/禁止操作监听立即失效(不再警告已结束的考试)
let examFinished = false

const TYPE_LABELS: Record<number, string> = {
  1: '单项选择题',
  2: '多项选择题',
  3: '判断题',
  4: '填空题',
  5: '简答题',
}

const TYPE_TAG_TYPES: Record<number, string> = {
  1: 'primary',
  2: 'success',
  3: 'warning',
  4: 'info',
  5: 'danger',
}

const TYPE_TAG_COLORS: Record<number, string> = {
  1: '#409eff',
  2: '#67c23a',
  3: '#e6a23c',
  4: '#909399',
  5: '#f56c6c',
}

const CHINESE_NUMS = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
const toChineseNum = (n: number) => CHINESE_NUMS[n - 1] || String(n)

const formatTime = (seconds: number) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const formatSaveTime = (date: Date) => {
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
}

const getTypeLabel = (type: number) => TYPE_LABELS[type] || '未知'
const getTypeTagType = (type: number) => TYPE_TAG_TYPES[type] || 'info'
const getTypeTagColor = (type: number) => TYPE_TAG_COLORS[type] || '#909399'

const isAnswered = (qid: number): boolean => {
  const a = answers.value[qid]
  if (a === undefined || a === null || a === '') return false
  if (Array.isArray(a) && a.length === 0) return false
  return true
}

const answeredCount = computed(() => questions.value.filter(q => isAnswered(q.id)).length)

const progressPercentage = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})

const progressColor = computed(() => {
  const p = progressPercentage.value
  if (p < 30) return '#f56c6c'
  if (p < 70) return '#e6a23c'
  return '#67c23a'
})

// 按题型分组
const questionGroups = computed(() => {
  const groups: any[] = []
  const typeOrder = [1, 2, 3, 4, 5]
  for (const type of typeOrder) {
    const qs = questions.value.filter(q => q.type === type)
    if (qs.length > 0) {
      groups.push({
        type,
        questions: qs,
        totalScore: qs.reduce((s, q) => s + (q.score || 0), 0),
        answeredCount: qs.filter(q => isAnswered(q.id)).length,
      })
    }
  }
  return groups
})

// 获取题目在整张试卷中的全局编号
const getGlobalIndex = (qid: number) => {
  return questions.value.findIndex(q => q.id === qid) + 1
}

const isLastQuestion = (gIndex: number, qIndex: number) => {
  if (gIndex === questionGroups.value.length - 1 &&
      qIndex === questionGroups.value[gIndex].questions.length - 1) {
    return true
  }
  return false
}

const prevQuestion = (gIndex: number, qIndex: number) => {
  if (qIndex > 0) {
    const q = questionGroups.value[gIndex].questions[qIndex - 1]
    scrollToQuestion(q.id)
  } else if (gIndex > 0) {
    const prevGroup = questionGroups.value[gIndex - 1]
    const q = prevGroup.questions[prevGroup.questions.length - 1]
    scrollToQuestion(q.id)
  }
}

const nextQuestion = (gIndex: number, qIndex: number) => {
  const group = questionGroups.value[gIndex]
  if (qIndex < group.questions.length - 1) {
    scrollToQuestion(group.questions[qIndex + 1].id)
  } else if (gIndex < questionGroups.value.length - 1) {
    const nextGroup = questionGroups.value[gIndex + 1]
    scrollToQuestion(nextGroup.questions[0].id)
  }
}

const setSingleAnswer = (qid: number, key: string) => {
  answers.value[qid] = key
  currentQuestionId.value = qid
  saveAnswersLocal()
}

const toggleMultiAnswer = (qid: number, key: string) => {
  const cur = (answers.value[qid] as string[]) || []
  if (cur.includes(key)) {
    answers.value[qid] = cur.filter(k => k !== key)
  } else {
    answers.value[qid] = [...cur, key]
  }
  currentQuestionId.value = qid
  saveAnswersLocal()
}

const toggleMark = (qid: number) => {
  if (markedQuestions.value.has(qid)) {
    markedQuestions.value.delete(qid)
  } else {
    markedQuestions.value.add(qid)
  }
  markedQuestions.value = new Set(markedQuestions.value)
  saveAnswersLocal()
}

// 填空题
const getBlankCount = (q: any) => {
  // 优先使用 blankCount 字段，否则从题干中检测 ___ 个数
  if (q.blankCount) return q.blankCount
  const matches = (q.content || '').match(/_{2,}/g) || []
  return Math.max(matches.length, 1)
}

const saveFillBlank = (qid: number) => {
  const arr = blankAnswers.value[qid] || []
  answers.value[qid] = arr.filter(s => s && s.trim()).join('|||')
  currentQuestionId.value = qid
  saveAnswersLocal()
}

const scrollToQuestion = (qid: number) => {
  currentQuestionId.value = qid
  nextTick(() => {
    const el = document.getElementById(`q-${qid}`)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const fetchPaperDetail = async () => {
  const id = route.params.id
  try {
    const res = await request.get(`/exam/paper/${id}?_t=${Date.now()}`)
    if (res.data.code === 200) {
      paper.value = res.data.data.paper
      questions.value = res.data.data.questions

      // 初始化填空题答案
      for (const q of questions.value) {
        if (q.type === 4) {
          const count = getBlankCount(q)
          blankAnswers.value[q.id] = new Array(count).fill('')
        }
      }

      // 设置考生信息
      const userInfo = localStorage.getItem('user_info')
      if (userInfo) {
        try {
          const u = JSON.parse(userInfo)
          candidateInfo.value.name = u.realName || u.username || '考生'
          candidateInfo.value.examNo = u.examNo || `K${u.id?.toString().padStart(6, '0') || '20240001'}`
        } catch (e) {}
      }

      // 缺陷修复: 记录后端下发的考试批次ID
      currentBatchId.value = res.data.data.batchId || null

      timeLeft.value = paper.value.duration * 60
      loadAnswersLocal()

      // 缺陷修复v3: 进入考试前检查是否已交卷 —— 已交卷直接拦截, 提示并返回列表
      checkAlreadySubmitted()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('加载试卷失败')
  }
}

const getOptions = (optionsStr: string) => {
  try {
    return JSON.parse(optionsStr)
  } catch (e) {
    return {}
  }
}

const saveAnswersLocal = () => {
  if (paper.value) {
    savedAt.value = null
    const data = {
      answers: answers.value,
      marked: Array.from(markedQuestions.value),
      blanks: blankAnswers.value,
    }
    localStorage.setItem(`exam_answers_${paper.value.id}`, JSON.stringify(data))
    clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      savedAt.value = new Date()
    }, 400)
  }
}

const loadAnswersLocal = () => {
  if (paper.value) {
    const saved = localStorage.getItem(`exam_answers_${paper.value.id}`)
    if (saved) {
      try {
        const data = JSON.parse(saved)
        answers.value = data.answers || {}
        markedQuestions.value = new Set(data.marked || [])
        blankAnswers.value = data.blanks || {}
        // 恢复填空题初始空数组
        for (const q of questions.value) {
          if (q.type === 4 && !blankAnswers.value[q.id]) {
            const count = getBlankCount(q)
            blankAnswers.value[q.id] = new Array(count).fill('')
          }
        }
        ElMessage.success('已恢复上次答题进度')
      } catch (e) {
        answers.value = JSON.parse(saved)
      }
    } else {
      // 初始化填空题空数组
      for (const q of questions.value) {
        if (q.type === 4) {
          const count = getBlankCount(q)
          blankAnswers.value[q.id] = new Array(count).fill('')
        }
      }
    }
    savedAt.value = new Date()
  }
}

const clearAnswersLocal = () => {
  if (paper.value) {
    localStorage.removeItem(`exam_answers_${paper.value.id}`)
  }
}

// 缺陷修复v3: 进入考试前检查当前用户是否已交过本批次 —— 已交卷则拦截并提示
const checkAlreadySubmitted = async () => {
  if (!currentBatchId.value) return
  const userInfo = localStorage.getItem('user_info')
  let userId: number | null = null
  if (userInfo) {
    try { userId = JSON.parse(userInfo).id } catch (e) {}
  }
  if (!userId) return
  try {
    const res = await request.get(`/exam/my-scores?userId=${userId}`)
    if (res.data.code === 200) {
      const rows = res.data.data || []
      const done = rows.some((s: any) => Number(s.batchId) === Number(currentBatchId.value))
      if (done) {
        examFinished = true // 已交卷: 本页面不再触发切屏/禁止操作警告
        ElMessageBox.alert('您已提交过本场考试的答卷，不能重复进入考试。', '提示', {
          type: 'warning',
          showClose: false,
          confirmButtonText: '返回考试列表',
          callback: () => router.push('/exam/list'),
        })
      }
    }
  } catch (e) {
    // 网络异常不阻断进入考试, 交由交卷时的后端 403 兜底
    console.warn('检查已交卷状态失败', e)
  }
}

const startTimer = () => {
  timer = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
      if (startTime.value) {
        elapsedTime.value = Math.floor((Date.now() - startTime.value.getTime()) / 1000)
      }
    } else {
      clearInterval(timer)
      ElMessageBox.alert('考试时间到，系统已自动交卷', '提示', {
        confirmButtonText: '确定',
        callback: () => submitExam(true),
      })
    }
  }, 1000)
}

const startExam = () => {
  showExamNotice.value = false
  startTime.value = new Date()
  startTimeStr.value = `${startTime.value.getFullYear()}-${(startTime.value.getMonth() + 1).toString().padStart(2, '0')}-${startTime.value.getDate().toString().padStart(2, '0')} ${startTime.value.getHours().toString().padStart(2, '0')}:${startTime.value.getMinutes().toString().padStart(2, '0')}`
  startTimer()
  ElMessage.success('考试已开始，请合理分配时间')
}

const handleVisibilityChange = () => {
  if (examFinished) return // 缺陷修复v3: 已交卷不再触发切屏警告
  if (document.hidden) {
    cheatCount++
    if (cheatCount >= MAX_CHEAT_COUNT) {
      ElMessageBox.alert('您已多次离开考试页面，系统将强制交卷！', '警告', {
        type: 'error',
        showClose: false,
        confirmButtonText: '确定',
        callback: () => submitExam(true),
      })
    } else {
      ElNotification({
        title: '⚠️ 警告',
        message: `检测到切屏！离开考试页面达到 ${MAX_CHEAT_COUNT} 次将强制交卷。当前次数: ${cheatCount}/${MAX_CHEAT_COUNT}`,
        type: 'warning',
        duration: 0,
      })
    }
  }
}

const preventDefaultAction = (e: Event) => {
  if (examFinished) return // 缺陷修复v3: 已交卷不再禁止复制/粘贴等操作
  e.preventDefault()
  ElMessage.warning('考试期间禁止此操作！')
}

const confirmSubmit = () => {
  const unanswered = questions.value.length - answeredCount.value
  let confirmText = '确定要交卷吗？'
  if (unanswered > 0) {
    confirmText = `您还有 ${unanswered} 道题未作答，确定要交卷吗？`
  }
  ElMessageBox.confirm(confirmText, '提交答卷', {
    confirmButtonText: '确定交卷',
    cancelButtonText: '再检查一下',
    type: 'warning',
    distinguishCancelAndClose: true,
  }).then(() => submitExam(false)).catch(() => {})
}

const submitExam = async (isAuto = false) => {
  const doSubmit = async () => {
    try {
      // 缺陷修复: 交卷前把所有填空题(含未触发@input最后一步)固化进 answers
      for (const q of questions.value) {
        if (q.type === 4) saveFillBlank(q.id)
      }
      const formattedAnswers: Record<string, string> = {}
      for (const [key, val] of Object.entries(answers.value)) {
        if (Array.isArray(val)) {
          formattedAnswers[key] = [...val].sort().join(',')
        } else {
          formattedAnswers[key] = String(val)
        }
      }

      // 缺陷修复: 交卷必须携带 试卷ID + 考生ID + 考试批次ID, 后端才能判分落库
      const userInfo = localStorage.getItem('user_info')
      let userId: number | null = null
      if (userInfo) {
        try { userId = JSON.parse(userInfo).id } catch (e) {}
      }
      if (!userId) {
        ElMessage.error('登录状态已失效, 请返回重新登录')
        return
      }
      const res = await request.post('/exam/submit', {
        paperId: paper.value.id,
        userId,
        batchId: currentBatchId.value,
        answers: formattedAnswers,
      })
      if (res.data.code === 200) {
        // 缺陷修复v3: 交卷成功立即置位 —— 切屏/复制粘贴监听失效, 倒计时停止, 不再警告
        examFinished = true
        if (timer) clearInterval(timer)
        clearAnswersLocal()
        ElMessageBox.alert(
          `🎉 交卷成功！\n\n您的客观题得分是：${res.data.score} 分\n\n${res.data.msg || ''}`,
          '考试结束',
          {
            confirmButtonText: '返回首页',
            callback: () => router.push('/'),
          }
        )
      } else {
        // 缺陷修复v3: 后端拒绝(如重复交卷 403)时给出明确提示并返回首页
        if (res.data.code === 403) {
          examFinished = true
          if (timer) clearInterval(timer)
          ElMessageBox.alert(res.data.msg || '本次交卷被拒绝', '提示', {
            type: 'warning',
            confirmButtonText: '返回首页',
            callback: () => router.push('/'),
          })
        } else {
          ElMessage.error(res.data.msg)
        }
      }
    } catch (e) {
      ElMessage.error('交卷失败')
    }
  }

  if (isAuto) {
    await doSubmit()
  } else {
    await doSubmit()
  }
}

const quitExam = () => {
  ElMessageBox.confirm('确定要放弃本次考试吗？进度将不会保留。', '提示', {
    confirmButtonText: '确定放弃',
    cancelButtonText: '继续考试',
    type: 'warning',
  }).then(() => {
    clearAnswersLocal()
    router.push('/exam/list')
  }).catch(() => {})
}

onMounted(() => {
  fetchPaperDetail()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  document.addEventListener('contextmenu', preventDefaultAction)
  document.addEventListener('copy', preventDefaultAction)
  document.addEventListener('paste', preventDefaultAction)
  document.addEventListener('cut', preventDefaultAction)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (saveTimer) clearTimeout(saveTimer)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('contextmenu', preventDefaultAction)
  document.removeEventListener('copy', preventDefaultAction)
  document.removeEventListener('paste', preventDefaultAction)
  document.removeEventListener('cut', preventDefaultAction)
})
</script>

<style scoped>
.exam-room {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f2f5 0%, #e6e9ed 100%);
  padding: 16px 24px 32px;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

/* 考试须知 */
.notice-content {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}
.notice-list {
  padding-left: 24px;
  margin: 12px 0 20px;
}
.notice-list li {
  margin-bottom: 8px;
}

/* 顶部固定信息栏 - 正式试卷抬头 */
.exam-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #0f2a5f 0%, #1e3a8a 50%, #2563eb 100%);
  color: #fff;
  padding: 14px 28px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(15, 42, 95, 0.2);
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.exam-title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.exam-icon {
  font-size: 24px;
}

.exam-title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.exam-status-tag {
  margin-left: 6px;
  font-weight: 600;
  letter-spacing: 1px;
}

.exam-meta {
  display: flex;
  gap: 18px;
  font-size: 12.5px;
  opacity: 0.92;
  flex-wrap: wrap;
}

.exam-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.timer-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.12);
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(8px);
}

.timer-icon {
  font-size: 26px;
}

.timer-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.timer-label {
  font-size: 11px;
  opacity: 0.85;
  letter-spacing: 1px;
}

.timer-value {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  letter-spacing: 1.5px;
  margin-top: 2px;
}

.timer-box.warning {
  background: rgba(230, 162, 60, 0.3);
  border-color: rgba(230, 162, 60, 0.7);
}

.timer-box.danger {
  background: rgba(245, 108, 108, 0.5);
  border-color: rgba(245, 108, 108, 0.9);
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 主体两栏布局 */
.exam-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  max-width: 1500px;
  margin: 0 auto;
}

.main-area {
  min-width: 0;
}

.side-area {
  position: sticky;
  top: 110px;
  align-self: flex-start;
  max-height: calc(100vh - 130px);
  overflow-y: auto;
}

/* 试卷封皮 */
.paper-cover {
  background: #fff;
  border-radius: 8px;
  padding: 24px 32px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #e4e7ed;
  background-image:
    linear-gradient(180deg, #fff 0%, #fafbfc 100%);
  position: relative;
  overflow: hidden;
}

.paper-cover::before {
  content: '试';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-30deg);
  font-size: 200px;
  font-weight: 900;
  color: rgba(30, 58, 138, 0.03);
  pointer-events: none;
  user-select: none;
  font-family: 'STKaiti', 'KaiTi', 'SimSun', serif;
}

.paper-info {
  position: relative;
  text-align: center;
}

.paper-name {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a8a;
  margin: 0 0 6px 0;
  letter-spacing: 1px;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.paper-subtitle {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
  letter-spacing: 4px;
}

.paper-candidate {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 16px;
  border-top: 2px solid #1e3a8a;
  border-bottom: 1px solid #dcdfe6;
  padding-bottom: 16px;
}

.candidate-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.field-label {
  color: #606266;
}

.field-value {
  color: #303133;
  font-weight: 600;
  border-bottom: 1px solid #303133;
  padding: 0 8px;
  min-width: 80px;
  display: inline-block;
  text-align: center;
}

/* 答题须知 */
.exam-instructions {
  margin-bottom: 16px;
  border-radius: 8px;
}

.instructions-list {
  margin: 4px 0 0 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
}

/* 进度条区域 */
.progress-section {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e4e7ed;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
  gap: 8px;
}

.progress-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.stat-item b {
  color: #1e3a8a;
  font-size: 14px;
}

.save-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

.save-status.saved {
  color: #67c23a;
}

/* 题目列表 - 按题型分组 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.question-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, #1e3a8a 0%, #2563eb 100%);
  color: #fff;
  padding: 10px 20px;
  border-bottom: 2px solid #1e3a8a;
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.section-num {
  font-size: 17px;
  font-weight: 700;
}

.section-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.section-stat {
  font-size: 13px;
  opacity: 0.9;
  font-weight: 400;
  font-family: 'Microsoft YaHei', sans-serif;
}

/* 题目卡片 */
.question-card {
  border-radius: 0;
  border: none;
  border-top: 1px solid #ebeef5;
  margin: 0;
  transition: background-color 0.2s, border-color 0.2s;
  scroll-margin-top: 110px;
}

.question-card:first-child {
  border-top: none;
}

.question-card.is-current {
  background: #f0f7ff;
}

.question-card.is-answered {
  border-left: 3px solid #67c23a;
}

.question-card.is-marked {
  border-left: 3px solid #e6a23c;
}

.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.q-num {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.q-index {
  font-size: 17px;
  font-weight: 700;
  color: #1e3a8a;
  font-family: 'STKaiti', 'KaiTi', serif;
  margin-right: 4px;
}

.q-score {
  color: #909399;
  font-size: 13px;
}

.q-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 14px;
}

.q-content-text {
  margin: 0;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 选项样式 */
.choice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.choice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafbfc;
}

.choice-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateX(2px);
}

.choice-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
  color: #1e3a8a;
  font-weight: 500;
  box-shadow: 0 0 0 1px #409eff inset;
}

.choice-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #dcdfe6;
  font-weight: 600;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.choice-item.selected .choice-label {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.choice-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
  padding-top: 2px;
}

.multi-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
  padding-left: 4px;
}

/* 填空题 */
.fill-blank {
  padding: 4px 0;
}

.blank-inputs {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.blank-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.blank-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 32px;
  font-weight: 600;
  color: #1e3a8a;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.blank-item .el-input {
  flex: 1;
}

/* 简答题 */
.text-answer {
  padding: 4px 0;
}

/* 题目底部 */
.q-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.q-nav-buttons {
  display: flex;
  gap: 8px;
}

/* 试卷底部 */
.paper-end {
  text-align: center;
  padding: 32px 0 16px;
  color: #909399;
  font-size: 13px;
  letter-spacing: 2px;
}

.end-line {
  font-family: 'STKaiti', 'KaiTi', 'Microsoft YaHei', sans-serif;
}

.end-text {
  margin: 12px 0;
  font-weight: 500;
  color: #606266;
}

/* 底部操作 */
.exam-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e4e7ed;
}

/* 答题卡 */
.answer-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e4e7ed;
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.card-header h3 {
  margin: 0;
  font-size: 15px;
  color: #1e3a8a;
  font-weight: 700;
}

.card-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.card-legend {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #606266;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-item b {
  color: #1e3a8a;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  border: 1px solid #dcdfe6;
}

.legend-dot.answered {
  background: #67c23a;
  border-color: #67c23a;
}

.legend-dot.unanswered {
  background: #fff;
  border-color: #dcdfe6;
}

.legend-dot.marked {
  background: #e6a23c;
  border-color: #e6a23c;
}

.card-sections {
  max-height: 400px;
  overflow-y: auto;
}

.card-section {
  margin-bottom: 12px;
}

.card-section:last-child {
  margin-bottom: 0;
}

.card-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: #1e3a8a;
  margin-bottom: 6px;
  padding: 4px 6px;
  background: #ecf5ff;
  border-radius: 4px;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 5px;
}

.card-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.15s;
  background: #fff;
  color: #606266;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.card-cell:hover {
  border-color: #409eff;
  transform: scale(1.08);
  z-index: 1;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.3);
}

.card-cell.answered {
  background: #67c23a;
  color: #fff;
  border-color: #67c23a;
}

.card-cell.marked {
  background: #e6a23c;
  color: #fff;
  border-color: #e6a23c;
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.3);
}

.card-cell.unanswered {
  background: #fff;
  color: #909399;
}

.card-cell.current {
  border: 2px solid #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
  transform: scale(1.05);
}

.card-summary {
  font-size: 12px;
  color: #606266;
  margin-bottom: 12px;
}

.summary-title {
  font-weight: 600;
  color: #1e3a8a;
  margin-bottom: 8px;
  font-size: 13px;
}

.summary-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.summary-stat {
  font-weight: 600;
  font-family: 'Consolas', monospace;
}

.num-answered {
  color: #67c23a;
}

.num-divider {
  color: #dcdfe6;
  margin: 0 1px;
}

.num-total {
  color: #909399;
}

.card-actions {
  margin-top: 8px;
}

/* 考试信息卡片 */
.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e4e7ed;
}

.info-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e3a8a;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 12.5px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #303133;
  font-weight: 600;
  font-family: 'Consolas', monospace;
}

.info-value.danger {
  color: #f56c6c;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* 加载状态 */
.loading-mask {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 滚动条美化 */
.card-sections::-webkit-scrollbar,
.side-area::-webkit-scrollbar {
  width: 6px;
}

.card-sections::-webkit-scrollbar-thumb,
.side-area::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.card-sections::-webkit-scrollbar-thumb:hover,
.side-area::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* 响应式 */
@media (max-width: 1100px) {
  .exam-body {
    grid-template-columns: 1fr;
  }
  .side-area {
    position: static;
    max-height: none;
  }
  .exam-meta {
    gap: 12px;
    font-size: 12px;
  }
  .paper-candidate {
    gap: 8px;
  }
  .field-value {
    min-width: 60px;
  }
}
</style>
