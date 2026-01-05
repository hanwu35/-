package com.example.aihealthcheck.entity.health.record;

import com.example.aihealthcheck.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "health_predictions")
public class HealthPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    @Column(name = "prediction_score")
    private Double predictionScore;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "prediction_details", columnDefinition = "JSON")
    private String predictionDetails;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "prediction_time", insertable = false, updatable = false)
    private java.time.LocalDateTime predictionTime;

    // Getter和Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }

    public Double getPredictionScore() { return predictionScore; }
    public void setPredictionScore(Double predictionScore) { this.predictionScore = predictionScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getPredictionDetails() { return predictionDetails; }
    public void setPredictionDetails(String predictionDetails) { this.predictionDetails = predictionDetails; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public java.time.LocalDateTime getPredictionTime() { return predictionTime; }
}