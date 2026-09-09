package com.school.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.exam.entity.*;
import com.school.exam.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamPaperMapper examPaperMapper;
    @Autowired
    private QuestionBankMapper questionBankMapper;
    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;
    @Autowired
    private ExamBatchMapper examBatchMapper;
    @Autowired
    private ExamScoreMapper examScoreMapper;
    @Autowired
    private ExamAnswerMapper examAnswerMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;

    // 获取试卷列表
    @GetMapping("/list")
    public Map<String, Object> listPapers() {
        Map<String, Object> result = new HashMap<>();
        List<ExamPaper> papers = examPaperMapper.selectList(new QueryWrapper<ExamPaper>().eq("status", 1));
        result.put("code", 200);
        result.put("data", papers);
        return result;
    }

    /** 取试卷绑定的题目ID列表（固定组卷的题源） */
    private List<Long> boundQuestionIds(Long paperId) {
        List<ExamPaperQuestion> rels = examPaperQuestionMapper.selectList(
                new QueryWrapper<ExamPaperQuestion>().eq("paper_id", paperId).orderByAsc("id"));
        return rels.stream().map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList());
    }

    /** 按试卷取题: 优先试卷绑定题; 未绑定(老数据/演示)则回退全题库 */
    private List<QuestionBank> questionsOfPaper(ExamPaper paper) {
        List<Long> ids = boundQuestionIds(paper.getId());
        QueryWrapper<QuestionBank> qw = new QueryWrapper<>();
        if (!ids.isEmpty()) {
            qw.in("id", ids);
        }
        qw.orderByAsc("id");
        if (paper.getType() != null && paper.getType() == 2) {
            // 随机组卷: 在题源范围内随机抽取(不超过10题, 演示口径)
            List<QuestionBank> pool = questionBankMapper.selectList(qw);
            if (pool.isEmpty()) return pool;
            Collections.shuffle(pool);
            return pool.size() > 10 ? pool.subList(0, 10) : pool;
        }
        return questionBankMapper.selectList(qw);
    }

    /** 判定某卷当前应使用的考试批次: 进行中优先, 其次最近一个; 找不到返回null */
    private ExamBatch resolveBatch(Long paperId) {
        LocalDateTime now = LocalDateTime.now();
        List<ExamBatch> running = examBatchMapper.selectList(new QueryWrapper<ExamBatch>()
                .eq("paper_id", paperId).eq("status", 1)
                .le("start_time", now).ge("end_time", now)
                .orderByDesc("id"));
        if (!running.isEmpty()) return running.get(0);
        List<ExamBatch> any = examBatchMapper.selectList(new QueryWrapper<ExamBatch>()
                .eq("paper_id", paperId).orderByDesc("id"));
        return any.isEmpty() ? null : any.get(0);
    }

    // 获取指定试卷及其题目
    @GetMapping("/paper/{id}")
    public Map<String, Object> getPaper(@PathVariable("id") Long id) {
        Map<String, Object> result = new HashMap<>();
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null) {
            result.put("code", 404);
            result.put("msg", "试卷不存在");
            return result;
        }
        List<QuestionBank> questions = questionsOfPaper(paper);
        // 同时返回当前考试批次信息, 便于交卷时对号入座
        ExamBatch batch = resolveBatch(id);
        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", questions);
        data.put("batchId", batch == null ? null : batch.getId());
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    // 提交答卷: 判分 + 成绩落库 + 作答明细落库(主观题留待人工阅卷)
    @PostMapping("/submit")
    public Map<String, Object> submitExam(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            Object paperIdObj = payload.get("paperId");
            Object userIdObj = payload.get("userId");
            if (paperIdObj == null || userIdObj == null) {
                result.put("code", 400);
                result.put("msg", "参数缺失: 缺少 paperId 或 userId(请确认已登录)");
                return result;
            }
            Long paperId = Long.valueOf(String.valueOf(paperIdObj));
            Long userId = Long.valueOf(String.valueOf(userIdObj));

            ExamPaper paper = examPaperMapper.selectById(paperId);
            if (paper == null) {
                result.put("code", 404);
                result.put("msg", "试卷不存在");
                return result;
            }
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                result.put("code", 401);
                result.put("msg", "考生不存在, 请重新登录");
                return result;
            }

            // 1. 解析考试批次
            ExamBatch batch = null;
            if (payload.get("batchId") != null) {
                batch = examBatchMapper.selectById(Long.valueOf(String.valueOf(payload.get("batchId"))));
            }
            if (batch == null) {
                batch = resolveBatch(paperId);
            }
            if (batch == null) {
                result.put("code", 400);
                result.put("msg", "该试卷暂未安排考试批次, 请联系管理员发布考试");
                return result;
            }
            Long batchId = batch.getId();

            // 缺陷修复v3: 防重复交卷 —— 已交卷(存在成绩记录)则直接拒绝, 不再覆盖
            // (修复前为"覆盖更新", 考生可反复交卷刷分; 改为首次交卷即锁定)
            ExamScore existingScore = examScoreMapper.selectOne(new QueryWrapper<ExamScore>()
                    .eq("batch_id", batchId).eq("user_id", userId));
            if (existingScore != null) {
                result.put("code", 403);
                result.put("msg", "您已提交过答卷，不能重复交卷。如为误操作请联系监考老师。");
                return result;
            }

            // 2. 读取考生答案 (questionId -> 答案文本; 多选题前端已排序逗号拼接, 填空 ||| 分隔)
            @SuppressWarnings("unchecked")
            Map<String, String> userAnswers = (Map<String, String>) payload.get("answers");
            if (userAnswers == null) userAnswers = new HashMap<>();

            // 3. 按试卷取题判分 (不再遍历全题库)
            List<QuestionBank> questions = questionsOfPaper(paper);
            BigDecimal objectiveScore = BigDecimal.ZERO;
            List<ExamAnswer> answersToSave = new ArrayList<>();
            List<ExamAnswer> existing = examAnswerMapper.selectList(new QueryWrapper<ExamAnswer>()
                    .eq("batch_id", batchId).eq("user_id", userId));
            Set<String> answeredKeys = existing.stream()
                    .map(a -> a.getBatchId() + "_" + a.getQuestionId()).collect(Collectors.toSet());

            for (QuestionBank q : questions) {
                String uAns = userAnswers.get(String.valueOf(q.getId()));
                if (uAns == null) continue;
                uAns = uAns.trim();

                ExamAnswer ans = new ExamAnswer();
                ans.setBatchId(batchId);
                ans.setUserId(userId);
                ans.setQuestionId(q.getId());
                ans.setQuestionType(q.getType());
                ans.setUserAnswer(uAns);
                ans.setCorrectFlag(0);
                ans.setGotScore(BigDecimal.ZERO);
                ans.setCreateTime(LocalDateTime.now());

                boolean correct = false;
                if (q.getType() != null && q.getType() <= 4) {
                    // 客观题: 1单选 2多选 3判断 4填空
                    if (q.getType() == 4) {
                        // 填空: 参考答案同样以 ||| 分段, 逐空比对
                        String[] ref = safeSplit(q.getAnswer());
                        String[] got = safeSplit(uAns);
                        if (ref.length == got.length) {
                            correct = true;
                            for (int i = 0; i < ref.length; i++) {
                                if (!ref[i].equalsIgnoreCase(got[i].trim())) { correct = false; break; }
                            }
                        }
                    } else {
                        correct = uAns.equalsIgnoreCase(q.getAnswer());
                    }
                    if (correct && q.getScore() != null) {
                        ans.setCorrectFlag(1);
                        ans.setGotScore(q.getScore());
                        objectiveScore = objectiveScore.add(q.getScore());
                    }
                } else {
                    // 主观题(简答): 保存答案, 标记待人工阅卷
                    ans.setCorrectFlag(0);
                }

                String key = batchId + "_" + q.getId();
                if (answeredKeys.contains(key)) {
                    // 防重复: 更新既有明细(以本次提交为准)
                    ExamAnswer old = existing.stream()
                            .filter(a -> a.getBatchId().equals(batchId) && a.getQuestionId().equals(q.getId()))
                            .findFirst().orElse(null);
                    if (old != null) {
                        ans.setId(old.getId());
                        examAnswerMapper.updateById(ans);
                    }
                } else {
                    examAnswerMapper.insert(ans);
                }
                answersToSave.add(ans);
            }

            // 4. 成绩 upsert (uk: batch_id + user_id; 重复交卷=覆盖, 附作弊/重交提示)
            ExamScore score = examScoreMapper.selectOne(new QueryWrapper<ExamScore>()
                    .eq("batch_id", batchId).eq("user_id", userId));
            boolean resubmit = (score != null);
            if (score == null) {
                score = new ExamScore();
                score.setBatchId(batchId);
                score.setUserId(userId);
                score.setTotalScore(objectiveScore);
                score.setStatus(1); // 1已交卷(客观已出分, 若含主观题状态2由阅卷置为已阅)
                score.setSubmitTime(LocalDateTime.now());
                examScoreMapper.insert(score);
            } else {
                score.setTotalScore(objectiveScore);
                score.setStatus(1);
                score.setSubmitTime(LocalDateTime.now());
                examScoreMapper.updateById(score);
            }

            boolean hasSubjective = questions.stream().anyMatch(q -> q.getType() != null && q.getType() == 5);
            result.put("code", 200);
            result.put("score", objectiveScore);
            result.put("subjectivePending", hasSubjective);
            result.put("msg", "交卷成功。客观题得分: " + objectiveScore
                    + (hasSubjective ? " 分。简答题已提交, 待人工阅卷后公布总分。" : " 分。")
                    + (resubmit ? "(检测到重复交卷, 已按本次覆盖)" : ""));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "交卷处理失败: " + e.getMessage());
        }
        return result;
    }

    private String[] safeSplit(String s) {
        if (s == null) return new String[0];
        return Arrays.stream(s.split("\\|\\|\\|"))
                .map(String::trim).filter(x -> !x.isEmpty()).toArray(String[]::new);
    }

    // 我的成绩: 按考生查询(带试卷/批次标题)
    @GetMapping("/my-scores")
    public Map<String, Object> myScores(@RequestParam("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<ExamScore> scores = examScoreMapper.selectList(new QueryWrapper<ExamScore>()
                .eq("user_id", userId).orderByDesc("submit_time"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamScore s : scores) {
            Map<String, Object> row = new HashMap<>();
            row.put("scoreId", s.getId());
            row.put("batchId", s.getBatchId());
            row.put("userId", s.getUserId());
            row.put("totalScore", s.getTotalScore());
            row.put("status", s.getStatus());
            row.put("submitTime", s.getSubmitTime());
            ExamBatch batch = examBatchMapper.selectById(s.getBatchId());
            if (batch != null) {
                row.put("batchTitle", batch.getTitle());
                ExamPaper p = examPaperMapper.selectById(batch.getPaperId());
                row.put("paperTitle", p == null ? "" : p.getTitle());
                row.put("paperId", batch.getPaperId());
                row.put("passScore", p == null ? 0 : p.getPassScore());
                row.put("totalScoreLimit", p == null ? 0 : p.getTotalScore());
            }
            rows.add(row);
        }
        result.put("code", 200);
        result.put("data", rows);
        return result;
    }

    // 某次作答的题目明细 (供"人工阅卷"/"我的成绩-查看详情")
    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam("batchId") Long batchId,
                                      @RequestParam("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<ExamAnswer> answers = examAnswerMapper.selectList(new QueryWrapper<ExamAnswer>()
                .eq("batch_id", batchId).eq("user_id", userId).orderByAsc("question_id"));
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal objectiveScore = BigDecimal.ZERO;
        BigDecimal paperTotal = BigDecimal.ZERO;
        for (ExamAnswer a : answers) {
            QuestionBank q = questionBankMapper.selectById(a.getQuestionId());
            Map<String, Object> row = new HashMap<>();
            row.put("questionId", a.getQuestionId());
            row.put("questionType", a.getQuestionType());
            row.put("questionContent", q == null ? "" : q.getContent());
            row.put("userAnswer", a.getUserAnswer());
            row.put("referenceAnswer", q == null ? "" : q.getAnswer());
            row.put("analysis", q == null ? "" : q.getAnalysis());
            row.put("correctFlag", a.getCorrectFlag());
            row.put("gotScore", a.getGotScore() == null ? BigDecimal.ZERO : a.getGotScore());
            row.put("fullScore", q == null ? BigDecimal.ZERO : q.getScore());
            // 客观题得分合计(状态判定用)
            if (a.getQuestionType() != null && a.getQuestionType() <= 4 && a.getCorrectFlag() != null && a.getCorrectFlag() == 1) {
                objectiveScore = objectiveScore.add(row.get("gotScore") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(row.get("gotScore"))));
            }
            paperTotal = paperTotal.add(q == null ? BigDecimal.ZERO : q.getScore());
            rows.add(row);
        }
        result.put("code", 200);
        Map<String, Object> data = new HashMap<>();
        data.put("answers", rows);
        data.put("objectiveScore", objectiveScore);
        data.put("paperTotal", paperTotal);
        result.put("data", data);
        return result;
    }
}
