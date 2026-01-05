package com.example.aihealthcheck.repository.health.record;

import com.example.aihealthcheck.entity.health.record.HealthRecord;
import com.example.aihealthcheck.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    // 方法1：通过 User 对象查询（保持原样）
    List<HealthRecord> findByUserOrderByCreatedAtDesc(User user);

    // 方法2：通过 userId 查询 - 使用正确的属性名 user.userId
    @Query("SELECT hr FROM HealthRecord hr WHERE hr.user.userId = :userId ORDER BY hr.createdAt DESC")
    List<HealthRecord> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    // 方法3：日期范围查询 - 同样需要修复 user.id 为 user.userId
    @Query("SELECT hr FROM HealthRecord hr WHERE hr.user.userId = :userId AND hr.checkDate BETWEEN :startDate AND :endDate ORDER BY hr.checkDate DESC")
    List<HealthRecord> findByUserIdAndDateRange(@Param("userId") Integer userId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // 可选：添加通过用户账号查询的方法
    @Query("SELECT hr FROM HealthRecord hr WHERE hr.user.account = :account ORDER BY hr.createdAt DESC")
    List<HealthRecord> findByUserAccountOrderByCreatedAtDesc(@Param("account") String account);

    // 可选：统计用户的健康记录数量
    @Query("SELECT COUNT(hr) FROM HealthRecord hr WHERE hr.user.userId = :userId")
    long countByUserId(@Param("userId") Integer userId);
}