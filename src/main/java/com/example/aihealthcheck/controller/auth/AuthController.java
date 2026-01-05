package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.entity.User;
import com.example.aihealthcheck.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    private static final String SESSION_USER_ID = "userId";
    private static final String SESSION_ACCOUNT = "account";
    private static final String SESSION_REAL_NAME = "realName";
    private static final String SESSION_GENDER = "gender";
    private static final String SESSION_AGE = "age";
    private static final String SESSION_CATEGORY = "category";
    private static final String SESSION_PATIENT_ID = "patientId";
    private static final String SESSION_PATIENT_CODE = "patientCode";

    /**
     * 生成问候语 - 提取姓氏 + 用户类型
     */
    private String generateGreeting(String realName, String userType) {
        if (realName == null || realName.trim().isEmpty()) {
            return "用户";
        }

        // 提取姓氏（第一个字符）
        String lastName = realName.substring(0, 1);

        // 根据用户类型生成问候语
        if ("doctor".equals(userType)) {
            return lastName + "医生";
        } else {
            return lastName + "客户";
        }
    }

    /**
     * 用户登录（患者登录） - 移除MD5，使用明文
     */
@PostMapping("/user/login")
public Map<String, Object> userLogin(@RequestBody Map<String, String> loginData, HttpSession session) {
    String account = loginData.get("account");
    String password = loginData.get("password");

    Map<String, Object> result = new HashMap<>();

    try {
        // 修复：重新加上category限制，只允许患者登录
        String sql = "SELECT u.user_id, u.account, u.real_name, u.gender, u.age, u.category, " +
                    "p.patient_id, p.patient_code " +
                    "FROM users u " +
                    "LEFT JOIN patients p ON u.user_id = p.user_id " +
                    "WHERE u.account = ? AND u.password = ? AND u.category = '患者'"; // 重新加上category限制

        Map<String, Object> user = jdbcTemplate.queryForMap(sql, account, password);

        if (user != null) {
            String realName = (String) user.get("real_name");
            String greeting = generateGreeting(realName, "patient");

            session.setAttribute(SESSION_USER_ID, user.get("user_id"));
            session.setAttribute(SESSION_ACCOUNT, user.get("account"));
            session.setAttribute(SESSION_REAL_NAME, user.get("real_name"));
            session.setAttribute(SESSION_GENDER, user.get("gender"));
            session.setAttribute(SESSION_AGE, user.get("age"));
            session.setAttribute(SESSION_CATEGORY, "患者");
            session.setAttribute(SESSION_PATIENT_ID, user.get("patient_id"));
            session.setAttribute(SESSION_PATIENT_CODE, user.get("patient_code"));

            result.put("success", true);
            result.put("message", "登录成功");
            result.put("user", user);
            result.put("userType", "patient");  // 固定返回patient类型
            result.put("greeting", greeting);
            result.put("realName", realName);
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误，或非患者账号");
        }
    } catch (Exception e) {
        result.put("success", false);
        result.put("message", "登录失败: " + e.getMessage());
        e.printStackTrace();
    }

    return result;
}
    /**
     * 医生登录 - 移除MD5，使用明文
     */
