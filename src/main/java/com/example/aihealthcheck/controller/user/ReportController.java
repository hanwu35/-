package com.example.aihealthcheck.controller.user;

import com.example.aihealthcheck.entity.User;
import com.example.aihealthcheck.repository.doctor.DoctorAdviceRepository;
import com.example.aihealthcheck.repository.health.examination.blood.HealthBloodItemRepository;
import com.example.aihealthcheck.repository.health.examination.blood.HealthBloodRepository;
import com.example.aihealthcheck.repository.health.examination.Kindey.HealthKidneyItemRepository;
import com.example.aihealthcheck.repository.health.examination.Kindey.HealthKidneyRepository;
import com.example.aihealthcheck.repository.health.examination.Liver.HealthLiverItemRepository;
import com.example.aihealthcheck.repository.health.examination.Liver.HealthLiverRepository;
import com.example.aihealthcheck.repository.health.examination.Urine.HealthUrineItemRepository;
import com.example.aihealthcheck.repository.health.examination.Urine.HealthUrineRepository;
import com.example.aihealthcheck.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HealthBloodRepository bloodRepository;
    @Autowired
    private HealthUrineRepository urineRepository;
    @Autowired
    private HealthLiverRepository liverRepository;
    @Autowired
    private HealthKidneyRepository kidneyRepository;

    @Autowired
    private HealthBloodItemRepository bloodItemRepository;
    @Autowired
    private HealthUrineItemRepository urineItemRepository;
    @Autowired
    private HealthLiverItemRepository liverItemRepository;
    @Autowired
    private HealthKidneyItemRepository kidneyItemRepository;

    @Autowired
    private com.example.aihealthcheck.repository.user.AppointmentRepository appointmentRepository;

    @Autowired
    private com.example.aihealthcheck.repository.doctor.DoctorAdviceRepository doctorAdviceRepository;

    @Autowired
    private com.example.aihealthcheck.repository.user.DoctorRepository doctorRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> myReports(HttpSession session) {
        Object uidObj = session.getAttribute("userId");
        if (uidObj == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "未登录");
            return ResponseEntity.status(401).body(res);
        }
        Integer userId = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.parseInt(uidObj.toString());

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "用户不存在");
            return ResponseEntity.status(404).body(res);
        }
        User user = userOpt.get();

        String patientCode = null;
        try {
            patientCode = jdbcTemplate.queryForObject("SELECT patient_code FROM patients WHERE user_id = ?", String.class, userId);
        } catch (Exception ignored) {}

        LocalDate bloodDate = getLatestDate(() -> bloodRepository.findByUserIdOrderByCheckDateDesc(userId));
        LocalDate urineDate = getLatestDate(() -> urineRepository.findByUserIdOrderByCheckDateDesc(userId));
        LocalDate liverDate = getLatestDate(() -> liverRepository.findByUserIdOrderByCheckDateDesc(userId));
        LocalDate kidneyDate = getLatestDate(() -> kidneyRepository.findByUserIdOrderByCheckDateDesc(userId));

        LocalDate lastExamDate = latestOf(bloodDate, urineDate, liverDate, kidneyDate);
        if (lastExamDate == null) {
            LocalDate itemBlood = firstDate(bloodItemRepository.findDistinctDatesByUserId(userId));
            LocalDate itemUrine = firstDate(urineItemRepository.findDistinctDatesByUserId(userId));
            LocalDate itemLiver = firstDate(liverItemRepository.findDistinctDatesByUserId(userId));
            LocalDate itemKidney = firstDate(kidneyItemRepository.findDistinctDatesByUserId(userId));
            lastExamDate = latestOf(itemBlood, itemUrine, itemLiver, itemKidney);
        }

        List<Map<String, Object>> blood = Collections.emptyList();
        List<Map<String, Object>> urine = Collections.emptyList();
        List<Map<String, Object>> liver = Collections.emptyList();
        List<Map<String, Object>> kidney = Collections.emptyList();

        if (lastExamDate != null) {
            blood = mapItems(bloodItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
            urine = mapItems(urineItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
            liver = mapItems(liverItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
            kidney = mapItems(kidneyItemRepository.findByUserIdAndCheckDate(userId, lastExamDate));
        }

        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("name", user.getRealName());
        basicInfo.put("gender", user.getGender());
        basicInfo.put("age", user.getAge());
        basicInfo.put("accountId", user.getAccount());
        basicInfo.put("userId", user.getUserId());
        basicInfo.put("medicalCardNumber", patientCode);

        Map<String, Object> reports = new HashMap<>();
        reports.put("blood", blood);
        reports.put("urine", urine);
        reports.put("kidney", kidney);
        reports.put("liver", liver);

        Map<String, Object> analysis = new HashMap<>();
        if (lastExamDate != null) {
            bloodRepository.findByUserIdAndCheckDate(userId, lastExamDate).ifPresent(h -> {
                Map<String, Object> m = new HashMap<>();
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
                Map<String, Object> m = new HashMap<>();
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
                Map<String, Object> m = new HashMap<>();
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
                Map<String, Object> m = new HashMap<>();
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

        // 获取医生建议 - 仅获取最后一次成功预约的医生的建议
        String doctorSuggestion = null;
        try {
            Integer pid = jdbcTemplate.queryForObject("SELECT patient_id FROM patients WHERE user_id = ?", Integer.class, userId);
            var lastApptOpt = appointmentRepository.findLastSuccessfulAppointment(pid);
            
            if (lastApptOpt.isPresent()) {
                Integer doctorId = lastApptOpt.get().getDoctorId();
                var doctorOpt = doctorRepository.findById(doctorId);
                
                if (doctorOpt.isPresent()) {
                    Integer doctorUserId = doctorOpt.get().getUserId();
                    
                    doctorSuggestion = doctorAdviceRepository.findByPatientAndDoctor(userId, doctorUserId)
                        .stream()
                        .filter(advice -> "SENT".equals(advice.getStatus()) || "COMPLETED".equals(advice.getStatus()))
                        .findFirst() // 因为findByPatientAndDoctor按updatedAt DESC排序
                        .map(com.example.aihealthcheck.entity.doctor.DoctorAdvice::getSuggestion)
                        .orElse(null);
                }
            }
        } catch (Exception ignored) {}

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("basicInfo", basicInfo);
        res.put("lastExamDate", lastExamDate != null ? lastExamDate.toString() : null);
        res.put("reports", reports);
        res.put("analysis", analysis.isEmpty() ? null : analysis);
        res.put("doctorSuggestion", doctorSuggestion);
        return ResponseEntity.ok(res);
    }

    private LocalDate getLatestDate(SupplierWithException<List<?>> supplier) {
        try {
            List<?> list = supplier.get();
            if (list == null || list.isEmpty()) return null;
            Object first = list.get(0);
            try {
                var method = first.getClass().getMethod("getCheckDate");
                Object dateObj = method.invoke(first);
                if (dateObj instanceof LocalDate d) return d;
            } catch (Exception ignored) {}
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate latestOf(LocalDate... dates) {
        LocalDate max = null;
        for (LocalDate d : dates) {
            if (d == null) continue;
            if (max == null || d.isAfter(max)) max = d;
        }
        return max;
    }

    private List<Map<String, Object>> mapItems(List<?> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object obj : items) {
            Map<String, Object> m = new HashMap<>();
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

    @FunctionalInterface
    interface SupplierWithException<T> {
        T get() throws Exception;
    }

    private List<Map<String, Object>> safeJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            if (json.trim().equals("[]")) return Collections.emptyList();
            if (json.startsWith("[") && json.endsWith("]")) {
                String inner = json.substring(1, json.length() - 1).trim();
                if (inner.isEmpty()) return Collections.emptyList();
                String[] parts = inner.split("\\},\\s*\\{");
                return Arrays.stream(parts).map(s -> {
                    String normalized = s;
                    if (!normalized.startsWith("{")) normalized = "{" + normalized;
                    if (!normalized.endsWith("}")) normalized = normalized + "}";
                    Map<String, Object> m = new HashMap<>();
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
                }).collect(Collectors.toList());
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private LocalDate firstDate(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) return null;
        return dates.get(0);
    }
}
