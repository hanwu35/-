package com.example.aihealthcheck.entity.health.examination.Blood;

import com.example.aihealthcheck.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_blood")
@Data
public class HealthBlood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    // ============ 血液指标 ============
    // 移除 precision 和 scale
    @Column(name = "hemoglobin")
    private Double hemoglobin;  // 血红蛋白 (g/L)

    @Column(name = "white_blood_cell")
    private Double whiteBloodCell;  // 白细胞计数 (×10⁹/L)

    @Column(name = "platelet")
    private Double platelet;  // 血小板计数 (×10⁹/L)

    @Column(name = "blood_glucose")
    private Double bloodGlucose;  // 血糖 (mmol/L)

    // ============ 预测结果 ============
    @Column(name = "prediction_result", length = 20)
    private String predictionResult;

    // 移除 precision 和 scale
    @Column(name = "abnormal_probability")
    private Double abnormalProbability;  // 异常概率 (%)

    @Column(name = "normal_probability")
    private Double normalProbability;  // 正常概率 (%)

    @Column(name = "abnormal_count")
    private Integer abnormalCount;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "model_confidence")
    private Double modelConfidence;  // 模型置信度 (%)

    @Column(name = "abnormal_indicators", columnDefinition = "JSON")
    private String abnormalIndicators;

    @Column(name = "model_version", length = 50)
    private String modelVersion = "1.0";

    @Column(name = "prediction_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime predictionTime;

    // ============ 时间戳 ============
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造方法
    public HealthBlood() {}

    public HealthBlood(Integer userId, LocalDate checkDate) {
        this.userId = userId;
        this.checkDate = checkDate;
    }
}