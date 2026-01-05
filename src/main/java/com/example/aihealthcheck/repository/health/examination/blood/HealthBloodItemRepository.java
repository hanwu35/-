package com.example.aihealthcheck.repository.health.examination.blood;

import com.example.aihealthcheck.entity.health.examination.Blood.HealthBloodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthBloodItemRepository extends JpaRepository<HealthBloodItem, Long> {

    @Query("SELECT hbi FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId ORDER BY hbi.checkDate DESC, hbi.itemName")
    List<HealthBloodItem> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT hbi FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId AND hbi.checkDate = :checkDate ORDER BY hbi.itemName")
    List<HealthBloodItem> findByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    @Query("SELECT DISTINCT hbi.checkDate FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId ORDER BY hbi.checkDate DESC")
    List<LocalDate> findDistinctDatesByUserId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT hbi.user.userId FROM HealthBloodItem hbi WHERE hbi.checkDate = :date")
    List<Integer> findDistinctUserIdsByDate(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT hbi.user.userId FROM HealthBloodItem hbi WHERE hbi.createdAt BETWEEN :start AND :end")
    List<Integer> findDistinctUserIdsByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT hbi FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId AND hbi.featureName = :featureName ORDER BY hbi.checkDate DESC")
    List<HealthBloodItem> findByUserIdAndFeatureName(@Param("userId") Integer userId, @Param("featureName") String featureName);

    @Query("SELECT COUNT(DISTINCT hbi.checkDate) FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId")
    long countDistinctDatesByUserId(@Param("userId") Integer userId);

    @Query("SELECT COUNT(hbi) > 0 FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId AND hbi.checkDate = :checkDate")
    boolean existsByUserIdAndCheckDate(@Param("userId") Integer userId, @Param("checkDate") LocalDate checkDate);

    // 查找特定的血检项目
    @Query("SELECT hbi FROM HealthBloodItem hbi " +
           "WHERE hbi.user.userId = :userId " +
           "AND hbi.checkDate = :checkDate " +
           "AND TRIM(hbi.itemName) = TRIM(:itemName)")
    Optional<HealthBloodItem> findByUserIdAndCheckDateAndItemName(
        @Param("userId") Integer userId,
        @Param("checkDate") LocalDate checkDate,
        @Param("itemName") String itemName);

    // 查找用户最新的血检项目
@Query("SELECT hbi FROM HealthBloodItem hbi WHERE hbi.user.userId = :userId ORDER BY hbi.checkDate DESC LIMIT 1")
Optional<HealthBloodItem> findTopByUserIdOrderByCheckDateDesc(@Param("userId") Integer userId);
}
