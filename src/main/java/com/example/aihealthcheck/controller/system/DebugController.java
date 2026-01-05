package com.example.aihealthcheck.controller.debug;

import com.example.aihealthcheck.entity.health.examination.Blood.HealthBlood;
import com.example.aihealthcheck.repository.health.examination.blood.HealthBloodRepository;
import com.example.aihealthcheck.repository.health.examination.Urine.HealthUrineRepository;
import com.example.aihealthcheck.repository.health.examination.Liver.HealthLiverRepository;
import com.example.aihealthcheck.repository.health.examination.Kindey.HealthKidneyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private HealthBloodRepository bloodRepository;

    @Autowired
    private HealthUrineRepository urineRepository;

    @Autowired
    private HealthLiverRepository liverRepository;

    @Autowired
    private HealthKidneyRepository kidneyRepository;

    /**
     * 测试创建一条血液记录
     */
    @PostMapping("/create-test-blood")
    public Map<String, Object> createTestBloodRecord() {
        Map<String, Object> response = new HashMap<>();

        try {
            HealthBlood blood = new HealthBlood();
            blood.setUserId(1);
            blood.setCheckDate(LocalDate.now());
            blood.setHemoglobin(130.5);
            blood.setWhiteBloodCell(6.8);
            blood.setPlatelet(250.0);
            blood.setBloodGlucose(5.2);

            blood.setPredictionResult("正常");
            blood.setRiskLevel("低风险");
            blood.setModelConfidence(95.5);

            HealthBlood saved = bloodRepository.save(blood);

            response.put("success", true);
            response.put("message", "测试血液记录创建成功");
            response.put("id", saved.getId());
            response.put("data", saved);

            logger.info("测试血液记录创建成功: {}", saved);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建失败: " + e.getMessage());
            logger.error("创建测试血液记录失败", e);
        }

        return response;
    }

    /**
     * 查看所有健康数据统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long bloodCount = bloodRepository.count();
            long urineCount = urineRepository.count();
            long liverCount = liverRepository.count();
            long kidneyCount = kidneyRepository.count();

            stats.put("blood_records", bloodCount);
            stats.put("urine_records", urineCount);
            stats.put("liver_records", liverCount);
            stats.put("kidney_records", kidneyCount);
            stats.put("total", bloodCount + urineCount + liverCount + kidneyCount);

            logger.info("数据统计 - 血液: {}, 尿液: {}, 肝: {}, 肾: {}",
                bloodCount, urineCount, liverCount, kidneyCount);

        } catch (Exception e) {
            stats.put("error", e.getMessage());
            logger.error("获取统计信息失败", e);
        }

        return stats;
    }
}