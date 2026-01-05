package com.example.aihealthcheck.entity.health.indicator;

import com.example.aihealthcheck.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "health_indicators")
public class HealthIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    @Column(name = "has_blood_data")
    private Boolean hasBloodData = false;

    @Column(name = "has_urine_data")
    private Boolean hasUrineData = false;

    @Column(name = "has_liver_data")
    private Boolean hasLiverData = false;

    @Column(name = "has_kidney_data")
    private Boolean hasKidneyData = false;

    @Column(name = "upload_source")
    private String uploadSource = "excel_upload";

    @Column(name = "created_at")
    private LocalDate createdAt;

    // 构造函数
    public HealthIndicator() {
        this.createdAt = LocalDate.now();
    }

    // Getter和Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }

    public Boolean getHasBloodData() { return hasBloodData; }
    public void setHasBloodData(Boolean hasBloodData) { this.hasBloodData = hasBloodData; }

    public Boolean getHasUrineData() { return hasUrineData; }
    public void setHasUrineData(Boolean hasUrineData) { this.hasUrineData = hasUrineData; }

    public Boolean getHasLiverData() { return hasLiverData; }
    public void setHasLiverData(Boolean hasLiverData) { this.hasLiverData = hasLiverData; }

    public Boolean getHasKidneyData() { return hasKidneyData; }
    public void setHasKidneyData(Boolean hasKidneyData) { this.hasKidneyData = hasKidneyData; }

    public String getUploadSource() { return uploadSource; }
    public void setUploadSource(String uploadSource) { this.uploadSource = uploadSource; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    // 辅助方法
    public boolean hasAnyData() {
        return Boolean.TRUE.equals(hasBloodData) ||
               Boolean.TRUE.equals(hasUrineData) ||
               Boolean.TRUE.equals(hasLiverData) ||
               Boolean.TRUE.equals(hasKidneyData);
    }
}