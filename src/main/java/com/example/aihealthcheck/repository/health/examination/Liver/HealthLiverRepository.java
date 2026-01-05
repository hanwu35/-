package com.example.aihealthcheck.repository.health.examination.Liver;

import com.example.aihealthcheck.entity.health.examination.Liver.HealthLiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthLiverRepository extends JpaRepository<HealthLiver, Long> {


    Optional<HealthLiver> findByUserIdAndCheckDate(Integer userId, LocalDate checkDate);

    List<HealthLiver> findByUserId(Integer userId);

    List<HealthLiver> findByUserIdOrderByCheckDateDesc(Integer userId);

    List<HealthLiver> findByRiskLevel(String riskLevel);

    @Query("SELECT h FROM HealthLiver h WHERE h.userId = :userId AND h.checkDate BETWEEN :startDate AND :endDate")
    List<HealthLiver> findByUserIdAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN TRUE ELSE FALSE END FROM HealthLiver h WHERE h.userId = :userId AND h.checkDate = :checkDate AND h.predictionResult IS NOT NULL")
    boolean existsPrediction(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);
}