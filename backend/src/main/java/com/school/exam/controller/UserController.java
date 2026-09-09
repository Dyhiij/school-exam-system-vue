package com.school.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.exam.entity.UserInfo;
import com.school.exam.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody UserInfo loginUser) {
        Map<String, Object> res = new HashMap<>();
        UserInfo user = userInfoMapper.selectOne(new QueryWrapper<UserInfo>()
                .eq("username", loginUser.getUsername()));
        
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        
        if (user != null && (loginUser.getPassword().equals(user.getPassword()) || encoder.matches(loginUser.getPassword(), user.getPassword()))) {
            res.put("code", 200);
            res.put("data", user);
            res.put("msg", "登录成功");
        } else {
            res.put("code", 401);
            res.put("msg", "用户名或密码错误");
        }
        return res;
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", userInfoMapper.selectList(null));
        return res;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody UserInfo user) {
        Map<String, Object> res = new HashMap<>();
        if (user.getId() == null) {
            userInfoMapper.insert(user);
        } else {
            userInfoMapper.updateById(user);
        }
        res.put("code", 200);
        res.put("msg", "保存成功");
        return res;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        userInfoMapper.deleteById(id);
        res.put("code", 200);
        res.put("msg", "删除成功");
        return res;
    }
}
