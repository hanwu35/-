package com.example.aihealthcheck.repository.health.examination.Urine;

import com.example.aihealthcheck.entity.health.examination.Urine.HealthUrineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthUrineItemRepository extends JpaRepository<HealthUrineItem, Long> {

    @Query("SELECT hui FROM HealthUrineItem hui WHERE hui.user.userId = :userId ORDER BY hui.checkDate DESC, hui.itemName")
    List<HealthUrineItem> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT hui FROM HealthUrineItem hui WHERE hui.user.userId = :userId AND hui.checkDate = :checkDate ORDER BY hui.itemName")
    List<HealthUrineItem> findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT DISTINCT hui.checkDate FROM HealthUrineItem hui WHERE hui.user.userId = :userId ORDER BY hui.checkDate DESC")
    List<LocalDate> findDistinctDatesByUserId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT hui.user.userId FROM HealthUrineItem hui WHERE hui.checkDate = :date")
    List<Integer> findDistinctUserIdsByDate(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT hui.user.userId FROM HealthUrineItem hui WHERE hui.createdAt BETWEEN :start AND :end")
    List<Integer> findDistinctUserIdsByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(hui) > 0 FROM HealthUrineItem hui WHERE hui.user.userId = :userId AND hui.checkDate = :checkDate")
    boolean existsByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    // 查找特定的尿检项目
    @Query("SELECT hui FROM HealthUrineItem hui WHERE hui.user.userId = :userId AND hui.checkDate = :checkDate AND TRIM(hui.itemName) = TRIM(:itemName)")
    Optional<HealthUrineItem> findByUserIdAndCheckDateAndItemName(
        @Param("userId") Integer userId,
        @Param("checkDate") LocalDate checkDate,
        @Param("itemName") String itemName);

// 查找用户最新的尿检项目
@Query("SELECT hui FROM HealthUrineItem hui WHERE hui.user.userId = :userId ORDER BY hui.checkDate DESC LIMIT 1")
Optional<HealthUrineItem> findTopByUserIdOrderByCheckDateDesc(@Param("userId") Integer userId);
}
