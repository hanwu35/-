package com.example.aihealthcheck.repository.health.indicator;

import com.example.aihealthcheck.entity.health.indicator.HealthFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthFeaturesRepository extends JpaRepository<HealthFeatures, Long> {

    @Query("SELECT hf FROM HealthFeatures hf WHERE hf.user.userId = :userId ORDER BY hf.checkDate DESC")
    List<HealthFeatures> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT hf FROM HealthFeatures hf WHERE hf.user.userId = :userId AND hf.checkDate = :checkDate")
    HealthFeatures findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT hf FROM HealthFeatures hf WHERE hf.user.userId = :userId AND hf.hasBloodData = true ORDER BY hf.checkDate DESC")
    List<HealthFeatures> findBloodFeaturesByUserId(@Param("userId") Integer userId);

    @Query("SELECT hf FROM HealthFeatures hf WHERE hf.user.userId = :userId AND hf.hasUrineData = true ORDER BY hf.checkDate DESC")
    List<HealthFeatures> findUrineFeaturesByUserId(@Param("userId") Integer userId);

    @Query("SELECT hf FROM HealthFeatures hf WHERE hf.user.userId = :userId AND hf.hasLiverData = true ORDER BY hf.checkDate DESC")
    List<HealthFeatures> findLiverFeaturesByUserId(@Param("userId") Integer userId);

    @Query("SELECT hf FROM HealthFeatures hf WHERE hf.user.userId = :userId AND hf.hasKidneyData = true ORDER BY hf.checkDate DESC")
    List<HealthFeatures> findKidneyFeaturesByUserId(@Param("userId") Integer userId);

    @Query("SELECT COUNT(hf) > 0 FROM HealthFeatures hf WHERE hf.user.userId = :userId AND hf.checkDate = :checkDate")
    boolean existsByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT COUNT(hf) FROM HealthFeatures hf WHERE hf.user.userId = :userId")
    long countByUserId(@Param("userId") Integer userId);
}