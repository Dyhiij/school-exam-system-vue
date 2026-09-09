package com.school.exam.controller;

import com.school.exam.entity.ExamBatch;
import com.school.exam.mapper.ExamBatchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/batch")
public class BatchController {

    @Autowired
    private ExamBatchMapper examBatchMapper;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", examBatchMapper.selectList(null));
        return res;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody ExamBatch batch) {
        Map<String, Object> res = new HashMap<>();
        if (batch.getId() == null) {
            examBatchMapper.insert(batch);
        } else {
            examBatchMapper.updateById(batch);
        }
        res.put("code", 200);
        res.put("msg", "保存成功");
        return res;
    }
}
