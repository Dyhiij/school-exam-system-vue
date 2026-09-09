package com.school.exam.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class HelloController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "系统运行正常");
        result.put("time", System.currentTimeMillis());
        result.put("version", "1.0.0");
        return result;
    }
}
