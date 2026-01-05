package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.entity.User;
import com.example.aihealthcheck.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ============ 注册接口 ============
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();

        try {
            String account = (String) userData.get("account");
            String password = (String) userData.get("password");
            String realName = (String) userData.get("realName");
            String gender = (String) userData.get("gender");
            Integer age = (Integer) userData.get("age");
            String category = (String) userData.get("category");

            User user = userService.registerUser(account, password, realName, gender, age, category);

            response.put("success", true);
            response.put("message", "注册成功");
            response.put("user", user);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "注册失败");
            return ResponseEntity.badRequest().body(response);
        }
    }

// ============ 登录接口 ============
@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
    String account = credentials.get("account");
    String password = credentials.get("password");
    Map<String, Object> response = new HashMap<>();

    System.out.println("登录请求 - 账号: " + account + ", 密码: " + password);

    try {
        Optional<User> userOpt = userService.login(account, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> userInfo = new HashMap<>();

            // 保持与前端一致的小写+下划线格式
            userInfo.put("user_id", user.getUserId());  // 改为 user_id
            userInfo.put("account", user.getAccount());
            userInfo.put("real_name", user.getRealName());  // 改为 real_name
            userInfo.put("gender", user.getGender());
            userInfo.put("age", user.getAge());
            userInfo.put("category", user.getCategory());

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user", userInfo);
            response.put("userType", user.getCategory().equals("患者") ? "patient" : "doctor");

            System.out.println("登录成功，返回数据: " + response);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            System.out.println("登录失败: 用户名或密码错误");
            return ResponseEntity.ok(response);
        }
    } catch (Exception e) {
        e.printStackTrace();
        response.put("success", false);
        response.put("message", "登录失败: " + e.getMessage());
        return ResponseEntity.ok(response);
    }
}

// ============ 医生登录接口 ============
@PostMapping("/doctor-login")
public ResponseEntity<Map<String, Object>> doctorLogin(@RequestBody Map<String, String> credentials) {
    String account = credentials.get("account");
    String password = credentials.get("password");
    Map<String, Object> response = new HashMap<>();

    System.out.println("医生登录请求 - 账号: " + account);

    try {
        Optional<User> userOpt = userService.login(account, password);

        if (userOpt.isPresent() && userOpt.get().getCategory().equals("医生")) {
            User user = userOpt.get();
            Map<String, Object> userInfo = new HashMap<>();

            // 保持与前端一致的小写+下划线格式
            userInfo.put("user_id", user.getUserId());
            userInfo.put("account", user.getAccount());
            userInfo.put("real_name", user.getRealName());  // 改为 real_name
            userInfo.put("gender", user.getGender());
            userInfo.put("age", user.getAge());
            userInfo.put("category", user.getCategory());

            // 添加科室信息
            userInfo.put("dept_name", "全科");  // 默认科室

            response.put("success", true);
            response.put("message", "医生登录成功");
            response.put("user", userInfo);
            response.put("userType", "doctor");

            System.out.println("医生登录成功，返回数据: " + response);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "医生工号或密码错误，或不是医生账号");
            System.out.println("医生登录失败");
            return ResponseEntity.ok(response);
        }
    } catch (Exception e) {
        e.printStackTrace();
        response.put("success", false);
        response.put("message", "医生登录失败: " + e.getMessage());
        return ResponseEntity.ok(response);
    }
}

    // ============ CRUD 接口 ============
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        try {
            Optional<User> user = userService.getUserById(id);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.ok(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ 查询接口 ============
    @GetMapping("/category/{category}")
    public ResponseEntity<List<User>> getUsersByCategory(@PathVariable String category) {
        try {
            List<User> users = userService.getUsersByCategory(category);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/account/{account}")
    public ResponseEntity<User> getUserByAccount(@PathVariable String account) {
        try {
            Optional<User> user = userService.findByAccount(account);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        try {
            List<User> users = userService.searchUsers(keyword);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("total", userService.count());
            stats.put("doctors", userService.countUsersByCategory("医生"));
            stats.put("patients", userService.countUsersByCategory("患者"));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(stats);
        }
    }
}