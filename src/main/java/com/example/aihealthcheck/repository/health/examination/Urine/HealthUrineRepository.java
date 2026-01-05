package com.example.aihealthcheck.repository.health.examination.Urine;

import com.example.aihealthcheck.entity.health.examination.Urine.HealthUrine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthUrineRepository extends JpaRepository<HealthUrine, Long> {

    // 基本查询

    Optional<HealthUrine> findByUserIdAndCheckDate(Integer userId, LocalDate checkDate);

    List<HealthUrine> findByUserId(Integer userId);

    List<HealthUrine> findByUserIdOrderByCheckDateDesc(Integer userId);

    // 按风险等级查询
    List<HealthUrine> findByRiskLevel(String riskLevel);

    List<HealthUrine> findByRiskLevelAndUserId(String riskLevel, Integer userId);

    // 按时间范围查询
    @Query("SELECT h FROM HealthUrine h WHERE h.userId = :userId AND h.checkDate BETWEEN :startDate AND :endDate")
    List<HealthUrine> findByUserIdAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 统计查询
    long countByUserId(Integer userId);

    long countByUserIdAndRiskLevel(Integer userId, String riskLevel);

    // 高风险查询
    @Query("SELECT h FROM HealthUrine h WHERE h.riskLevel = '高风险'")
    List<HealthUrine> findHighRiskRecords();

    // 检查是否存在预测
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN TRUE ELSE FALSE END FROM HealthUrine h WHERE h.userId = :userId AND h.checkDate = :checkDate AND h.predictionResult IS NOT NULL")
    boolean existsPrediction(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);
}