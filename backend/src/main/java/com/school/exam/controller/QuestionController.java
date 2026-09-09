package com.school.exam.controller;

import com.school.exam.entity.QuestionBank;
import com.school.exam.mapper.QuestionBankMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", questionBankMapper.selectList(null));
        return res;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody QuestionBank question) {
        Map<String, Object> res = new HashMap<>();
        if (question.getId() == null) {
            questionBankMapper.insert(question);
        } else {
            questionBankMapper.updateById(question);
        }
        res.put("code", 200);
        res.put("msg", "保存成功");
        return res;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        questionBankMapper.deleteById(id);
        res.put("code", 200);
        res.put("msg", "删除成功");
        return res;
    }
}
