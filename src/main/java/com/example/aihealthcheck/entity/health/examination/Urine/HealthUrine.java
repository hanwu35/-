package com.example.aihealthcheck.entity.health.examination.Urine;

import com.example.aihealthcheck.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_urine")
@Data
public class HealthUrine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    // ============ 尿液指标 ============
    @Column(name = "urine_protein", length = 20)
    private String urineProtein;

    @Column(name = "urine_glucose", length = 20)
    private String urineGlucose;

    // 移除 precision 和 scale
    @Column(name = "urine_specific_gravity")
    private Double urineSpecificGravity;  // 尿比重

    @Column(name = "urine_ph")
    private Double urinePh;  // 尿PH

    @Column(name = "nitrite", length = 20)
    private String nitrite;

    @Column(name = "ketone", length = 20)
    private String ketone;

    @Column(name = "bilirubin", length = 20)
    private String bilirubin;

    @Column(name = "leukocyte_esterase", length = 20)
    private String leukocyteEsterase;

    @Column(name = "occult_blood", length = 20)
    private String occultBlood;

    @Column(name = "urobilinogen", length = 20)
    private String urobilinogen;

    @Column(name = "vitamin_c", length = 20)
    private String vitaminC;

    @Column(name = "microscopy", columnDefinition = "TEXT")
    private String microscopy;

    // ============ 预测结果 ============
    @Column(name = "prediction_result", length = 20)
    private String predictionResult;

    // 移除 precision 和 scale
    @Column(name = "risk_probability")
    private Double riskProbability;  // 风险概率 (%)

    @Column(name = "risk_score")
    private Double riskScore;  // 风险评分

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "key_indicator_count")
    private Integer keyIndicatorCount;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "model_confidence")
    private Double modelConfidence;  // 模型置信度 (%)

    @Column(name = "key_indicators", columnDefinition = "JSON")
    private String keyIndicators;

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
    public HealthUrine() {}

    public HealthUrine(Integer userId, LocalDate checkDate) {
        this.userId = userId;
        this.checkDate = checkDate;
    }
}