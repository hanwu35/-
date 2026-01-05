package com.example.aihealthcheck.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 修改这里：根路径重定向到登录页
    @GetMapping("/")
    public String home() {
        return "redirect:/login";  // 改为重定向到登录页
    }

    // 可以添加这个方法来直接访问登录页
    @GetMapping("/login")
    public String login() {
        return "login/index";  // 指向登录页面
    }

    // 添加用户页面路由
    @GetMapping("/user")
    public String user() {
        return "user/index";  // 指向用户页面
    }

    // 添加医生页面路由
    @GetMapping("/doctor")
    public String doctor() {
        return "doctor/index";  // 指向医生页面
    }

    @GetMapping("/api/info")
    public String apiInfo() {
        return "欢迎使用 AI Health Check 应用！<br><br>" +
               "用户管理 API：<br>" +
               "- GET /api/users - 获取所有用户<br>" +
               "- POST /api/users - 创建用户<br><br>" +
               "模型文件管理 API：<br>" +
               "- GET /api/model-files - 获取所有模型<br>" +
               "- POST /api/model-files - 创建模型记录<br>" +
               "- GET /api/model-files/active - 获取活跃模型<br>" +
               "- PUT /api/model-files/{id}/activate - 激活模型<br>" +
               "- GET /api/model-files/{id}/download - 下载模型文件";
    }
}