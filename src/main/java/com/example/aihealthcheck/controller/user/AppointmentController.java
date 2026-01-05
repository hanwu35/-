package com.example.aihealthcheck.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAppointment(@RequestBody Map<String, String> payload, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. 获取当前用户
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            response.put("success", false);
            response.put("message", "请先登录");
            return ResponseEntity.status(401).body(response);
        }
        Integer userId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());

        String doctorCode = payload.get("doctorCode");
        if (doctorCode == null) {
            response.put("success", false);
            response.put("message", "医生代码不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 2. 获取 patient_id
            Integer patientId;
            try {
                patientId = jdbcTemplate.queryForObject("SELECT patient_id FROM patients WHERE user_id = ?", Integer.class, userId);
            } catch (Exception e) {
                // 如果没有患者记录，尝试创建? 或者返回错误
                // 这里假设必须有患者记录。如果没有，可能是注册流程问题。
                // 尝试自动创建一个? 
                // 为了简化，尝试插入
                 String realName = jdbcTemplate.queryForObject("SELECT real_name FROM users WHERE user_id = ?", String.class, userId);
                 String patientCode = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                 jdbcTemplate.update("INSERT INTO patients (user_id, patient_code, real_name) VALUES (?, ?, ?)", userId, patientCode, realName);
                 patientId = jdbcTemplate.queryForObject("SELECT patient_id FROM patients WHERE user_id = ?", Integer.class, userId);
            }

            // 3. 获取 doctor_id 和 fee
            Map<String, Object> doctor = jdbcTemplate.queryForMap("SELECT doctor_id, normal_fee, real_name, dept_id FROM doctors WHERE doctor_code = ?", doctorCode);
            Integer doctorId = (Integer) doctor.get("doctor_id");
            java.math.BigDecimal fee = (java.math.BigDecimal) doctor.get("normal_fee");
            String doctorName = (String) doctor.get("real_name");

            // 4. 获取今日排班
            LocalDate today = LocalDate.now();
            String weekDay = getChineseWeekDay(today);
            
            // 查找该医生的今日排班
            // 优先找一个未满的排班，或者随便找一个
            String sqlSchedule = "SELECT schedule_id, time_slot FROM schedules WHERE doctor_id = ? AND work_day = ? LIMIT 1";
            List<Map<String, Object>> schedules = jdbcTemplate.queryForList(sqlSchedule, doctorId, weekDay);
            
            if (schedules.isEmpty()) {
                response.put("success", false);
                response.put("message", "该医生今日(" + weekDay + ")没有排班");
                return ResponseEntity.badRequest().body(response);
            }
            
            Integer scheduleId = (Integer) schedules.get(0).get("schedule_id");
            String timeSlot = (String) schedules.get(0).get("time_slot");

            // 5. 创建预约
            // 检查该时段是否已有预约（即使是不同医生，同一时段也不能重复，除非我们允许覆盖）
            // 根据报错 uk_patient_time，应该是同一天同一时段只能有一个预约
            // 我们可以检查是否有同一天的预约，如果有，就更新它（视为更换医生）
            
            String checkSql = "SELECT appointment_id, doctor_id FROM appointments WHERE patient_id = ? AND appointment_date = ? AND time_slot = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, patientId, today, timeSlot);
            
            if (!existing.isEmpty()) {
                // 已存在预约
                Integer existingDoctorId = (Integer) existing.get(0).get("doctor_id");
                
                if (existingDoctorId.equals(doctorId)) {
                    // 同一医生，直接返回成功
                    response.put("success", true);
                    response.put("message", "您今日已预约过该医生");
                    response.put("doctorName", doctorName);
                    return ResponseEntity.ok(response);
                } else {
                    // 不同医生，更新为新医生（覆盖）
                    String updateSql = "UPDATE appointments SET doctor_id = ?, schedule_id = ?, fee = ?, status = 'confirmed' WHERE patient_id = ? AND appointment_date = ? AND time_slot = ?";
                    jdbcTemplate.update(updateSql, doctorId, scheduleId, fee, patientId, today, timeSlot);
                    
                    response.put("success", true);
                    response.put("message", "预约已更新为新医生");
                    response.put("doctorName", doctorName);
                    return ResponseEntity.ok(response);
                }
            } else {
                // 不存在，插入新记录
                String insertSql = "INSERT INTO appointments (patient_id, doctor_id, schedule_id, appointment_date, time_slot, status, fee, is_urgent) " +
                                   "VALUES (?, ?, ?, ?, ?, 'confirmed', ?, 0)";
                
                jdbcTemplate.update(insertSql, patientId, doctorId, scheduleId, today, timeSlot, fee);

                response.put("success", true);
                response.put("message", "预约成功");
                response.put("doctorName", doctorName);
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "预约失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/last-successful")
    public ResponseEntity<Map<String, Object>> getLastSuccessfulAppointment(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }
        Integer userId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());

        try {
            // 查找最后一次成功的预约
            String sql = "SELECT d.real_name, d.doctor_code, dept.dept_name " +
                         "FROM appointments a " +
                         "JOIN patients p ON a.patient_id = p.patient_id " +
                         "JOIN doctors d ON a.doctor_id = d.doctor_id " +
                         "JOIN departments dept ON d.dept_id = dept.dept_id " +
                         "WHERE p.user_id = ? AND a.status IN ('confirmed', 'completed') " +
                         "ORDER BY a.appointment_date DESC, a.appointment_id DESC LIMIT 1";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            
            if (results.isEmpty()) {
                response.put("success", false);
                response.put("message", "无预约记录");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> appointment = results.get(0);
            response.put("success", true);
            response.put("doctorName", appointment.get("real_name"));
            response.put("deptName", appointment.get("dept_name"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String getChineseWeekDay(LocalDate date) {
        int day = date.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)
        switch (day) {
            case 1: return "周一";
            case 2: return "周二";
            case 3: return "周三";
            case 4: return "周四";
            case 5: return "周五";
            case 6: return "周六";
            case 7: return "周日";
            default: return "";
        }
    }
}
