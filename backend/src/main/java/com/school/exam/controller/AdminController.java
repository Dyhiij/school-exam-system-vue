package com.school.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.exam.entity.*;
import com.school.exam.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private QuestionBankMapper questionBankMapper;
    @Autowired
    private ExamPaperMapper examPaperMapper;
    @Autowired
    private ExamBatchMapper examBatchMapper;
    @Autowired
    private ExamScoreMapper examScoreMapper;
    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;
    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    // Users
    @GetMapping("/users")
    public Map<String, Object> getUsers() {
        return success(userInfoMapper.selectList(null));
    }

    @PostMapping("/user")
    public Map<String, Object> saveUser(@RequestBody UserInfo user) {
        if (user.getPassword() != null && user.getPassword().length() < 20
                && !user.getPassword().startsWith("$2")) {
            // 演示库存明文; 新密码不做 BCrypt(与现有登录逻辑保持一致, 避免改库兼容问题)
        }
        if (user.getId() == null) {
            userInfoMapper.insert(user);
        } else {
            userInfoMapper.updateById(user);
        }
        return success("ok");
    }

    @DeleteMapping("/user/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        userInfoMapper.deleteById(id);
        return success("ok");
    }

    // Questions
    @GetMapping("/questions")
    public Map<String, Object> getQuestions() {
        return success(questionBankMapper.selectList(null));
    }

    @PostMapping("/question")
    public Map<String, Object> saveQuestion(@RequestBody QuestionBank question) {
        if (question.getId() == null) {
            questionBankMapper.insert(question);
        } else {
            questionBankMapper.updateById(question);
        }
        return success("ok");
    }

    @DeleteMapping("/question/{id}")
    public Map<String, Object> deleteQuestion(@PathVariable Long id) {
        questionBankMapper.deleteById(id);
        return success("ok");
    }

    // Papers
    @GetMapping("/papers")
    public Map<String, Object> getPapers() {
        return success(examPaperMapper.selectList(null));
    }

    @PostMapping("/paper")
    public Map<String, Object> savePaper(@RequestBody ExamPaper paper) {
        if (paper.getId() == null) {
            examPaperMapper.insert(paper);
        } else {
            examPaperMapper.updateById(paper);
        }
        return success("ok");
    }

    @DeleteMapping("/paper/{id}")
    public Map<String, Object> deletePaper(@PathVariable Long id) {
        examPaperMapper.deleteById(id);
        // 级联清理试卷-题目关联, 避免孤儿数据
        examPaperQuestionMapper.delete(new QueryWrapper<ExamPaperQuestion>().eq("paper_id", id));
        return success("ok");
    }

    // Batches
    @GetMapping("/batches")
    public Map<String, Object> getBatches() {
        return success(examBatchMapper.selectList(null));
    }

    @PostMapping("/batch")
    public Map<String, Object> saveBatch(@RequestBody ExamBatch batch) {
        if (batch.getId() == null) {
            examBatchMapper.insert(batch);
        } else {
            examBatchMapper.updateById(batch);
        }
        return success("ok");
    }

    // ---- 组卷: 试卷-题目配置 (缺陷修复: 固定组卷此前无题目来源) ----

    /** 试卷已绑定题目ID列表 */
    @GetMapping("/paper/{id}/questions")
    public Map<String, Object> paperQuestions(@PathVariable Long id) {
        List<ExamPaperQuestion> rels = examPaperQuestionMapper.selectList(
                new QueryWrapper<ExamPaperQuestion>().eq("paper_id", id));
        List<Long> ids = new ArrayList<>();
        for (ExamPaperQuestion r : rels) ids.add(r.getQuestionId());
        Map<String, Object> data = new HashMap<>();
        data.put("boundQuestionIds", ids);
        data.put("questionCount", ids.size());
        return success(data);
    }

    /** 保存试卷绑定题目: body {questionIds: [1,2,3]} */
    @PostMapping("/paper/{id}/questions")
    public Map<String, Object> savePaperQuestions(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body) {
        examPaperQuestionMapper.delete(new QueryWrapper<ExamPaperQuestion>().eq("paper_id", id));
        @SuppressWarnings("unchecked")
        List<Integer> qids = (List<Integer>) body.getOrDefault("questionIds", new ArrayList<>());
        for (Integer qid : qids) {
            ExamPaperQuestion r = new ExamPaperQuestion();
            r.setPaperId(id);
            r.setQuestionId(qid.longValue());
            r.setCreateTime(LocalDateTime.now());
            examPaperQuestionMapper.insert(r);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "组卷配置已保存, 共 " + qids.size() + " 题");
        res.put("data", qids.size());
        return res;
    }

    // ---- 人工阅卷 (缺陷修复: 简答题此前答案被丢弃) ----

    /** 待人工阅卷列表: 含主观题且尚未阅卷的交卷记录 */
    @GetMapping("/review/list")
    public Map<String, Object> reviewList() {
        // 1. 找出所有含主观题(简答)的作答明细批次+考生
        List<ExamAnswer> subjective = examAnswerMapper.selectList(
                new QueryWrapper<ExamAnswer>().eq("question_type", 5).eq("correct_flag", 0));
        Set<String> keys = new LinkedHashSet<>();
        for (ExamAnswer a : subjective) keys.add(a.getBatchId() + "_" + a.getUserId());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String k : keys) {
            String[] parts = k.split("_");
            Long batchId = Long.valueOf(parts[0]);
            Long userId = Long.valueOf(parts[1]);
            ExamScore score = examScoreMapper.selectOne(new QueryWrapper<ExamScore>()
                    .eq("batch_id", batchId).eq("user_id", userId));
            if (score == null || score.getStatus() == null || score.getStatus() >= 2) continue;
            ExamBatch batch = examBatchMapper.selectById(batchId);
            ExamPaper paper = batch == null ? null : examPaperMapper.selectById(batch.getPaperId());
            UserInfo user = userInfoMapper.selectById(userId);
            Map<String, Object> row = new HashMap<>();
            row.put("batchId", batchId);
            row.put("userId", userId);
            row.put("batchTitle", batch == null ? "" : batch.getTitle());
            row.put("paperTitle", paper == null ? "" : paper.getTitle());
            row.put("studentName", user == null ? "" : (user.getRealName() != null ? user.getRealName() : user.getUsername()));
            row.put("objectiveScore", score.getTotalScore());
            row.put("submitTime", score.getSubmitTime());
            rows.add(row);
        }
        return success(rows);
    }

    /** 人工阅卷打分: {batchId, userId, items:[{questionId, score}]} → 重算总分并置已阅 */
    @PostMapping("/review/grade")
    public Map<String, Object> grade(@RequestBody Map<String, Object> body) {
        Long batchId = Long.valueOf(String.valueOf(body.get("batchId")));
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.getOrDefault("items",
                (List<Map<String, Object>>) body.getOrDefault("scores", new ArrayList<>()));

        BigDecimal objectiveScore = BigDecimal.ZERO;
        BigDecimal subjectiveTotal = BigDecimal.ZERO;
        ExamScore score = examScoreMapper.selectOne(new QueryWrapper<ExamScore>()
                .eq("batch_id", batchId).eq("user_id", userId));

        for (Map<String, Object> it : items) {
            Long qid = Long.valueOf(String.valueOf(it.get("questionId")));
            BigDecimal s = new BigDecimal(String.valueOf(it.getOrDefault("score", 0)));
            ExamAnswer ans = examAnswerMapper.selectOne(new QueryWrapper<ExamAnswer>()
                    .eq("batch_id", batchId).eq("user_id", userId).eq("question_id", qid));
            if (ans != null) {
                ans.setCorrectFlag(1);
                ans.setGotScore(s);
                examAnswerMapper.updateById(ans);
            }
            QuestionBank q = questionBankMapper.selectById(qid);
            if (q != null && q.getType() != null && q.getType() <= 4) {
                objectiveScore = objectiveScore.add(s);
            } else {
                subjectiveTotal = subjectiveTotal.add(s);
            }
        }
        // 客观题分数不变, 从明细里累计(防止只传主观题时把客观清空)
        List<ExamAnswer> all = examAnswerMapper.selectList(new QueryWrapper<ExamAnswer>()
                .eq("batch_id", batchId).eq("user_id", userId));
        BigDecimal obj = BigDecimal.ZERO;
        for (ExamAnswer a : all) {
            if (a.getQuestionType() != null && a.getQuestionType() <= 4) {
                obj = obj.add(a.getGotScore() == null ? BigDecimal.ZERO : a.getGotScore());
            }
        }
        BigDecimal total = obj.add(subjectiveTotal);
        if (score == null) {
            score = new ExamScore();
            score.setBatchId(batchId);
            score.setUserId(userId);
            score.setTotalScore(total);
            score.setStatus(2);
            score.setSubmitTime(LocalDateTime.now());
            examScoreMapper.insert(score);
        } else {
            score.setTotalScore(total);
            score.setStatus(2); // 2已阅卷
            examScoreMapper.updateById(score);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "阅卷完成, 总分: " + total + " 分");
        res.put("totalScore", total);
        return res;
    }

    // Scores (成绩查询: 带考生名/试卷名)
    @GetMapping("/scores")
    public Map<String, Object> getScores() {
        List<ExamScore> scores = examScoreMapper.selectList(
                new QueryWrapper<ExamScore>().orderByDesc("submit_time"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamScore s : scores) {
            Map<String, Object> row = new HashMap<>();
            row.put("scoreId", s.getId());
            row.put("batchId", s.getBatchId());
            row.put("userId", s.getUserId());
            row.put("totalScore", s.getTotalScore());
            row.put("status", s.getStatus());
            row.put("submitTime", s.getSubmitTime());
            UserInfo u = userInfoMapper.selectById(s.getUserId());
            row.put("studentName", u == null ? "" : (u.getRealName() != null ? u.getRealName() : u.getUsername()));
            ExamBatch b = examBatchMapper.selectById(s.getBatchId());
            if (b != null) {
                row.put("batchTitle", b.getTitle());
                ExamPaper p = examPaperMapper.selectById(b.getPaperId());
                row.put("paperTitle", p == null ? "" : p.getTitle());
                row.put("passScore", p == null ? 0 : p.getPassScore());
                row.put("totalScoreLimit", p == null ? 0 : p.getTotalScore());
            }
            rows.add(row);
        }
        return success(rows);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", data);
        return res;
    }
}
