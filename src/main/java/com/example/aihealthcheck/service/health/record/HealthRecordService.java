package com.example.aihealthcheck.service;

import com.example.aihealthcheck.entity.User;
import com.example.aihealthcheck.entity.health.record.HealthRecord;
import com.example.aihealthcheck.repository.health.record.HealthRecordRepository;
import com.example.aihealthcheck.service.prediction.ModelPredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HealthRecordService {

    private static final Logger logger = LoggerFactory.getLogger(HealthRecordService.class);

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private ModelPredictionService modelPredictionService;

    // ============ 新增缺失的方法 ============

    /**
     * 查询所有健康记录
     */
    public List<HealthRecord> findAll() {
        logger.info("查询所有健康记录");
        return healthRecordRepository.findAll();
    }

    /**
     * 根据ID查询健康记录
     */
    public Optional<HealthRecord> findById(Long id) {
        logger.info("根据ID查询健康记录，ID: {}", id);
        return healthRecordRepository.findById(id);
    }

    @Transactional
    public HealthRecord save(HealthRecord healthRecord) {
        logger.info("开始保存健康记录: {}", healthRecord);

        // 如果创建时间为空，设置为当前时间
        if (healthRecord.getCreatedAt() == null) {
            healthRecord.setCreatedAt(LocalDate.now());
            logger.debug("设置创建时间为当前时间: {}", healthRecord.getCreatedAt());
        }

        // 保存健康记录
        HealthRecord savedRecord = healthRecordRepository.save(healthRecord);
        logger.info("健康记录保存成功，ID: {}", savedRecord.getId());

        // 触发模型预测（异步）
        if (healthRecord.getUser() != null && healthRecord.getCheckDate() != null) {
            Integer userId = healthRecord.getUser().getUserId();
            String checkDate = healthRecord.getCheckDate().toString();

            logger.info("准备触发模型预测，用户ID: {}，检查日期: {}", userId, checkDate);

            try {
                // 异步触发所有模型预测
                triggerModelPredictions(userId, checkDate);
                logger.info("已触发模型预测任务");
            } catch (Exception e) {
                logger.error("触发模型预测失败", e);
            }
        } else {
            logger.warn("健康记录缺少用户或检查日期信息，跳过预测");
        }

        return savedRecord;
    }

    @Async
    private void triggerModelPredictions(Integer userId, String checkDate) {
        logger.info("开始执行模型预测，用户ID: {}，检查日期: {}", userId, checkDate);
        try {
            modelPredictionService.triggerAllModelPredictions(userId, checkDate);
            logger.info("模型预测任务提交成功");
        } catch (Exception e) {
            logger.error("模型预测执行失败", e);
        }
    }


    public void deleteById(Long id) {
        logger.info("删除健康记录，ID: {}", id);
        healthRecordRepository.deleteById(id);
    }

    // ============ 查询方法 ============

    public List<HealthRecord> findByUser(User user) {
        logger.info("根据用户查询健康记录，用户ID: {}", user.getUserId());
        return healthRecordRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<HealthRecord> findByUserId(Integer userId) {
        logger.info("根据用户ID查询健康记录，用户ID: {}", userId);
        return healthRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<HealthRecord> findByUserIdAndDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        logger.info("根据用户ID和时间范围查询健康记录，用户ID: {}, 开始日期: {}, 结束日期: {}",
                   userId, startDate, endDate);
        return healthRecordRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public List<HealthRecord> findByUserAccount(String account) {
        logger.info("根据用户账号查询健康记录，账号: {}", account);
        return healthRecordRepository.findByUserAccountOrderByCreatedAtDesc(account);
    }

    // ============ 统计方法 ============

    public long countByUserId(Integer userId) {
        logger.debug("统计用户健康记录数量，用户ID: {}", userId);
        return healthRecordRepository.countByUserId(userId);
    }

    // ============ 业务方法 ============

    @Transactional
    public HealthRecord createHealthRecord(Integer userId, HealthRecord healthRecord) {
        logger.info("创建健康记录，用户ID: {}", userId);

        // 这个方法需要在 Controller 中调用 UserService 来获取 User 对象
        // 这里只设置 createdAt
        if (healthRecord.getCreatedAt() == null) {
            healthRecord.setCreatedAt(LocalDate.now());
        }

        // 保存健康记录
        HealthRecord savedRecord = healthRecordRepository.save(healthRecord);

        // 触发模型预测（异步）
        if (healthRecord.getCheckDate() != null) {
            String checkDate = healthRecord.getCheckDate().toString();

            try {
                // 异步触发所有模型预测
                triggerModelPredictions(userId, checkDate);
                logger.info("已触发模型预测，用户ID: {}，检查日期: {}", userId, checkDate);
            } catch (Exception e) {
                logger.error("触发模型预测失败: {}", e.getMessage());
                // 不抛出异常，避免影响主流程
            }
        }

        return savedRecord;
    }

    public List<HealthRecord> getRecentHealthRecords(Integer userId, int limit) {
        logger.info("获取最近健康记录，用户ID: {}, 限制数量: {}", userId, limit);
        List<HealthRecord> records = healthRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return records.stream().limit(limit).toList();
    }

    public boolean existsByUserId(Integer userId) {
        logger.debug("检查用户是否存在健康记录，用户ID: {}", userId);
        return countByUserId(userId) > 0;
    }

    public void deleteByUserId(Integer userId) {
        logger.info("删除用户所有健康记录，用户ID: {}", userId);
        List<HealthRecord> records = healthRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (HealthRecord record : records) {
            healthRecordRepository.deleteById(record.getId());
        }
    }

    // ============ 更新方法 ============

    public Optional<HealthRecord> updateHealthRecord(Long recordId, HealthRecord healthRecordDetails) {
        logger.info("更新健康记录，记录ID: {}", recordId);
        Optional<HealthRecord> existingRecord = healthRecordRepository.findById(recordId);
        if (existingRecord.isPresent()) {
            HealthRecord record = existingRecord.get();

            // 更新字段
            if (healthRecordDetails.getCheckDate() != null) {
                record.setCheckDate(healthRecordDetails.getCheckDate());
            }
            if (healthRecordDetails.getHeight() != null) {
                record.setHeight(healthRecordDetails.getHeight());
            }
            if (healthRecordDetails.getWeight() != null) {
                record.setWeight(healthRecordDetails.getWeight());
            }
            if (healthRecordDetails.getBloodPressure() != null) {
                record.setBloodPressure(healthRecordDetails.getBloodPressure());
            }
            if (healthRecordDetails.getHeartRate() != null) {
                record.setHeartRate(healthRecordDetails.getHeartRate());
            }
            if (healthRecordDetails.getSymptoms() != null) {
                record.setSymptoms(healthRecordDetails.getSymptoms());
            }
            if (healthRecordDetails.getAiAnalysis() != null) {
                record.setAiAnalysis(healthRecordDetails.getAiAnalysis());
            }

            HealthRecord updatedRecord = healthRecordRepository.save(record);
            logger.info("健康记录更新成功，ID: {}", updatedRecord.getId());
            return Optional.of(updatedRecord);
        }
        logger.warn("未找到要更新的健康记录，ID: {}", recordId);
        return Optional.empty();
    }
}



