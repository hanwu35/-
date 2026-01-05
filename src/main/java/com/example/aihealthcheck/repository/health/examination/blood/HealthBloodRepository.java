package com.example.aihealthcheck.repository.health.examination.blood;

import com.example.aihealthcheck.entity.health.examination.Blood.HealthBlood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthBloodRepository extends JpaRepository<HealthBlood, Long> {

    // 基本查询

    Optional<HealthBlood> findByUserIdAndCheckDate(Integer userId, LocalDate checkDate);

    List<HealthBlood> findByUserId(Integer userId);

    List<HealthBlood> findByUserIdOrderByCheckDateDesc(Integer userId);

    // 按风险等级查询
    List<HealthBlood> findByRiskLevel(String riskLevel);

    List<HealthBlood> findByRiskLevelAndUserId(String riskLevel, Integer userId);

    // 按时间范围查询
    @Query("SELECT h FROM HealthBlood h WHERE h.userId = :userId AND h.checkDate BETWEEN :startDate AND :endDate")
    List<HealthBlood> findByUserIdAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 统计查询
    long countByUserId(Integer userId);

    long countByUserIdAndRiskLevel(Integer userId, String riskLevel);

    @Query("SELECT COUNT(h) FROM HealthBlood h WHERE h.predictionResult IS NOT NULL AND h.userId = :userId")
    long countPredictionsByUserId(@Param("userId") Integer userId);

    // 高风险查询
    @Query("SELECT h FROM HealthBlood h WHERE h.riskLevel = '高风险' OR h.predictionResult = '异常'")
    List<HealthBlood> findHighRiskRecords();

    @Query("SELECT h FROM HealthBlood h WHERE (h.riskLevel = '高风险' OR h.predictionResult = '异常') AND h.userId = :userId")
    List<HealthBlood> findHighRiskRecordsByUserId(@Param("userId") Integer userId);

    // 检查是否存在预测
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN TRUE ELSE FALSE END FROM HealthBlood h WHERE h.userId = :userId AND h.checkDate = :checkDate AND h.predictionResult IS NOT NULL")
    boolean existsPrediction(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    // 获取最新的预测
    @Query("SELECT h FROM HealthBlood h WHERE h.userId = :userId AND h.predictionResult IS NOT NULL ORDER BY h.checkDate DESC")
    List<HealthBlood> findLatestPredictionsByUserId(@Param("userId") Integer userId);
}