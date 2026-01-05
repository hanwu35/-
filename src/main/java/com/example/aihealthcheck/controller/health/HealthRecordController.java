package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.entity.health.record.HealthRecord;
import com.example.aihealthcheck.entity.User;
import com.example.aihealthcheck.service.HealthRecordService;
import com.example.aihealthcheck.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    @Autowired
    private HealthRecordService healthRecordService;

    @Autowired
    private UserService userService;

    // ============ 获取所有健康记录 ============
    @GetMapping
    public ResponseEntity<List<HealthRecord>> getAllHealthRecords() {
        try {
            List<HealthRecord> records = healthRecordService.findAll();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ 通过ID获取健康记录 ============
    @GetMapping("/{id}")
    public ResponseEntity<HealthRecord> getHealthRecordById(@PathVariable Long id) {
        try {
            Optional<HealthRecord> record = healthRecordService.findById(id);
            return record.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ 通过用户ID获取健康记录 ============
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HealthRecord>> getHealthRecordsByUser(@PathVariable Integer userId) {
        try {
            // 验证用户是否存在
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<HealthRecord> records = healthRecordService.findByUserId(userId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ 创建健康记录 ============
    @PostMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> createHealthRecord(
            @PathVariable Integer userId,
            @RequestBody HealthRecord healthRecord) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证用户是否存在
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }

            // 设置用户关联
            healthRecord.setUser(user.get());

            // 保存健康记录
            HealthRecord savedRecord = healthRecordService.save(healthRecord);

            response.put("success", true);
            response.put("message", "健康记录创建成功");
            response.put("record", savedRecord);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============ 通过账号获取健康记录 ============
    @GetMapping("/account/{account}")
    public ResponseEntity<List<HealthRecord>> getHealthRecordsByAccount(@PathVariable String account) {
        try {
            List<HealthRecord> records = healthRecordService.findByUserAccount(account);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ 按日期范围查询 ============
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<HealthRecord>> getHealthRecordsByDateRange(
            @PathVariable Integer userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            List<HealthRecord> records = healthRecordService.findByUserIdAndDateRange(userId, start, end);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ 更新健康记录 ============
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateHealthRecord(
            @PathVariable Long id,
            @RequestBody HealthRecord healthRecordDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<HealthRecord> updatedRecord = healthRecordService.updateHealthRecord(id, healthRecordDetails);

            if (updatedRecord.isPresent()) {
                response.put("success", true);
                response.put("message", "健康记录更新成功");
                response.put("record", updatedRecord.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "健康记录不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============ 删除健康记录 ============
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHealthRecord(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (healthRecordService.findById(id).isPresent()) {
                healthRecordService.deleteById(id);
                response.put("success", true);
                response.put("message", "健康记录删除成功");
                return ResponseEntity.ok(response);
            }

            response.put("success", false);
            response.put("message", "健康记录不存在");
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============ 统计接口 ============
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getHealthRecordStats(@PathVariable Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 验证用户是否存在
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                stats.put("error", "用户不存在");
                return ResponseEntity.notFound().build();
            }

            long recordCount = healthRecordService.countByUserId(userId);
            List<HealthRecord> recentRecords = healthRecordService.getRecentHealthRecords(userId, 5);

            stats.put("userId", userId);
            stats.put("recordCount", recordCount);
            stats.put("hasRecords", recordCount > 0);
            stats.put("recentRecords", recentRecords);
            stats.put("userName", user.get().getRealName());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(stats);
        }
    }
}