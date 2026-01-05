package com.example.aihealthcheck.repository.health.indicator;

import com.example.aihealthcheck.entity.health.indicator.HealthIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthIndicatorRepository extends JpaRepository<HealthIndicator, Long> {
    
    @Query("SELECT hi FROM HealthIndicator hi WHERE hi.user.userId = :userId ORDER BY hi.checkDate DESC")
    List<HealthIndicator> findByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT hi FROM HealthIndicator hi WHERE hi.user.userId = :userId AND hi.checkDate = :checkDate")
    HealthIndicator findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);
    
    @Query("SELECT COUNT(hi) FROM HealthIndicator hi WHERE hi.user.userId = :userId")
    long countByUserId(@Param("userId") Integer userId);
}