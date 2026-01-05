package com.example.aihealthcheck.entity.health.examination.Liver;

import com.example.aihealthcheck.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_liver")
@Data
public class HealthLiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    // ============ 肝功能指标 ============
    // 移除 precision 和 scale
    @Column(name = "alt")
    private Double alt;  // 谷丙转氨酶 (U/L)

    @Column(name = "ast")
    private Double ast;  // 谷草转氨酶 (U/L)

    @Column(name = "total_bilirubin")
    private Double totalBilirubin;  // 总胆红素 (μmol/L)

    @Column(name = "direct_bilirubin")
    private Double directBilirubin;  // 直接胆红素 (μmol/L)

    @Column(name = "indirect_bilirubin")
    private Double indirectBilirubin;  // 间接胆红素 (μmol/L)

    @Column(name = "albumin")
    private Double albumin;  // 白蛋白 (g/L)

    @Column(name = "globulin")
    private Double globulin;  // 球蛋白 (g/L)

    @Column(name = "total_protein")
    private Double totalProtein;  // 总蛋白 (g/L)

    @Column(name = "ag_ratio")
    private Double agRatio;  // A/G

    @Column(name = "ggt")
    private Double ggt;  // 谷氨酰转酞酶 (IU/L)

    @Column(name = "alp")
    private Double alp;  // 碱性磷酸酶 (IU/L)

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

    @Column(name = "abnormal_count")
    private Integer abnormalCount;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "model_confidence")
    private Double modelConfidence;  // 模型置信度 (%)

    @Column(name = "abnormal_indicators", columnDefinition = "JSON")
    private String abnormalIndicators;

    @Column(name = "diagnosis_hypotheses", columnDefinition = "JSON")
    private String diagnosisHypotheses;

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
    public HealthLiver() {}

    public HealthLiver(Integer userId, LocalDate checkDate) {
        this.userId = userId;
        this.checkDate = checkDate;
    }
}