@PostMapping("/doctor/login")
public Map<String, Object> doctorLogin(@RequestBody Map<String, String> loginData, HttpSession session) {
    String account = loginData.get("account");
    String password = loginData.get("password");
    // String department = loginData.get("department"); // 移除科室参数

    Map<String, Object> result = new HashMap<>();

    try {
        // 验证医生用户 - 使用明文密码，移除科室关联查询
        String sql = "SELECT u.user_id, u.account, u.real_name, u.gender, u.age, u.category " +
                    "FROM users u " +
                    "WHERE u.account = ? AND u.password = ? AND u.category = '医生'";

        Map<String, Object> user = jdbcTemplate.queryForMap(sql, account, password);

        if (user != null) {
            // 生成问候语
            String realName = (String) user.get("real_name");
            String greeting = generateGreeting(realName, "doctor");

            session.setAttribute(SESSION_USER_ID, user.get("user_id"));
            session.setAttribute(SESSION_ACCOUNT, user.get("account"));
            session.setAttribute(SESSION_REAL_NAME, user.get("real_name"));
            session.setAttribute(SESSION_GENDER, user.get("gender"));
            session.setAttribute(SESSION_AGE, user.get("age"));
            session.setAttribute(SESSION_CATEGORY, "医生");
            session.removeAttribute(SESSION_PATIENT_ID);
            session.removeAttribute(SESSION_PATIENT_CODE);

            result.put("success", true);
            result.put("message", "医生登录成功");
            result.put("user", user);
            result.put("userType", "doctor");
            result.put("greeting", greeting);
            result.put("realName", realName);
        } else {
            result.put("success", false);
            result.put("message", "工号或密码错误，或非医生账号");
        }
    } catch (Exception e) {
        result.put("success", false);
        result.put("message", "登录失败: " + e.getMessage());
        e.printStackTrace();
    }

    return result;
}

    /**
     * 使用JPA进行登录验证
     */
    @PostMapping("/jpa/login")
    public Map<String, Object> jpaLogin(@RequestBody Map<String, String> loginData, HttpSession session) {
        String account = loginData.get("account");
        String password = loginData.get("password");

        Map<String, Object> result = new HashMap<>();

        try {
            // 使用UserService进行验证（明文验证）
            java.util.Optional<User> userOpt = userService.login(account, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // 构建返回数据
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("user_id", user.getUserId());
                userInfo.put("account", user.getAccount());
                userInfo.put("real_name", user.getRealName());
                userInfo.put("gender", user.getGender());
                userInfo.put("age", user.getAge());
                userInfo.put("category", user.getCategory());

                // 根据用户类型查询额外信息
                if ("医生".equals(user.getCategory())) {
                    String doctorSql = "SELECT d.doctor_id, d.doctor_code, d.dept_id, d.level " +
                                      "FROM doctors d WHERE d.user_id = ?";
                    try {
                        Map<String, Object> doctorInfo = jdbcTemplate.queryForMap(doctorSql, user.getUserId());
                        userInfo.putAll(doctorInfo);
                    } catch (Exception e) {
                        // 没有医生信息，继续
                    }
                } else if ("患者".equals(user.getCategory())) {
                    String patientSql = "SELECT p.patient_id, p.patient_code " +
                                       "FROM patients p WHERE p.user_id = ?";
                    try {
                        Map<String, Object> patientInfo = jdbcTemplate.queryForMap(patientSql, user.getUserId());
                        userInfo.putAll(patientInfo);
                    } catch (Exception e) {
                        // 没有患者信息，继续
                    }
                }

                // 生成问候语
                String greeting = generateGreeting(user.getRealName(),
                    "医生".equals(user.getCategory()) ? "doctor" : "patient");

                session.setAttribute(SESSION_USER_ID, user.getUserId());
                session.setAttribute(SESSION_ACCOUNT, user.getAccount());
                session.setAttribute(SESSION_REAL_NAME, user.getRealName());
                session.setAttribute(SESSION_GENDER, user.getGender());
                session.setAttribute(SESSION_AGE, user.getAge());
                session.setAttribute(SESSION_CATEGORY, user.getCategory());

                if ("患者".equals(user.getCategory())) {
                    Object pid = userInfo.get("patient_id");
                    Object pcode = userInfo.get("patient_code");
                    if (pid != null) session.setAttribute(SESSION_PATIENT_ID, pid);
                    if (pcode != null) session.setAttribute(SESSION_PATIENT_CODE, pcode);
                } else {
                    session.removeAttribute(SESSION_PATIENT_ID);
                    session.removeAttribute(SESSION_PATIENT_CODE);
                }

                result.put("success", true);
                result.put("message", "登录成功");
                result.put("user", userInfo);
                result.put("userType", "医生".equals(user.getCategory()) ? "doctor" : "patient");
                result.put("greeting", greeting);
                result.put("realName", user.getRealName());
            } else {
                result.put("success", false);
                result.put("message", "用户名或密码错误");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        Map<String, Object> user = new HashMap<>();
        user.put("user_id", userId);
        user.put("account", session.getAttribute(SESSION_ACCOUNT));
        user.put("real_name", session.getAttribute(SESSION_REAL_NAME));
        user.put("gender", session.getAttribute(SESSION_GENDER));
        user.put("age", session.getAttribute(SESSION_AGE));
        user.put("category", session.getAttribute(SESSION_CATEGORY));
        user.put("patient_id", session.getAttribute(SESSION_PATIENT_ID));
        user.put("patient_code", session.getAttribute(SESSION_PATIENT_CODE));
        result.put("success", true);
        result.put("user", user);
        return result;
    }

    /**
     * 用户注册 - 移除MD5，存储明文
     */
@PostMapping("/user/register")
public Map<String, Object> userRegister(@RequestBody Map<String, Object> registerData) {
    Map<String, Object> result = new HashMap<>();

    try {
        // 检查账号是否已存在
        String checkSql = "SELECT COUNT(*) FROM users WHERE account = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
            registerData.get("account"));

        if (count != null && count > 0) {
            result.put("success", false);
            result.put("message", "该账号已被注册");
            return result;
        }

        // 修复：正确处理年龄字段（String转Integer）
        Object ageObj = registerData.get("age");
        Integer age = null;
        if (ageObj != null) {
            if (ageObj instanceof Integer) {
                age = (Integer) ageObj;
            } else if (ageObj instanceof String) {
                try {
                    age = Integer.parseInt((String) ageObj);
                } catch (NumberFormatException e) {
                    age = 25; // 默认值
                }
            } else if (ageObj instanceof Number) {
                age = ((Number) ageObj).intValue();
            }
        }

        // 设置默认年龄
        if (age == null) {
            age = 25; // 默认年龄
        }

        // 插入用户数据 - 直接存储明文密码
        String userSql = "INSERT INTO users (account, password, real_name, gender, age, category) " +
                       "VALUES (?, ?, ?, ?, ?, '患者')";

        int rowsAffected = jdbcTemplate.update(userSql,
            registerData.get("account"),
            registerData.get("password"), // 明文
            registerData.get("realName"),
            registerData.get("gender"),
            age); // 使用转换后的Integer

        if (rowsAffected > 0) {
            // 获取刚插入的用户ID
            String userIdSql = "SELECT LAST_INSERT_ID()";
            Long userId = jdbcTemplate.queryForObject(userIdSql, Long.class);

            // 插入患者数据
            String patientCode = "PAT" + System.currentTimeMillis();
            Object realNameObj = registerData.get("realName");
            String realName = (realNameObj != null) ? realNameObj.toString() : "用户";

            String patientSql = "INSERT INTO patients (user_id, patient_code, real_name) " +
                              "VALUES (?, ?, ?)";

            jdbcTemplate.update(patientSql, userId, patientCode, realName);

            result.put("success", true);
            result.put("message", "注册成功");
            result.put("userId", userId);
        } else {
            result.put("success", false);
            result.put("message", "注册失败");
        }

    } catch (Exception e) {
        result.put("success", false);
        result.put("message", "注册失败: " + e.getMessage());
        e.printStackTrace();
    }

    return result;
}
    /**
     * 医生注册
     */
    @PostMapping("/doctor/register")
    public Map<String, Object> doctorRegister(@RequestBody Map<String, Object> registerData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查账号是否已存在
            String checkSql = "SELECT COUNT(*) FROM users WHERE account = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                registerData.get("account"));

            if (count != null && count > 0) {
                result.put("success", false);
                result.put("message", "该账号已被注册");
                return result;
            }

            // 插入用户数据 - 直接存储明文密码
            String userSql = "INSERT INTO users (account, password, real_name, gender, age, category) " +
                           "VALUES (?, ?, ?, ?, ?, '医生')";

            int rowsAffected = jdbcTemplate.update(userSql,
                registerData.get("account"),
                registerData.get("password"), // 明文
                registerData.get("realName"),
                registerData.get("gender"),
                registerData.get("age"));

            if (rowsAffected > 0) {
                // 获取刚插入的用户ID
                String userIdSql = "SELECT LAST_INSERT_ID()";
                Long userId = jdbcTemplate.queryForObject(userIdSql, Long.class);

                // 插入医生数据
                String doctorCode = "DOC" + System.currentTimeMillis();
                Object deptId = registerData.get("deptId");
                Object level = registerData.get("level");

                String doctorSql = "INSERT INTO doctors (user_id, doctor_code, dept_id, level) " +
                                 "VALUES (?, ?, ?, ?)";

                jdbcTemplate.update(doctorSql, userId, doctorCode,
                    deptId != null ? deptId : 1, // 默认科室ID
                    level != null ? level : "医师" // 默认级别
                );

                result.put("success", true);
                result.put("message", "医生注册成功");
                result.put("userId", userId);
            } else {
                result.put("success", false);
                result.put("message", "注册失败");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "注册失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取所有科室
     */
    @GetMapping("/departments")
    public Map<String, Object> getDepartments() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT dept_id, dept_code, dept_name, dept_type FROM departments ORDER BY dept_name";
            var departments = jdbcTemplate.queryForList(sql);

            result.put("success", true);
            result.put("departments", departments);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取科室失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查账号是否存在
     */
    @GetMapping("/check-account/{account}")
    public Map<String, Object> checkAccount(@PathVariable String account) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT COUNT(*) as count FROM users WHERE account = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, account);

            result.put("success", true);
            result.put("exists", count != null && count > 0);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 登出
     */
    @GetMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            session.invalidate();
        } catch (Exception ignored) {}
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> resetData) {
        Map<String, Object> result = new HashMap<>();

        try {
            String account = (String) resetData.get("account");
            String newPassword = (String) resetData.get("newPassword");

            String sql = "UPDATE users SET password = ? WHERE account = ?";
            int rowsAffected = jdbcTemplate.update(sql, newPassword, account);

            if (rowsAffected > 0) {
                result.put("success", true);
                result.put("message", "密码重置成功");
            } else {
                result.put("success", false);
                result.put("message", "账号不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "密码重置失败: " + e.getMessage());
        }

        return result;
    }
}



