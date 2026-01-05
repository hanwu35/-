package com.example.aihealthcheck.repository.health.examination.Kindey;

import com.example.aihealthcheck.entity.health.examination.Kindey.HealthKidneyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthKidneyItemRepository extends JpaRepository<HealthKidneyItem, Long> {

    @Query("SELECT hki FROM HealthKidneyItem hki WHERE hki.user.userId = :userId ORDER BY hki.checkDate DESC, hki.itemName")
    List<HealthKidneyItem> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT hki FROM HealthKidneyItem hki WHERE hki.user.userId = :userId AND hki.checkDate = :checkDate ORDER BY hki.itemName")
    List<HealthKidneyItem> findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT DISTINCT hki.checkDate FROM HealthKidneyItem hki WHERE hki.user.userId = :userId ORDER BY hki.checkDate DESC")
    List<LocalDate> findDistinctDatesByUserId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT hki.user.userId FROM HealthKidneyItem hki WHERE hki.checkDate = :date")
    List<Integer> findDistinctUserIdsByDate(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT hki.user.userId FROM HealthKidneyItem hki WHERE hki.createdAt BETWEEN :start AND :end")
    List<Integer> findDistinctUserIdsByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(hki) > 0 FROM HealthKidneyItem hki WHERE hki.user.userId = :userId AND hki.checkDate = :checkDate")
    boolean existsByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    // 查找特定的肾功能项目
    @Query("SELECT hki FROM HealthKidneyItem hki WHERE hki.user.userId = :userId AND hki.checkDate = :checkDate AND TRIM(hki.itemName) = TRIM(:itemName)")
    Optional<HealthKidneyItem> findByUserIdAndCheckDateAndItemName(
        @Param("userId") Integer userId,
        @Param("checkDate") LocalDate checkDate,
        @Param("itemName") String itemName);

// 查找用户最新的肾功能项目
@Query("SELECT hki FROM HealthKidneyItem hki WHERE hki.user.userId = :userId ORDER BY hki.checkDate DESC LIMIT 1")
Optional<HealthKidneyItem> findTopByUserIdOrderByCheckDateDesc(@Param("userId") Integer userId);
}
