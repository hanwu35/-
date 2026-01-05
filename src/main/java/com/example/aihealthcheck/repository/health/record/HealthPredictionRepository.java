package com.example.aihealthcheck.repository.health.record;

import com.example.aihealthcheck.entity.health.record.HealthPrediction;
import com.example.aihealthcheck.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthPredictionRepository extends JpaRepository<HealthPrediction, Long> {

    @Query("SELECT hp FROM HealthPrediction hp WHERE hp.user.userId = :userId ORDER BY hp.checkDate DESC")
    List<HealthPrediction> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT hp FROM HealthPrediction hp WHERE hp.user.userId = :userId AND hp.checkDate = :checkDate")
    HealthPrediction findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT hp FROM HealthPrediction hp WHERE hp.user.userId = :userId AND hp.riskLevel = :riskLevel ORDER BY hp.checkDate DESC")
    List<HealthPrediction> findByUserIdAndRiskLevel(@Param("userId") Integer userId, @Param("riskLevel") String riskLevel);

    @Query("SELECT COUNT(hp) FROM HealthPrediction hp WHERE hp.user.userId = :userId AND hp.riskLevel = :riskLevel")
    long countByUserIdAndRiskLevel(@Param("userId") Integer userId, @Param("riskLevel") String riskLevel);

    @Query("SELECT COUNT(hp) > 0 FROM HealthPrediction hp WHERE hp.user.userId = :userId AND hp.checkDate = :checkDate")
    boolean existsByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);
}