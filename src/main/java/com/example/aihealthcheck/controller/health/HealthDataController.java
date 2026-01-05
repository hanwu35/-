// 文件位置：src/main/java/com/example/aihealthcheck/controller/HealthDataController.java
package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.entity.*;
import com.example.aihealthcheck.entity.health.examination.Blood.*;
import com.example.aihealthcheck.entity.health.examination.Kindey.*;
import com.example.aihealthcheck.entity.health.examination.Liver.*;
import com.example.aihealthcheck.entity.health.examination.Urine.*;
import com.example.aihealthcheck.entity.health.record.*;
import com.example.aihealthcheck.entity.health.indicator.*;
import com.example.aihealthcheck.repository.*;
import com.example.aihealthcheck.repository.health.examination.blood.*;
import com.example.aihealthcheck.repository.health.examination.Kindey.*;
import com.example.aihealthcheck.repository.health.examination.Liver.*;
import com.example.aihealthcheck.repository.health.examination.Urine.*;
import com.example.aihealthcheck.repository.health.record.*;
import com.example.aihealthcheck.repository.health.indicator.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health-data")
public class HealthDataController {

    @Autowired
    private HealthIndicatorRepository indicatorRepository;

    @Autowired
    private HealthBloodRepository bloodRepository;

    @Autowired
    private HealthUrineRepository urineRepository;

    @Autowired
    private HealthLiverRepository liverRepository;

    @Autowired
    private HealthKidneyRepository kidneyRepository;

    /**
     * 获取用户的所有健康数据总览
     */
    @GetMapping("/user/{userId}/overview")
    public ResponseEntity<Map<String, Object>> getUserHealthOverview(@PathVariable Integer userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取总览记录
            List<HealthIndicator> indicators = indicatorRepository.findByUserId(userId);

            // 统计信息
            long totalIndicators = indicatorRepository.countByUserId(userId);
            long bloodCount = bloodRepository.findByUserId(userId).size();
            long urineCount = urineRepository.findByUserId(userId).size();
            long liverCount = liverRepository.findByUserId(userId).size();
            long kidneyCount = kidneyRepository.findByUserId(userId).size();

            response.put("success", true);
            response.put("userId", userId);
            response.put("totalIndicators", totalIndicators);
            response.put("bloodRecords", bloodCount);
            response.put("urineRecords", urineCount);
            response.put("liverRecords", liverCount);
            response.put("kidneyRecords", kidneyCount);
            response.put("indicators", indicators);

            // 检测类别统计
            Map<String, Boolean> testTypes = new HashMap<>();
            if (!indicators.isEmpty()) {
                HealthIndicator latest = indicators.get(0);
                testTypes.put("hasBloodTest", latest.getHasBloodData());
                testTypes.put("hasUrineTest", latest.getHasUrineData());
                testTypes.put("hasLiverTest", latest.getHasLiverData());
                testTypes.put("hasKidneyTest", latest.getHasKidneyData());
            }
            response.put("testTypes", testTypes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取用户的详细健康数据
     */
    @GetMapping("/user/{userId}/detailed")
    public ResponseEntity<Map<String, Object>> getUserDetailedData(
            @PathVariable Integer userId,
            @RequestParam(required = false) String date) {

        Map<String, Object> response = new HashMap<>();

        try {
            LocalDate checkDate = date != null ? LocalDate.parse(date) : null;

            Map<String, Object> data = new HashMap<>();

            // 获取各表数据
            if (checkDate != null) {
                // 获取指定日期的数据
                data.put("blood", bloodRepository.findByUserIdAndCheckDate(userId, checkDate));
                data.put("urine", urineRepository.findByUserIdAndCheckDate(userId, checkDate));
                data.put("liver", liverRepository.findByUserIdAndCheckDate(userId, checkDate));
                data.put("kidney", kidneyRepository.findByUserIdAndCheckDate(userId, checkDate));
                data.put("indicator", indicatorRepository.findByUserIdAndCheckDate(userId, checkDate));
            } else {
                // 获取所有数据
                data.put("blood", bloodRepository.findByUserId(userId));
                data.put("urine", urineRepository.findByUserId(userId));
                data.put("liver", liverRepository.findByUserId(userId));
                data.put("kidney", kidneyRepository.findByUserId(userId));
                data.put("indicators", indicatorRepository.findByUserId(userId));
            }

            response.put("success", true);
            response.put("userId", userId);
            response.put("checkDate", checkDate);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取详细数据失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取最新一条记录的所有数据
     */
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<Map<String, Object>> getUserLatestData(@PathVariable Integer userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取最新总览记录
            List<HealthIndicator> indicators = indicatorRepository.findByUserId(userId);
            if (indicators.isEmpty()) {
                response.put("success", true);
                response.put("message", "该用户暂无健康数据");
                return ResponseEntity.ok(response);
            }

            HealthIndicator latestIndicator = indicators.get(0);
            LocalDate latestDate = latestIndicator.getCheckDate();

            // 获取该日期的所有数据
            Map<String, Object> latestData = new HashMap<>();
            latestData.put("indicator", latestIndicator);
            latestData.put("blood", bloodRepository.findByUserIdAndCheckDate(userId, latestDate));
            latestData.put("urine", urineRepository.findByUserIdAndCheckDate(userId, latestDate));
            latestData.put("liver", liverRepository.findByUserIdAndCheckDate(userId, latestDate));
            latestData.put("kidney", kidneyRepository.findByUserIdAndCheckDate(userId, latestDate));

            response.put("success", true);
            response.put("checkDate", latestDate);
            response.put("data", latestData);
            response.put("hasBlood", latestIndicator.getHasBloodData());
            response.put("hasUrine", latestIndicator.getHasUrineData());
            response.put("hasLiver", latestIndicator.getHasLiverData());
            response.put("hasKidney", latestIndicator.getHasKidneyData());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取最新数据失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 健康数据统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getHealthStats() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 这里可以添加系统级别的统计信息
            long totalBloodRecords = bloodRepository.count();
            long totalUrineRecords = urineRepository.count();
            long totalLiverRecords = liverRepository.count();
            long totalKidneyRecords = kidneyRepository.count();
            long totalIndicatorRecords = indicatorRepository.count();

            response.put("success", true);
            response.put("totalBloodRecords", totalBloodRecords);
            response.put("totalUrineRecords", totalUrineRecords);
            response.put("totalLiverRecords", totalLiverRecords);
            response.put("totalKidneyRecords", totalKidneyRecords);
            response.put("totalIndicatorRecords", totalIndicatorRecords);
            response.put("totalAllRecords", totalBloodRecords + totalUrineRecords + totalLiverRecords + totalKidneyRecords);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}