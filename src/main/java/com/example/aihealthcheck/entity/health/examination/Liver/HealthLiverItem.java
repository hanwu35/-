package com.example.aihealthcheck.entity.health.examination.Liver;

import com.example.aihealthcheck.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "health_liver_items")
public class HealthLiverItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "card_number")
    private String cardNumber;

    private String gender;
    private Integer age;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "item_value")
    private String itemValue;

    private String unit;

    @Column(name = "feature_name")
    private String featureName;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    // Getter和Setter（与HealthBloodItem类似）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemValue() { return itemValue; }
    public void setItemValue(String itemValue) { this.itemValue = itemValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public Double getNumericValue() { return numericValue; }
    public void setNumericValue(Double numericValue) { this.numericValue = numericValue; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}