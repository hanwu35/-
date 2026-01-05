package com.example.aihealthcheck.service.prediction;

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ModelPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(ModelPredictionService.class);
    private static final Logger predictionLogger = LoggerFactory.getLogger("PredictionService");

    @Autowired
    private PythonModelExecutor pythonModelExecutor;

    @Autowired
    private HealthBloodRepository healthBloodRepository;

    @Autowired
    private HealthUrineRepository healthUrineRepository;

    @Autowired
    private HealthLiverRepository healthLiverRepository;

    @Autowired
    private HealthKidneyRepository healthKidneyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${python.model.timeout:300}")
    private int modelTimeout;

    /**
     * 触发所有模型预测（异步执行）
     */
    @Async("taskExecutor")
    @Transactional
    public void triggerAllModelPredictions(Integer userId, String checkDate) {
        logger.info("🎯 开始执行所有模型预测，用户ID: {}，检查日期: {}", userId, checkDate);
        predictionLogger.info("PREDICTION_START | user_id:{} | check_date:{}", userId, checkDate);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 检查数据是否存在
            if (!hasHealthData(userId, checkDate)) {
                logger.warn("⚠️ 用户 {} 在 {} 没有健康数据，跳过预测", userId, checkDate);
                predictionLogger.warn("NO_DATA | user_id:{} | check_date:{}", userId, checkDate);
                return;
            }

            // 2. 并行执行四个模型预测
            Map<String, Map<String, Object>> allResults = new HashMap<>();

            // 使用线程池并行执行
            List<Thread> threads = new ArrayList<>();
            String[] modelTypes = {"liver", "kidney", "blood", "urine"};

            for (String modelType : modelTypes) {
                Thread thread = new Thread(() -> {
                    logger.info("🚀 开始执行{}模型预测...", modelType);
                    predictionLogger.info("MODEL_START | model:{} | user_id:{}", modelType, userId);

                    long modelStartTime = System.currentTimeMillis();
                    Map<String, Object> result = executeSingleModel(modelType, userId, checkDate);
                    long modelEndTime = System.currentTimeMillis();

                    synchronized (allResults) {
                        allResults.put(modelType, result);
                    }

                    long duration = modelEndTime - modelStartTime;
                    if (Boolean.TRUE.equals(result.get("success"))) {
                        logger.info("✅ {}模型预测完成，耗时: {}ms", modelType, duration);
                        predictionLogger.info("MODEL_SUCCESS | model:{} | duration:{}ms", modelType, duration);
                    } else {
                        logger.error("❌ {}模型预测失败，耗时: {}ms，错误: {}", modelType, duration, result.get("error"));
                        predictionLogger.error("MODEL_FAILED | model:{} | duration:{}ms | error:{}", modelType, duration, result.get("error"));
                    }
                });
                thread.start();
                threads.add(thread);
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join(modelTimeout * 1000); // 超时时间
            }

            // 3. 处理预测结果
            processPredictionResults(userId, checkDate, allResults);

            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            logger.info("🎉 所有模型预测处理完成，用户ID: {}，检查日期: {}，总耗时: {}ms", userId, checkDate, totalDuration);
            predictionLogger.info("PREDICTION_COMPLETE | user_id:{} | total_duration:{}ms | success_count:{}",
                    userId, totalDuration, countSuccessResults(allResults));

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            logger.error("💥 执行模型预测时发生错误，用户ID: {}，耗时: {}ms", userId, totalDuration, e);
            predictionLogger.error("PREDICTION_ERROR | user_id:{} | duration:{}ms | error:{}",
                    userId, totalDuration, e.getMessage());
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 执行单个模型预测 - 改进版
     */
    private Map<String, Object> executeSingleModel(String modelType, Integer userId, String checkDate) {
        logger.info("执行{}模型预测，用户ID: {}，检查日期: {}", modelType, userId, checkDate);

        try {
            // 调用Python模型
            Map<String, Object> result = pythonModelExecutor.executeModel(modelType, userId, checkDate);

            if (Boolean.TRUE.equals(result.get("success"))) {
                String output = (String) result.get("output");
                logger.debug("Python原始输出:\n{}", output);

                try {
                    // 改进的JSON解析逻辑
                    Map<String, Object> predictionData = parsePythonOutput(output);

                    if (predictionData != null && !predictionData.isEmpty()) {
                        result.put("predictionData", predictionData);
                        logger.info("{}模型解析成功", modelType);
                    } else {
                        logger.warn("{}模型解析结果为空", modelType);
                        result.put("success", false);
                        result.put("error", "解析结果为空");
                    }
                } catch (Exception e) {
                    logger.error("解析{}模型输出失败: {}", modelType, e.getMessage(), e);
                    result.put("success", false);
                    result.put("error", "解析失败: " + e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("执行{}模型预测时发生错误", modelType, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 解析Python输出 - 改进版
     */
    private Map<String, Object> parsePythonOutput(String output) throws Exception {
        if (output == null || output.trim().isEmpty()) {
            logger.warn("Python输出为空");
            return new HashMap<>();
        }

        // 方法1：尝试直接解析整个输出
        try {
            JsonNode rootNode = objectMapper.readTree(output);
            if (rootNode.isObject()) {
                return objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            logger.debug("尝试直接解析失败: {}", e.getMessage());
        }

        // 方法2：提取JSON部分
        String jsonPart = extractJsonFromMixedOutput(output);
        if (jsonPart != null && !jsonPart.trim().isEmpty()) {
            try {
                return objectMapper.readValue(jsonPart, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.debug("提取JSON部分解析失败: {}", e.getMessage());
            }
        }

        // 方法3：按行查找JSON
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                try {
                    return objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    // 继续下一行
                }
            }
        }

        // 方法4：如果是错误消息，包装成JSON
        if (output.contains("error") || output.contains("Error") || output.contains("ERROR")) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", output);
            errorResult.put("success", false);
            return errorResult;
        }

        logger.warn("无法从输出中解析JSON: {}", output.substring(0, Math.min(output.length(), 200)));
        return new HashMap<>();
    }

    /**
     * 从混合输出中提取JSON
     */
    private String extractJsonFromMixedOutput(String output) {
        if (output == null) return null;

        // 查找第一个{和最后一个}
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            String json = output.substring(start, end + 1);

            // 验证JSON括号是否匹配
            if (isValidJson(json)) {
                return json;
            }
        }

        // 查找第一个[和最后一个]
        start = output.indexOf('[');
        end = output.lastIndexOf(']');

        if (start != -1 && end != -1 && end > start) {
            String json = output.substring(start, end + 1);

            if (isValidJson(json)) {
                return json;
            }
        }

        return null;
    }

    /**
     * 验证JSON是否有效
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) return false;

        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 处理所有预测结果
     */
    private void processPredictionResults(Integer userId, String checkDate,
                                          Map<String, Map<String, Object>> allResults) {
        logger.info("开始处理预测结果...");

        // 处理每个模型的结果
        for (Map.Entry<String, Map<String, Object>> entry : allResults.entrySet()) {
            String modelType = entry.getKey();
            Map<String, Object> result = entry.getValue();

            if (Boolean.TRUE.equals(result.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> predictionData = (Map<String, Object>) result.get("predictionData");

                if (predictionData != null && !predictionData.isEmpty()) {
                    try {
                        savePredictionResult(modelType, userId, checkDate, predictionData);
                        logger.info("{}模型预测结果保存成功", modelType);
                    } catch (Exception e) {
                        logger.error("保存{}模型预测结果失败", modelType, e);
                    }
                } else {
                    logger.warn("{}模型预测数据为空", modelType);
                }
            } else {
                logger.error("{}模型预测失败: {}", modelType, result.get("error"));
            }
        }
    }

    /**
     * 保存预测结果到数据库
     */
    @Transactional
    public void savePredictionResult(String modelType, Integer userId, String checkDate,
                                     Map<String, Object> predictionData) throws Exception {

        LocalDate localCheckDate = LocalDate.parse(checkDate);
        LocalDateTime now = LocalDateTime.now();

        // 先打印原始数据，便于调试
        logger.info("准备保存 {} 模型预测结果，用户ID: {}, 日期: {}", modelType, userId, checkDate);

        switch (modelType) {
            case "blood":
                saveBloodPrediction(userId, localCheckDate, now, predictionData);
                break;
            case "urine":
                saveUrinePrediction(userId, localCheckDate, now, predictionData);
                break;
            case "kidney":
                saveKidneyPrediction(userId, localCheckDate, now, predictionData);
                break;
            case "liver":
                saveLiverPrediction(userId, localCheckDate, now, predictionData);
                break;
            default:
                throw new IllegalArgumentException("未知的模型类型: " + modelType);
        }
    }

    /**
     * 保存血液模型预测结果
     */
    private void saveBloodPrediction(Integer userId, LocalDate checkDate, LocalDateTime predictionTime,
                                     Map<String, Object> predictionData) throws Exception {

        HealthBlood healthBlood = healthBloodRepository.findByUserIdAndCheckDate(userId, checkDate)
                .orElseGet(() -> {
                    HealthBlood newBlood = new HealthBlood();
                    newBlood.setUserId(userId);
                    newBlood.setCheckDate(checkDate);
                    return newBlood;
                });

        logger.info("保存血液模型预测结果");

        // 设置预测结果
        healthBlood.setPredictionResult(getStringValue(predictionData, "prediction_label"));

        // 设置风险概率
        Double riskProbability = getDoubleValue(predictionData, "risk_probability");
        if (riskProbability != null) {
            healthBlood.setAbnormalProbability(riskProbability);
            healthBlood.setNormalProbability(100.0 - riskProbability);
        }

        healthBlood.setAbnormalCount(getIntegerValue(predictionData, "total_abnormal_count"));
        healthBlood.setRiskLevel(getStringValue(predictionData, "risk_level"));
        healthBlood.setRecommendation(getStringValue(predictionData, "recommendation"));
        healthBlood.setModelConfidence(getDoubleValue(predictionData, "model_confidence"));
        healthBlood.setPredictionTime(predictionTime);

        // 保存异常指标
        saveJsonField(predictionData, "key_abnormalities", healthBlood::setAbnormalIndicators);

        healthBloodRepository.save(healthBlood);
        logger.info("血液模型预测结果保存成功，用户ID: {}，检查日期: {}", userId, checkDate);
    }

    /**
     * 保存尿液模型预测结果
     */
    private void saveUrinePrediction(Integer userId, LocalDate checkDate, LocalDateTime predictionTime,
                                     Map<String, Object> predictionData) throws Exception {

        HealthUrine healthUrine = healthUrineRepository.findByUserIdAndCheckDate(userId, checkDate)
                .orElseGet(() -> {
                    HealthUrine newUrine = new HealthUrine();
                    newUrine.setUserId(userId);
                    newUrine.setCheckDate(checkDate);
                    return newUrine;
                });

        logger.info("保存尿液模型预测结果");

        healthUrine.setPredictionResult(getStringValue(predictionData, "prediction_label"));
        healthUrine.setRiskProbability(getDoubleValue(predictionData, "risk_probability"));
        healthUrine.setRiskScore(getDoubleValue(predictionData, "risk_score"));
        healthUrine.setRiskLevel(getStringValue(predictionData, "risk_level"));
        healthUrine.setKeyIndicatorCount(getIntegerValue(predictionData, "infection_indicators"));
        healthUrine.setRecommendation(getStringValue(predictionData, "recommendation"));
        healthUrine.setModelConfidence(getDoubleValue(predictionData, "model_confidence"));
        healthUrine.setPredictionTime(predictionTime);

        // 保存关键指标
        saveJsonField(predictionData, "key_findings", healthUrine::setKeyIndicators);

        healthUrineRepository.save(healthUrine);
        logger.info("尿液模型预测结果保存成功，用户ID: {}，检查日期: {}", userId, checkDate);
    }

    /**
     * 保存肾功能模型预测结果
     */
    private void saveKidneyPrediction(Integer userId, LocalDate checkDate, LocalDateTime predictionTime,
                                      Map<String, Object> predictionData) throws Exception {

        HealthKidney healthKidney = healthKidneyRepository.findByUserIdAndCheckDate(userId, checkDate)
                .orElseGet(() -> {
                    HealthKidney newKidney = new HealthKidney();
                    newKidney.setUserId(userId);
                    newKidney.setCheckDate(checkDate);
                    return newKidney;
                });

        logger.info("保存肾功能模型预测结果");

        healthKidney.setPredictionResult(getStringValue(predictionData, "prediction_label"));
        healthKidney.setRiskProbability(getDoubleValue(predictionData, "risk_probability"));
        healthKidney.setRiskScore(getDoubleValue(predictionData, "risk_score"));
        healthKidney.setRiskLevel(getStringValue(predictionData, "risk_level"));
        healthKidney.setAbnormalCount(getIntegerValue(predictionData, "total_abnormal_count"));
        healthKidney.setRecommendation(getStringValue(predictionData, "recommendation"));
        healthKidney.setModelConfidence(getDoubleValue(predictionData, "model_confidence"));
        healthKidney.setPredictionTime(predictionTime);

        // 保存异常指标和诊断假设
        saveJsonField(predictionData, "abnormal_indicators", healthKidney::setAbnormalIndicators);
        saveJsonField(predictionData, "diagnosis_hypotheses", healthKidney::setDiagnosisHypotheses);

        healthKidneyRepository.save(healthKidney);
        logger.info("肾功能模型预测结果保存成功，用户ID: {}，检查日期: {}", userId, checkDate);
    }

    /**
     * 保存肝功能模型预测结果
     */
    private void saveLiverPrediction(Integer userId, LocalDate checkDate, LocalDateTime predictionTime,
                                     Map<String, Object> predictionData) throws Exception {

        HealthLiver healthLiver = healthLiverRepository.findByUserIdAndCheckDate(userId, checkDate)
                .orElseGet(() -> {
                    HealthLiver newLiver = new HealthLiver();
                    newLiver.setUserId(userId);
                    newLiver.setCheckDate(checkDate);
                    return newLiver;
                });

        logger.info("保存肝功能模型预测结果");

        healthLiver.setPredictionResult(getStringValue(predictionData, "prediction_label"));
        healthLiver.setRiskProbability(getDoubleValue(predictionData, "risk_probability"));
        healthLiver.setRiskScore(getDoubleValue(predictionData, "risk_score"));
        healthLiver.setRiskLevel(getStringValue(predictionData, "risk_level"));
        healthLiver.setAbnormalCount(getIntegerValue(predictionData, "abnormal_count"));
        healthLiver.setRecommendation(getStringValue(predictionData, "recommendation"));
        healthLiver.setModelConfidence(getDoubleValue(predictionData, "model_confidence"));
        healthLiver.setPredictionTime(predictionTime);

        // 保存异常指标和诊断假设
        saveJsonField(predictionData, "abnormal_indicators", healthLiver::setAbnormalIndicators);
        saveJsonField(predictionData, "diagnosis_hypotheses", healthLiver::setDiagnosisHypotheses);

        healthLiverRepository.save(healthLiver);
        logger.info("肝功能模型预测结果保存成功，用户ID: {}，检查日期: {}", userId, checkDate);
    }

    /**
     * 保存JSON字段的通用方法
     */
    private void saveJsonField(Map<String, Object> data, String key, java.util.function.Consumer<String> setter) {
        try {
            Object jsonData = data.get(key);
            if (jsonData != null) {
                String jsonString = objectMapper.writeValueAsString(jsonData);
                setter.accept(jsonString);
                logger.debug("保存JSON字段 {}: {}", key, jsonString);
            } else {
                // 如果字段不存在，设置为空数组
                setter.accept("[]");
            }
        } catch (Exception e) {
            logger.warn("保存JSON字段失败: {}", key, e);
            try {
                // 尝试保存为空数组
                setter.accept("[]");
            } catch (Exception ex) {
                logger.error("无法保存JSON字段: {}", key, ex);
            }
        }
    }

    // ============ 工具方法 ============

    /**
     * 检查是否有健康数据
     */
    private boolean hasHealthData(Integer userId, String checkDate) {
        logger.debug("检查用户 {} 在 {} 是否有健康数据", userId, checkDate);
        // 暂时返回true，实际应该检查数据库
        return true;
    }

    private int countSuccessResults(Map<String, Map<String, Object>> allResults) {
        int successCount = 0;
        for (Map<String, Object> result : allResults.values()) {
            if (Boolean.TRUE.equals(result.get("success"))) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 获取字符串值（支持空值安全）
     */
    private String getStringValue(Map<String, Object> data, String key) {
        if (data == null || key == null) return null;

        Object value = data.get(key);
        if (value == null) return null;

        // 如果是布尔值，转换为字符串
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "是" : "否";
        }

        return value.toString();
    }

    /**
     * 获取整数值（支持空值安全）
     */
    private Integer getIntegerValue(Map<String, Object> data, String key) {
        if (data == null || key == null) return null;

        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            return null;
        } catch (Exception e) {
            logger.warn("转换整数值失败: {} = {}", key, value);
            return null;
        }
    }

    /**
     * 获取浮点数值（支持空值安全）
     */
    private Double getDoubleValue(Map<String, Object> data, String key) {
        if (data == null || key == null) return null;

        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            return null;
        } catch (Exception e) {
            logger.warn("转换浮点数值失败: {} = {}", key, value);
            return null;
        }
    }
}