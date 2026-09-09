package com.school.exam.controller;

import com.school.exam.entity.ExamPaper;
import com.school.exam.mapper.ExamPaperMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/paper")
public class PaperController {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", examPaperMapper.selectList(null));
        return res;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody ExamPaper paper) {
        Map<String, Object> res = new HashMap<>();
        if (paper.getId() == null) {
            examPaperMapper.insert(paper);
        } else {
            examPaperMapper.updateById(paper);
        }
        res.put("code", 200);
        res.put("msg", "保存成功");
        return res;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        examPaperMapper.deleteById(id);
        res.put("code", 200);
        res.put("msg", "删除成功");
        return res;
    }
}
