package com.example.aihealthcheck.repository.health.examination.Liver;

import com.example.aihealthcheck.entity.health.examination.Liver.HealthLiverItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthLiverItemRepository extends JpaRepository<HealthLiverItem, Long> {

    @Query("SELECT hli FROM HealthLiverItem hli WHERE hli.user.userId = :userId ORDER BY hli.checkDate DESC, hli.itemName")
    List<HealthLiverItem> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT hli FROM HealthLiverItem hli WHERE hli.user.userId = :userId AND hli.checkDate = :checkDate ORDER BY hli.itemName")
    List<HealthLiverItem> findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT DISTINCT hli.checkDate FROM HealthLiverItem hli WHERE hli.user.userId = :userId ORDER BY hli.checkDate DESC")
    List<LocalDate> findDistinctDatesByUserId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT hli.user.userId FROM HealthLiverItem hli WHERE hli.checkDate = :date")
    List<Integer> findDistinctUserIdsByDate(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT hli.user.userId FROM HealthLiverItem hli WHERE hli.createdAt BETWEEN :start AND :end")
    List<Integer> findDistinctUserIdsByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(hli) > 0 FROM HealthLiverItem hli WHERE hli.user.userId = :userId AND hli.checkDate = :checkDate")
    boolean existsByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    // 查找特定的肝功能项目
    @Query("SELECT hli FROM HealthLiverItem hli WHERE hli.user.userId = :userId AND hli.checkDate = :checkDate AND TRIM(hli.itemName) = TRIM(:itemName)")
    Optional<HealthLiverItem> findByUserIdAndCheckDateAndItemName(
        @Param("userId") Integer userId,
        @Param("checkDate") LocalDate checkDate,
        @Param("itemName") String itemName);

// 查找用户最新的肝功能项目
@Query("SELECT hli FROM HealthLiverItem hli WHERE hli.user.userId = :userId ORDER BY hli.checkDate DESC LIMIT 1")
Optional<HealthLiverItem> findTopByUserIdOrderByCheckDateDesc(@Param("userId") Integer userId);
}
