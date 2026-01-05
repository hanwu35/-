package com.example.aihealthcheck.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "item_mapping_config")
public class ItemMappingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chinese_names", nullable = false, length = 500)
    private String chineseNames;

    @Column(name = "feature_name", nullable = false, unique = true, length = 50)
    private String featureName;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type")
    private ValueType valueType = ValueType.numeric;

    @Column(name = "default_unit", length = 50)
    private String defaultUnit;

    @Column(name = "unit_conversion")
    private Double unitConversion = 1.0;

    @Column(name = "display_name", length = 100)
    private String displayName;

    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;

    // 枚举类型
    public enum Category {
        blood, urine, liver, kidney
    }

    public enum ValueType {
        numeric, text_positive
    }

    // Getter和Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getChineseNames() { return chineseNames; }
    public void setChineseNames(String chineseNames) { this.chineseNames = chineseNames; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public ValueType getValueType() { return valueType; }
    public void setValueType(ValueType valueType) { this.valueType = valueType; }

    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }

    public Double getUnitConversion() { return unitConversion; }
    public void setUnitConversion(Double unitConversion) { this.unitConversion = unitConversion; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}