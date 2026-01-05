package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.dto.DoctorDTO;
import com.example.aihealthcheck.dto.DoctorQueryDTO;
import com.example.aihealthcheck.dto.PageResultDTO;
import com.example.aihealthcheck.repository.health.examination.blood.HealthBloodItemRepository;
import com.example.aihealthcheck.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private com.example.aihealthcheck.repository.health.examination.blood.HealthBloodItemRepository bloodItemRepository;
    @Autowired
    private com.example.aihealthcheck.repository.health.examination.Urine.HealthUrineItemRepository urineItemRepository;
    @Autowired
    private com.example.aihealthcheck.repository.health.examination.Liver.HealthLiverItemRepository liverItemRepository;
    @Autowired
    private com.example.aihealthcheck.repository.health.examination.Kindey.HealthKidneyItemRepository kidneyItemRepository;

    @Autowired
    private com.example.aihealthcheck.repository.health.examination.blood.HealthBloodRepository bloodRepository;
    @Autowired
    private com.example.aihealthcheck.repository.health.examination.Urine.HealthUrineRepository urineRepository;
    @Autowired
    private com.example.aihealthcheck.repository.health.examination.Liver.HealthLiverRepository liverRepository;
    @Autowired
    private com.example.aihealthcheck.repository.health.examination.Kindey.HealthKidneyRepository kidneyRepository;

    @Autowired
    private com.example.aihealthcheck.service.UserService userService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.example.aihealthcheck.repository.doctor.DoctorAdviceRepository doctorAdviceRepository;

    @Autowired
    private com.example.aihealthcheck.repository.user.AppointmentRepository appointmentRepository;

    @Autowired
    private com.example.aihealthcheck.repository.user.DoctorRepository doctorRepository;

    @GetMapping
    public ResponseEntity<PageResultDTO<DoctorDTO>> getDoctors(
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String doctorName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer pageSize) {

        DoctorQueryDTO query = new DoctorQueryDTO(deptCode, sortBy, page, pageSize);
        query.setDoctorName(doctorName);
        PageResultDTO<DoctorDTO> result = doctorService.getDoctors(query);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{doctorCode}")
    public ResponseEntity<DoctorDTO> getDoctorByCode(@PathVariable String doctorCode) {
        DoctorDTO doctor = doctorService.getDoctorByCode(doctorCode);
        if (doctor != null) {
            return ResponseEntity.ok(doctor);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/id/{doctorId}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Integer doctorId) {
        DoctorDTO doctor = doctorService.getDoctorById(doctorId);
        if (doctor != null) {
            return ResponseEntity.ok(doctor);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/daily-patients")
    public ResponseEntity<List<Map<String, Object>>> getDailyPatients(@RequestParam String date, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            // 如果未登录，暂时保留旧逻辑或者返回空？
            // 考虑到安全，应该返回未授权，但为了兼容旧前端测试，暂时返回空列表
            return ResponseEntity.status(401).build();
        }
        Integer currentUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());

        // 获取当前医生ID
        var doctorOpt = doctorRepository.findByUserId(currentUserId);
        if (doctorOpt.isEmpty()) {
             return ResponseEntity.status(403).build();
        }
        Integer doctorId = doctorOpt.get().getDoctorId();

        java.time.LocalDate d = java.time.LocalDate.parse(date);
        
        // 1. 获取当天的预约
        var appointments = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, d);
        java.util.Set<Integer> patientIds = appointments.stream()
                .map(com.example.aihealthcheck.entity.user.Appointment::getPatientId)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 将 patient_id 转换为 user_id
        java.util.Set<Integer> userIds = new java.util.HashSet<>();
        if (!patientIds.isEmpty()) {
            try {
                String ids = patientIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                String sql = "SELECT user_id FROM patients WHERE patient_id IN (" + ids + ")";
                java.util.List<Integer> uids = jdbcTemplate.queryForList(sql, Integer.class);
                userIds.addAll(uids);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 为了获取检测状态，我们需要查询这些用户在当天的检测记录
        java.time.LocalDateTime startOfDay = d.atStartOfDay();
        java.time.LocalDateTime endOfDay = d.atTime(java.time.LocalTime.MAX);
        
        java.util.Set<Integer> bloodUserIds = new java.util.HashSet<>(bloodItemRepository.findDistinctUserIdsByDate(d));
        bloodUserIds.addAll(bloodItemRepository.findDistinctUserIdsByCreatedAtBetween(startOfDay, endOfDay));

        java.util.Set<Integer> urineUserIds = new java.util.HashSet<>(urineItemRepository.findDistinctUserIdsByDate(d));
        urineUserIds.addAll(urineItemRepository.findDistinctUserIdsByCreatedAtBetween(startOfDay, endOfDay));

        java.util.Set<Integer> liverUserIds = new java.util.HashSet<>(liverItemRepository.findDistinctUserIdsByDate(d));
        liverUserIds.addAll(liverItemRepository.findDistinctUserIdsByCreatedAtBetween(startOfDay, endOfDay));

        java.util.Set<Integer> kidneyUserIds = new java.util.HashSet<>(kidneyItemRepository.findDistinctUserIdsByDate(d));
        kidneyUserIds.addAll(kidneyItemRepository.findDistinctUserIdsByCreatedAtBetween(startOfDay, endOfDay));

        java.util.List<Map<String, Object>> patients = new java.util.ArrayList<>();
        for (Integer uid : userIds) {
            var userOpt = userService.findById(uid);
            if (userOpt.isEmpty()) continue;
            var user = userOpt.get();
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("userId", user.getUserId());
            m.put("name", user.getRealName());
            m.put("gender", user.getGender());
            m.put("age", user.getAge());
            
            boolean hasBlood = bloodUserIds.contains(uid);
            boolean hasUrine = urineUserIds.contains(uid);
            boolean hasLiver = liverUserIds.contains(uid);
            boolean hasKidney = kidneyUserIds.contains(uid);
            
            if (!hasBlood) {
                hasBlood = !bloodItemRepository.findDistinctDatesByUserId(uid).isEmpty();
            }
            if (!hasUrine) {
                hasUrine = !urineItemRepository.findDistinctDatesByUserId(uid).isEmpty();
            }
            if (!hasLiver) {
                hasLiver = !liverItemRepository.findDistinctDatesByUserId(uid).isEmpty();
            }
            if (!hasKidney) {
                hasKidney = !kidneyItemRepository.findDistinctDatesByUserId(uid).isEmpty();
            }
            
            m.put("tests", java.util.stream.Stream.of(
                    hasBlood ? "血检" : null,
                    hasUrine ? "尿检" : null,
                    hasLiver ? "肝功能" : null,
                    hasKidney ? "肾功能" : null
            ).filter(java.util.Objects::nonNull).toList());

            var adviceOpt = doctorAdviceRepository.findTopByPatientUserIdOrderByUpdatedAtDesc(uid);
            String adviceStatus = adviceOpt.map(com.example.aihealthcheck.entity.doctor.DoctorAdvice::getStatus).orElse("PENDING");
            m.put("adviceStatus", adviceStatus);
            
            String status;
            String action;
            if (!hasBlood && !hasUrine && !hasLiver && !hasKidney) {
                status = "待检查";
                action = "呼叫检查";
            } else if (!"COMPLETED".equalsIgnoreCase(adviceStatus)) {
                status = "待处理";
                action = "查看报告";
            } else {
                status = "处理完成";
                action = "查看报告";
            }
            m.put("status", status);
            m.put("action", action);

            patients.add(m);
        }
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/patient-report/{userId}")
    public ResponseEntity<Map<String, Object>> getPatientReport(@PathVariable Integer userId, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer currentDoctorUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());

        // 验证当前医生是否是该患者最后一次成功预约的医生
        // 或者当前医生是否有当天的预约记录（放宽条件）
        try {
            var doctorOpt = doctorRepository.findByUserId(currentDoctorUserId);
            if (doctorOpt.isEmpty()) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "当前用户不是医生");
                return ResponseEntity.status(403).body(res);
            }
            Integer currentDoctorId = doctorOpt.get().getDoctorId();

            // 获取 patient_id
            Integer patientIdVal;
            try {
                patientIdVal = jdbcTemplate.queryForObject("SELECT patient_id FROM patients WHERE user_id = ?", Integer.class, userId);
            } catch (Exception e) {
                // 如果找不到对应的患者ID，说明不是患者或数据异常
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "未找到患者信息");
                return ResponseEntity.status(404).body(res);
            }
            final Integer patientId = patientIdVal;

            var lastApptOpt = appointmentRepository.findLastSuccessfulAppointment(patientId);
            boolean isLastDoctor = lastApptOpt.isPresent() && lastApptOpt.get().getDoctorId().equals(currentDoctorId);
            
            // 补充检查：如果不是最后一次预约，但今天有预约，也可以看
            boolean hasTodayAppt = !appointmentRepository.findByDoctorIdAndAppointmentDate(currentDoctorId, java.time.LocalDate.now())
                    .stream().filter(a -> a.getPatientId().equals(patientId)).findAny().isEmpty();

            if (!isLastDoctor && !hasTodayAppt) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "您无权查看该患者的报告（只能查看最近一次成功预约您的患者，或今日有预约的患者）");
                return ResponseEntity.status(403).body(res);
            }
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "权限验证失败");
            return ResponseEntity.status(500).body(res);
        }

        var userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "用户不存在");
            return ResponseEntity.status(404).body(res);
        }
        var user = userOpt.get();

        java.time.LocalDate bloodDate = latestDate(() -> bloodRepository.findByUserIdOrderByCheckDateDesc(userId));
        java.time.LocalDate urineDate = latestDate(() -> urineRepository.findByUserIdOrderByCheckDateDesc(userId));
        java.time.LocalDate liverDate = latestDate(() -> liverRepository.findByUserIdOrderByCheckDateDesc(userId));
        java.time.LocalDate kidneyDate = latestDate(() -> kidneyRepository.findByUserIdOrderByCheckDateDesc(userId));
        java.time.LocalDate lastExamDate = maxOf(bloodDate, urineDate, liverDate, kidneyDate);
        if (lastExamDate == null) {
            java.time.LocalDate itemBlood = firstDate(bloodItemRepository.findDistinctDatesByUserId(userId));
            java.time.LocalDate itemUrine = firstDate(urineItemRepository.findDistinctDatesByUserId(userId));
            java.time.LocalDate itemLiver = firstDate(liverItemRepository.findDistinctDatesByUserId(userId));
            java.time.LocalDate itemKidney = firstDate(kidneyItemRepository.findDistinctDatesByUserId(userId));
            lastExamDate = maxOf(itemBlood, itemUrine, itemLiver, itemKidney);
        }

        java.util.List<Map<String, Object>> blood = java.util.Collections.emptyList();
        java.util.List<Map<String, Object>> urine = java.util.Collections.emptyList();
        java.util.List<Map<String, Object>> liver = java.util.Collections.emptyList();
        java.util.List<Map<String, Object>> kidney = java.util.Collections.emptyList();
        if (lastExamDate != null) {
            blood = mapItems(bloodItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
            urine = mapItems(urineItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
            liver = mapItems(liverItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
            kidney = mapItems(kidneyItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
        }

        Map<String, Object> basicInfo = new java.util.HashMap<>();
        basicInfo.put("name", user.getRealName());
        basicInfo.put("gender", user.getGender());
        basicInfo.put("age", user.getAge());
        basicInfo.put("userId", user.getUserId());
        String patientCode = null;
        try {
            patientCode = jdbcTemplate.queryForObject("SELECT patient_code FROM patients WHERE user_id = ?", String.class, userId);
        } catch (Exception ignored) {}
        basicInfo.put("medicalCardNumber", patientCode);

        Map<String, Object> reports = new java.util.HashMap<>();
        reports.put("blood", blood);
        reports.put("urine", urine);
        reports.put("kidney", kidney);
        reports.put("liver", liver);

        Map<String, Object> analysis = new java.util.HashMap<>();
        if (lastExamDate != null) {
            bloodRepository.findByUserIdAndCheckDate(userId, lastExamDate).ifPresent(h -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("predictionResult", h.getPredictionResult());
                m.put("riskLevel", h.getRiskLevel());
                m.put("abnormalProbability", h.getAbnormalProbability());
                m.put("normalProbability", h.getNormalProbability());
                m.put("abnormalCount", h.getAbnormalCount());
                m.put("recommendation", h.getRecommendation());
                m.put("modelConfidence", h.getModelConfidence());
                m.put("predictionTime", h.getPredictionTime() != null ? h.getPredictionTime().toString() : null);
                m.put("abnormalIndicators", safeJsonArray(h.getAbnormalIndicators()));
                analysis.put("blood", m);
            });
            urineRepository.findByUserIdAndCheckDate(userId, lastExamDate).ifPresent(h -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("predictionResult", h.getPredictionResult());
                m.put("riskLevel", h.getRiskLevel());
                m.put("riskProbability", h.getRiskProbability());
                m.put("riskScore", h.getRiskScore());
                m.put("keyIndicatorCount", h.getKeyIndicatorCount());
                m.put("recommendation", h.getRecommendation());
                m.put("modelConfidence", h.getModelConfidence());
                m.put("predictionTime", h.getPredictionTime() != null ? h.getPredictionTime().toString() : null);
                m.put("keyIndicators", safeJsonArray(h.getKeyIndicators()));
                analysis.put("urine", m);
            });
            kidneyRepository.findByUserIdAndCheckDate(userId, lastExamDate).ifPresent(h -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("predictionResult", h.getPredictionResult());
                m.put("riskLevel", h.getRiskLevel());
                m.put("riskProbability", h.getRiskProbability());
                m.put("riskScore", h.getRiskScore());
                m.put("abnormalCount", h.getAbnormalCount());
                m.put("recommendation", h.getRecommendation());
                m.put("modelConfidence", h.getModelConfidence());
                m.put("predictionTime", h.getPredictionTime() != null ? h.getPredictionTime().toString() : null);
                m.put("abnormalIndicators", safeJsonArray(h.getAbnormalIndicators()));
                m.put("diagnosisHypotheses", safeJsonArray(h.getDiagnosisHypotheses()));
                analysis.put("kidney", m);
            });
            liverRepository.findByUserIdAndCheckDate(userId, lastExamDate).ifPresent(h -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("predictionResult", h.getPredictionResult());
                m.put("riskLevel", h.getRiskLevel());
                m.put("riskProbability", h.getRiskProbability());
                m.put("riskScore", h.getRiskScore());
                m.put("abnormalCount", h.getAbnormalCount());
                m.put("recommendation", h.getRecommendation());
                m.put("modelConfidence", h.getModelConfidence());
                m.put("predictionTime", h.getPredictionTime() != null ? h.getPredictionTime().toString() : null);
                m.put("abnormalIndicators", safeJsonArray(h.getAbnormalIndicators()));
                m.put("diagnosisHypotheses", safeJsonArray(h.getDiagnosisHypotheses()));
                analysis.put("liver", m);
            });
        }

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        res.put("basicInfo", basicInfo);
        res.put("lastExamDate", lastExamDate != null ? lastExamDate.toString() : null);
        res.put("reports", reports);
        res.put("analysis", analysis.isEmpty() ? null : analysis);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/advice/save")
    public ResponseEntity<Map<String, Object>> saveAdvice(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer doctorUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        Integer patientUserId = Integer.parseInt(String.valueOf(body.get("patientUserId")));
        String suggestion = String.valueOf(body.get("suggestion"));

        // 验证权限：只有最后一次成功预约的医生可以保存建议
        try {
            var doctorOpt = doctorRepository.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "当前用户不是医生");
                return ResponseEntity.status(403).body(res);
            }
            Integer currentDoctorId = doctorOpt.get().getDoctorId();

            // 将 patientUserId 映射为 patient_id
            Integer patientIdVal;
            try {
                patientIdVal = jdbcTemplate.queryForObject("SELECT patient_id FROM patients WHERE user_id = ?", Integer.class, patientUserId);
            } catch (Exception e) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "未找到患者信息");
                return ResponseEntity.status(404).body(res);
            }
            final Integer patientId = patientIdVal;

            var lastApptOpt = appointmentRepository.findLastSuccessfulAppointment(patientId);
            if (lastApptOpt.isEmpty() || !lastApptOpt.get().getDoctorId().equals(currentDoctorId)) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "您无权给该患者撰写建议（只能给最近一次成功预约您的患者撰写）");
                return ResponseEntity.status(403).body(res);
            }
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "权限验证失败");
            return ResponseEntity.status(500).body(res);
        }

        com.example.aihealthcheck.entity.doctor.DoctorAdvice advice = new com.example.aihealthcheck.entity.doctor.DoctorAdvice();
        advice.setDoctorUserId(doctorUserId);
        advice.setPatientUserId(patientUserId);
        advice.setSuggestion(suggestion);
        advice.setStatus("DRAFT");
        advice.setCreatedAt(java.time.LocalDateTime.now());
        advice.setUpdatedAt(java.time.LocalDateTime.now());
        doctorAdviceRepository.save(advice);

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        res.put("adviceId", advice.getId());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/advice/send")
    public ResponseEntity<Map<String, Object>> sendAdvice(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer doctorUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        Integer patientUserId = Integer.parseInt(String.valueOf(body.get("patientUserId")));
        
        // 验证权限：只有最后一次成功预约的医生可以发送建议
        try {
            var doctorOpt = doctorRepository.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "当前用户不是医生");
                return ResponseEntity.status(403).body(res);
            }
            Integer currentDoctorId = doctorOpt.get().getDoctorId();

            // 将 patientUserId 映射为 patient_id
            Integer patientIdVal;
            try {
                patientIdVal = jdbcTemplate.queryForObject("SELECT patient_id FROM patients WHERE user_id = ?", Integer.class, patientUserId);
            } catch (Exception e) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "未找到患者信息");
                return ResponseEntity.status(404).body(res);
            }
            final Integer patientId = patientIdVal;

            var lastApptOpt = appointmentRepository.findLastSuccessfulAppointment(patientId);
            if (lastApptOpt.isEmpty() || !lastApptOpt.get().getDoctorId().equals(currentDoctorId)) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "您无权给该患者发送建议（只能给最近一次成功预约您的患者发送）");
                return ResponseEntity.status(403).body(res);
            }
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "权限验证失败");
            return ResponseEntity.status(500).body(res);
        }

        String suggestion = String.valueOf(body.get("suggestion"));

        com.example.aihealthcheck.entity.doctor.DoctorAdvice advice = new com.example.aihealthcheck.entity.doctor.DoctorAdvice();
        advice.setDoctorUserId(doctorUserId);
        advice.setPatientUserId(patientUserId);
        advice.setSuggestion(suggestion);
        advice.setStatus("SENT");
        advice.setCreatedAt(java.time.LocalDateTime.now());
        advice.setUpdatedAt(java.time.LocalDateTime.now());
        doctorAdviceRepository.save(advice);

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        res.put("message", "已发送给患者");
        res.put("adviceId", advice.getId());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/call/start")
    public ResponseEntity<Map<String, Object>> startCall(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer doctorUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        Integer patientUserId = Integer.parseInt(String.valueOf(body.get("patientUserId")));
        try {
            var doctorOpt = doctorRepository.findByUserId(doctorUserId);
            if (doctorOpt.isEmpty()) {
                Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", false);
                res.put("message", "当前用户不是医生");
                return ResponseEntity.status(403).body(res);
            }
            var doctor = doctorOpt.get();
            String deptName = doctor.getDepartment() != null ? doctor.getDepartment().getDeptName() : "全科";
            String message = "请" + (jdbcTemplate.queryForObject("SELECT real_name FROM users WHERE user_id = ?", String.class, patientUserId))
                    + "到" + deptName + doctor.getRealName() + "医生咨询就诊！";
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "CALL");
            payload.put("active", true);
            payload.put("message", message);
            String suggestionJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            com.example.aihealthcheck.entity.doctor.DoctorAdvice advice = new com.example.aihealthcheck.entity.doctor.DoctorAdvice();
            advice.setDoctorUserId(doctorUserId);
            advice.setPatientUserId(patientUserId);
            advice.setSuggestion(suggestionJson);
            advice.setStatus("CALL");
            advice.setCreatedAt(java.time.LocalDateTime.now());
            advice.setUpdatedAt(java.time.LocalDateTime.now());
            doctorAdviceRepository.save(advice);
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", true);
            res.put("message", message);
            res.put("adviceId", advice.getId());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "呼叫失败");
            return ResponseEntity.status(500).body(res);
        }
    }

    @PostMapping("/call/cancel")
    public ResponseEntity<Map<String, Object>> cancelCall(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer doctorUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        Integer patientUserId = Integer.parseInt(String.valueOf(body.get("patientUserId")));
        try {
            // 找到最近的CALL记录，更新为取消
            var list = doctorAdviceRepository.findByPatientAndDoctor(patientUserId, doctorUserId);
            var call = list.stream().filter(a -> "CALL".equals(a.getStatus())).findFirst();
            if (call.isPresent()) {
                var a = call.get();
                a.setStatus("CALL_CANCELLED");
                a.setUpdatedAt(java.time.LocalDateTime.now());
                doctorAdviceRepository.save(a);
            }
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", true);
            res.put("message", "已取消呼叫");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "取消失败");
            return ResponseEntity.status(500).body(res);
        }
    }

    @GetMapping("/call/me")
    public ResponseEntity<Map<String, Object>> getMyCall(jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer patientUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        var list = doctorAdviceRepository.findByPatientUserId(patientUserId);
        var opt = list.stream().filter(a -> "CALL".equals(a.getStatus())).findFirst();
        Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        if (opt.isPresent()) {
            String suggestion = opt.get().getSuggestion();
            try {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(suggestion);
                if (node.has("active") && node.get("active").asBoolean()) {
                    res.put("active", true);
                    res.put("message", node.get("message").asText());
                    return ResponseEntity.ok(res);
                }
            } catch (Exception ignored) {}
        }
        res.put("active", false);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/checklist/send")
    public ResponseEntity<Map<String, Object>> sendChecklist(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer doctorUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        Integer patientUserId = Integer.parseInt(String.valueOf(body.get("patientUserId")));
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) body.get("items");
        boolean allowBypass = body.containsKey("allowBypass") && Boolean.TRUE.equals(body.get("allowBypass"));
        
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "CHECKLIST");
            payload.put("items", items);
            payload.put("sentAt", java.time.LocalDateTime.now().toString());
            payload.put("allCompleted", false);
            payload.put("allowBypass", allowBypass);
            
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            com.example.aihealthcheck.entity.doctor.DoctorAdvice advice = new com.example.aihealthcheck.entity.doctor.DoctorAdvice();
            advice.setDoctorUserId(doctorUserId);
            advice.setPatientUserId(patientUserId);
            advice.setSuggestion(json);
            advice.setStatus("CHECKLIST_SENT");
            advice.setCreatedAt(java.time.LocalDateTime.now());
            advice.setUpdatedAt(java.time.LocalDateTime.now());
            doctorAdviceRepository.save(advice);
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", true);
            res.put("adviceId", advice.getId());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "发送清单失败");
            return ResponseEntity.status(500).body(res);
        }
    }

    @GetMapping("/checklist/me")
    public ResponseEntity<Map<String, Object>> getMyChecklist(jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer patientUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        var list = doctorAdviceRepository.findByPatientUserId(patientUserId);
        var opt = list.stream().filter(a -> "CHECKLIST_SENT".equals(a.getStatus()) || "CHECKLIST_PROGRESS".equals(a.getStatus()) || "CHECKLIST_COMPLETED".equals(a.getStatus())).findFirst();
        Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        if (opt.isPresent()) {
            String suggestion = opt.get().getSuggestion();
            try {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(suggestion);
                java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
                if (node.has("items")) {
                    for (var it : node.get("items")) {
                        Map<String, Object> m = new java.util.HashMap<>();
                        m.put("name", it.get("name").asText());
                        m.put("location", it.get("location").asText());
                        m.put("completed", it.has("completed") && it.get("completed").asBoolean());
                        items.add(m);
                    }
                }
                res.put("hasChecklist", true);
                res.put("items", items);
                res.put("allCompleted", node.has("allCompleted") && node.get("allCompleted").asBoolean());
                res.put("allowBypass", node.has("allowBypass") && node.get("allowBypass").asBoolean());
                return ResponseEntity.ok(res);
            } catch (Exception ignored) {}
        }
        res.put("hasChecklist", false);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/checklist/progress")
    public ResponseEntity<Map<String, Object>> updateChecklistProgress(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer patientUserId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) body.get("items");
        var list = doctorAdviceRepository.findByPatientUserId(patientUserId);
        var opt = list.stream().filter(a -> "CHECKLIST_SENT".equals(a.getStatus()) || "CHECKLIST_PROGRESS".equals(a.getStatus())).findFirst();
        if (opt.isEmpty()) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "暂无待完成清单");
            return ResponseEntity.badRequest().body(res);
        }
        try {
            boolean allCompleted = items.stream().allMatch(i -> Boolean.TRUE.equals(i.get("completed")));
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "CHECKLIST");
            payload.put("items", items);
            payload.put("allCompleted", allCompleted);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            var a = opt.get();
            a.setSuggestion(json);
            a.setStatus(allCompleted ? "CHECKLIST_COMPLETED" : "CHECKLIST_PROGRESS");
            a.setUpdatedAt(java.time.LocalDateTime.now());
            doctorAdviceRepository.save(a);
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", true);
            res.put("allCompleted", allCompleted);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("success", false);
            res.put("message", "更新清单失败");
            return ResponseEntity.status(500).body(res);
        }
    }
    private java.time.LocalDate latestDate(SupplierWithException<List<?>> supplier) {
        try {
            List<?> list = supplier.get();
            if (list == null || list.isEmpty()) return null;
            Object first = list.get(0);
            try {
                var method = first.getClass().getMethod("getCheckDate");
                Object dateObj = method.invoke(first);
                if (dateObj instanceof java.time.LocalDate d) return d;
            } catch (Exception ignored) {}
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private java.time.LocalDate maxOf(java.time.LocalDate... dates) {
        java.time.LocalDate max = null;
        for (java.time.LocalDate d : dates) {
            if (d == null) continue;
            if (max == null || d.isAfter(max)) max = d;
        }
        return max;
    }

    private java.time.LocalDate firstDate(List<java.time.LocalDate> dates) {
        if (dates == null || dates.isEmpty()) return null;
        return dates.get(0);
    }

    private List<Map<String, Object>> mapItems(List<?> items) {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Object obj : items) {
            Map<String, Object> m = new java.util.HashMap<>();
            try {
                String cardNo = (String) obj.getClass().getMethod("getCardNumber").invoke(obj);
                String itemName = (String) obj.getClass().getMethod("getItemName").invoke(obj);
                String itemValue = (String) obj.getClass().getMethod("getItemValue").invoke(obj);
                String unit = (String) obj.getClass().getMethod("getUnit").invoke(obj);
                m.put("cardNo", cardNo);
                m.put("itemName", itemName);
                m.put("result", unit != null && itemValue != null ? itemValue + " " + unit : itemValue);
                m.put("unit", unit);
                m.put("reference", null);
                m.put("status", "normal");
            } catch (Exception ignored) {}
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> safeJsonArray(String json) {
        if (json == null || json.isBlank()) return java.util.Collections.emptyList();
        try {
            if (json.trim().equals("[]")) return java.util.Collections.emptyList();
            if (json.startsWith("[") && json.endsWith("]")) {
                String inner = json.substring(1, json.length() - 1).trim();
                if (inner.isEmpty()) return java.util.Collections.emptyList();
                String[] parts = inner.split("\\},\\s*\\{");
                return java.util.Arrays.stream(parts).map(s -> {
                    String normalized = s;
                    if (!normalized.startsWith("{")) normalized = "{" + normalized;
                    if (!normalized.endsWith("}")) normalized = normalized + "}";
                    Map<String, Object> m = new java.util.HashMap<>();
                    String[] kvs = normalized.substring(1, normalized.length() - 1).split(",");
                    for (String kv : kvs) {
                        String[] pair = kv.split(":", 2);
                        if (pair.length == 2) {
                            String k = pair[0].trim().replaceAll("^\"|\"$", "");
                            String v = pair[1].trim().replaceAll("^\"|\"$", "");
                            m.put(k, v);
                        }
                    }
                    return m;
                }).collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception ignored) {}
        return java.util.Collections.emptyList();
    }

    @FunctionalInterface
    interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
