package com.example.aihealthcheck.repository.health.examination.Kindey;

import com.example.aihealthcheck.entity.health.examination.Kindey.HealthKidney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthKidneyRepository extends JpaRepository<HealthKidney, Long> {


    Optional<HealthKidney> findByUserIdAndCheckDate(Integer userId, LocalDate checkDate);

    List<HealthKidney> findByUserId(Integer userId);

    List<HealthKidney> findByUserIdOrderByCheckDateDesc(Integer userId);

    List<HealthKidney> findByRiskLevel(String riskLevel);

    @Query("SELECT h FROM HealthKidney h WHERE h.userId = :userId AND h.checkDate BETWEEN :startDate AND :endDate")
    List<HealthKidney> findByUserIdAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN TRUE ELSE FALSE END FROM HealthKidney h WHERE h.userId = :userId AND h.checkDate = :checkDate AND h.predictionResult IS NOT NULL")
    boolean existsPrediction(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);
}