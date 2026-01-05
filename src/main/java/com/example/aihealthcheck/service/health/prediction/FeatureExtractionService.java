package com.example.aihealthcheck.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureExtractionService {

    @Autowired
    private HealthBloodItemRepository bloodItemRepository;

    @Autowired
    private HealthUrineItemRepository urineItemRepository;

    @Autowired
    private HealthLiverItemRepository liverItemRepository;

    @Autowired
    private HealthKidneyItemRepository kidneyItemRepository;

    @Autowired
    private HealthFeaturesRepository featuresRepository;

    @Autowired
    private ItemMappingService mappingService;

    @Autowired
    private ValueConversionService conversionService;

    /**
     * 为用户和日期提取特征向量
     */
    @Transactional
    public HealthFeatures extractFeaturesForUserAndDate(User user, LocalDate checkDate) {
        // 检查是否已存在特征向量
        HealthFeatures existingFeatures = featuresRepository.findByUserIdAndCheckDate(
            user.getUserId(), checkDate);

        if (existingFeatures != null) {
            System.out.println("特征向量已存在，用户ID: " + user.getUserId() + ", 日期: " + checkDate);
            return existingFeatures;
        }

        System.out.println("开始提取特征向量，用户ID: " + user.getUserId() + ", 日期: " + checkDate);

        try {
            // 创建新的特征向量
            HealthFeatures features = new HealthFeatures();
            features.setUser(user);
            features.setCheckDate(checkDate);

            // 设置患者基本信息
            setPatientBasicInfo(features, user, checkDate);

            // 提取各分类特征
            boolean hasBloodData = extractBloodFeatures(features, user, checkDate);
            boolean hasUrineData = extractUrineFeatures(features, user, checkDate);
            boolean hasLiverData = extractLiverFeatures(features, user, checkDate);
            boolean hasKidneyData = extractKidneyFeatures(features, user, checkDate);

            // 设置数据标志
            features.setHasBloodData(hasBloodData);
            features.setHasUrineData(hasUrineData);
            features.setHasLiverData(hasLiverData);
            features.setHasKidneyData(hasKidneyData);

            // 保存特征向量
            HealthFeatures savedFeatures = featuresRepository.save(features);
            System.out.println("特征向量保存成功，ID: " + savedFeatures.getId());
            
            return savedFeatures;

        } catch (Exception e) {
            System.err.println("提取特征向量失败，用户ID: " + user.getUserId() + ", 日期: " + checkDate + ", 错误: " + e.getMessage());
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 设置患者基本信息
     */
    private void setPatientBasicInfo(HealthFeatures features, User user, LocalDate checkDate) {
        // 从原始数据中获取年龄和性别信息
        List<HealthBloodItem> bloodItems = bloodItemRepository.findByUserIdAndCheckDate(
            user.getUserId(), checkDate);

        if (!bloodItems.isEmpty()) {
            HealthBloodItem firstItem = bloodItems.get(0);
            if (firstItem.getAge() != null) {
                features.setAge(firstItem.getAge());
            }
            if (firstItem.getGender() != null) {
                features.setGenderCode(convertGenderToCode(firstItem.getGender()));
            }
        }
    }

    /**
     * 提取血检特征
     */
    private boolean extractBloodFeatures(HealthFeatures features, User user, LocalDate checkDate) {
        List<HealthBloodItem> bloodItems = bloodItemRepository.findByUserIdAndCheckDate(
            user.getUserId(), checkDate);

        if (bloodItems.isEmpty()) {
            System.out.println("未找到血检数据，用户ID: " + user.getUserId() + ", 日期: " + checkDate);
            return false;
        }

        System.out.println("找到 " + bloodItems.size() + " 条血检数据");

        // 将项目映射到特征
        Map<String, Double> featureMap = new HashMap<>();
        for (HealthBloodItem item : bloodItems) {
            if (item.getFeatureName() != null && item.getNumericValue() != null) {
                featureMap.put(item.getFeatureName(), item.getNumericValue());
                System.out.println("血检特征映射: " + item.getFeatureName() + " = " + item.getNumericValue());
            }
        }

        if (featureMap.isEmpty()) {
            System.out.println("没有有效的血检特征数据");
            return false;
        }

        // 设置特征值 - 只设置数据库表中存在的字段
        features.setPlatelet(featureMap.get("platelet"));
        features.setWhiteBloodCell(featureMap.get("white_blood_cell"));
        features.setHemoglobin(featureMap.get("hemoglobin"));
        features.setRedBloodCell(featureMap.get("red_blood_cell"));
        features.setLymphocytePercentage(featureMap.get("lymphocyte_percentage"));
        features.setMonocytePercentage(featureMap.get("monocyte_percentage"));
        features.setEosinophilPercentage(featureMap.get("eosinophil_percentage"));
        features.setBasophilPercentage(featureMap.get("basophil_percentage"));
        features.setNeutrophilPercentage(featureMap.get("neutrophil_percentage"));

        // 注意：如果数据库中不存在 blood_glucose 字段，不要设置它
        // features.setBloodGlucose(featureMap.get("blood_glucose"));

        System.out.println("血检特征提取完成");
        return true;
    }

    /**
     * 提取尿检特征
     */
    private boolean extractUrineFeatures(HealthFeatures features, User user, LocalDate checkDate) {
        List<HealthUrineItem> urineItems = urineItemRepository.findByUserIdAndCheckDate(
            user.getUserId(), checkDate);

        if (urineItems.isEmpty()) {
            System.out.println("未找到尿检数据，用户ID: " + user.getUserId() + ", 日期: " + checkDate);
            return false;
        }

        Map<String, Double> featureMap = new HashMap<>();
        for (HealthUrineItem item : urineItems) {
            if (item.getFeatureName() != null && item.getNumericValue() != null) {
                featureMap.put(item.getFeatureName(), item.getNumericValue());
            }
        }

        if (featureMap.isEmpty()) {
            return false;
        }

        // 只设置存在的字段
        features.setUrineProtein(featureMap.get("urine_protein"));
        features.setUrineGlucose(featureMap.get("urine_glucose"));
        features.setUrineSpecificGravity(featureMap.get("urine_specific_gravity"));
        features.setNitrite(featureMap.get("nitrite"));
        features.setKetone(featureMap.get("ketone"));
        features.setBilirubin(featureMap.get("bilirubin"));
        features.setLeukocyteEsterase(featureMap.get("leukocyte_esterase"));
        features.setVitaminC(featureMap.get("vitamin_c"));
        features.setUrinePh(featureMap.get("urine_ph"));
        features.setOccultBlood(featureMap.get("occult_blood"));
        features.setUrobilinogen(featureMap.get("urobilinogen"));

        return true;
    }

    /**
     * 提取肝功能特征
     */
    private boolean extractLiverFeatures(HealthFeatures features, User user, LocalDate checkDate) {
        List<HealthLiverItem> liverItems = liverItemRepository.findByUserIdAndCheckDate(
            user.getUserId(), checkDate);

        if (liverItems.isEmpty()) {
            return false;
        }

        Map<String, Double> featureMap = new HashMap<>();
        for (HealthLiverItem item : liverItems) {
            if (item.getFeatureName() != null && item.getNumericValue() != null) {
                featureMap.put(item.getFeatureName(), item.getNumericValue());
            }
        }

        if (featureMap.isEmpty()) {
            return false;
        }

        features.setAlt(featureMap.get("alt"));
        features.setAst(featureMap.get("ast"));
        features.setTotalBilirubin(featureMap.get("total_bilirubin"));
        features.setDirectBilirubin(featureMap.get("direct_bilirubin"));
        features.setIndirectBilirubin(featureMap.get("indirect_bilirubin"));
        features.setAlbumin(featureMap.get("albumin"));
        features.setGlobulin(featureMap.get("globulin"));
        features.setTotalProtein(featureMap.get("total_protein"));
        features.setAgRatio(featureMap.get("ag_ratio"));
        features.setAstAltRatio(featureMap.get("ast_alt_ratio"));
        features.setGgt(featureMap.get("ggt"));
        features.setAlp(featureMap.get("alp"));

        return true;
    }

    /**
     * 提取肾功能特征
     */
    private boolean extractKidneyFeatures(HealthFeatures features, User user, LocalDate checkDate) {
        List<HealthKidneyItem> kidneyItems = kidneyItemRepository.findByUserIdAndCheckDate(
            user.getUserId(), checkDate);

        if (kidneyItems.isEmpty()) {
            return false;
        }

        Map<String, Double> featureMap = new HashMap<>();
        for (HealthKidneyItem item : kidneyItems) {
            if (item.getFeatureName() != null && item.getNumericValue() != null) {
                featureMap.put(item.getFeatureName(), item.getNumericValue());
            }
        }

        if (featureMap.isEmpty()) {
            return false;
        }

        features.setCreatinine(featureMap.get("creatinine"));
        features.setUreaNitrogen(featureMap.get("urea_nitrogen"));
        features.setUricAcid(featureMap.get("uric_acid"));

        return true;
    }

    /**
     * 转换性别为编码
     */
    private Integer convertGenderToCode(String gender) {
        if (gender == null) return null;

        return switch (gender.trim()) {
            case "男", "male", "M" -> 0;
            case "女", "female", "F" -> 1;
            default -> null;
        };
    }

    /**
     * 批量提取特征
     */
    @Transactional
    public int extractFeaturesForUser(User user) {
        try {
            System.out.println("开始批量提取特征，用户ID: " + user.getUserId());

            // 收集所有日期
            List<LocalDate> dates = bloodItemRepository.findDistinctDatesByUserId(user.getUserId());
            dates.addAll(urineItemRepository.findDistinctDatesByUserId(user.getUserId()));
            dates.addAll(liverItemRepository.findDistinctDatesByUserId(user.getUserId()));
            dates.addAll(kidneyItemRepository.findDistinctDatesByUserId(user.getUserId()));

            // 去重
            List<LocalDate> distinctDates = dates.stream().distinct().toList();

            System.out.println("找到 " + distinctDates.size() + " 个不同的检查日期");

            int count = 0;
            for (LocalDate date : distinctDates) {
                try {
                    HealthFeatures features = extractFeaturesForUserAndDate(user, date);
                    if (features != null) {
                        count++;
                        System.out.println("成功提取特征，日期: " + date + "，ID: " + features.getId());
                    }
                } catch (Exception e) {
                    System.err.println("提取特征失败，用户ID: " + user.getUserId() + ", 日期: " + date + ", 错误: " + e.getMessage());
                    // 继续处理下一个日期，不中断整个流程
                }
            }

            System.out.println("批量提取特征完成，共提取 " + count + " 个特征向量");
            return count;

        } catch (Exception e) {
            System.err.println("批量提取特征失败，用户ID: " + user.getUserId() + ", 错误: " + e.getMessage());
            return 0;
        }
    }
}