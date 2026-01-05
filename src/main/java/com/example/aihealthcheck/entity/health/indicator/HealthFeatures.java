package com.example.aihealthcheck.entity.health.indicator;

import com.example.aihealthcheck.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "health_features")
public class HealthFeatures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    // 患者基本信息
    private Integer age;
    @Column(name = "gender_code")
    private Integer genderCode; // 0=男，1=女

    // 血检特征
    private Double platelet;           // 血小板总数（10^9/L）
    @Column(name = "white_blood_cell")
    private Double whiteBloodCell;     // 白细胞总数（10^9/L）
    private Double hemoglobin;         // 血红蛋白浓度（g/L）
    @Column(name = "red_blood_cell")
    private Double redBloodCell;       // 红细胞总数（10^12/L）
    @Column(name = "lymphocyte_percentage")
    private Double lymphocytePercentage; // 淋巴细胞百分比（%）
    @Column(name = "monocyte_percentage")
    private Double monocytePercentage; // 单核细胞百分比（%）
    @Column(name = "eosinophil_percentage")
    private Double eosinophilPercentage; // 嗜酸性细胞百分比（%）
    @Column(name = "basophil_percentage")
    private Double basophilPercentage; // 嗜碱性细胞百分比（%）
    @Column(name = "neutrophil_percentage")
    private Double neutrophilPercentage; // 中性粒细胞百分比（%")

    // 尿检特征
    @Column(name = "urine_protein")
    private Double urineProtein;       // 尿蛋白
    @Column(name = "urine_glucose")
    private Double urineGlucose;       // 尿葡萄糖
    @Column(name = "urine_specific_gravity")
    private Double urineSpecificGravity; // 尿比重
    private Double nitrite;            // 亚硝酸盐
    private Double ketone;             // 尿酮体
    private Double bilirubin;          // 胆红素
    @Column(name = "leukocyte_esterase")
    private Double leukocyteEsterase;  // 白细胞脂酶
    @Column(name = "vitamin_c")
    private Double vitaminC;           // 维生素C
    @Column(name = "urine_ph")
    private Double urinePh;            // 尿PH
    @Column(name = "occult_blood")
    private Double occultBlood;        // 尿潜血
    private Double urobilinogen;       // 尿胆原

    // 肝功能特征
    private Double alt;                // 谷丙转氨酶（IU/L）
    private Double ast;                // 谷草转氨酶（IU/L）
    @Column(name = "total_bilirubin")
    private Double totalBilirubin;     // 总胆红素（umol/L）
    @Column(name = "direct_bilirubin")
    private Double directBilirubin;    // 直接胆红素（umol/L）
    @Column(name = "indirect_bilirubin")
    private Double indirectBilirubin;  // 间接胆红素（umol/L）
    private Double albumin;            // 白蛋白（g/L）
    private Double globulin;           // 球蛋白（g/L）
    @Column(name = "total_protein")
    private Double totalProtein;       // 总蛋白（g/L）
    @Column(name = "ag_ratio")
    private Double agRatio;            // A/G
    @Column(name = "ast_alt_ratio")
    private Double astAltRatio;        // 谷草/谷丙
    private Double ggt;                // 谷氨酰转酞酶（IU/L）
    private Double alp;                // 碱性磷酸酶（IU/L）

    // 肾功能特征
    private Double creatinine;         // 肌酐（umol/L）
    @Column(name = "urea_nitrogen")
    private Double ureaNitrogen;       // 尿素氮（mmol/L）
    @Column(name = "uric_acid")
    private Double uricAcid;           // 尿酸（umol/L）

    // 注意：暂时注释掉数据库中不存在的字段
    // 等数据库表结构更新后再取消注释
    // @Column(name = "blood_glucose")
    // private Double bloodGlucose;       // 血糖（mmol/L）

    // 状态标志
    @Column(name = "has_blood_data")
    private Boolean hasBloodData = false;

    @Column(name = "has_urine_data")
    private Boolean hasUrineData = false;

    @Column(name = "has_liver_data")
    private Boolean hasLiverData = false;

    @Column(name = "has_kidney_data")
    private Boolean hasKidneyData = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    // ==================== Getter和Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Integer getGenderCode() { return genderCode; }
    public void setGenderCode(Integer genderCode) { this.genderCode = genderCode; }

    // 血检特征
    public Double getPlatelet() { return platelet; }
    public void setPlatelet(Double platelet) { this.platelet = platelet; }

    public Double getWhiteBloodCell() { return whiteBloodCell; }
    public void setWhiteBloodCell(Double whiteBloodCell) { this.whiteBloodCell = whiteBloodCell; }

    public Double getHemoglobin() { return hemoglobin; }
    public void setHemoglobin(Double hemoglobin) { this.hemoglobin = hemoglobin; }

    public Double getRedBloodCell() { return redBloodCell; }
    public void setRedBloodCell(Double redBloodCell) { this.redBloodCell = redBloodCell; }

    public Double getLymphocytePercentage() { return lymphocytePercentage; }
    public void setLymphocytePercentage(Double lymphocytePercentage) { this.lymphocytePercentage = lymphocytePercentage; }

    public Double getMonocytePercentage() { return monocytePercentage; }
    public void setMonocytePercentage(Double monocytePercentage) { this.monocytePercentage = monocytePercentage; }

    public Double getEosinophilPercentage() { return eosinophilPercentage; }
    public void setEosinophilPercentage(Double eosinophilPercentage) { this.eosinophilPercentage = eosinophilPercentage; }

    public Double getBasophilPercentage() { return basophilPercentage; }
    public void setBasophilPercentage(Double basophilPercentage) { this.basophilPercentage = basophilPercentage; }

    public Double getNeutrophilPercentage() { return neutrophilPercentage; }
    public void setNeutrophilPercentage(Double neutrophilPercentage) { this.neutrophilPercentage = neutrophilPercentage; }

    // 尿检特征
    public Double getUrineProtein() { return urineProtein; }
    public void setUrineProtein(Double urineProtein) { this.urineProtein = urineProtein; }

    public Double getUrineGlucose() { return urineGlucose; }
    public void setUrineGlucose(Double urineGlucose) { this.urineGlucose = urineGlucose; }

    public Double getUrineSpecificGravity() { return urineSpecificGravity; }
    public void setUrineSpecificGravity(Double urineSpecificGravity) { this.urineSpecificGravity = urineSpecificGravity; }

    public Double getNitrite() { return nitrite; }
    public void setNitrite(Double nitrite) { this.nitrite = nitrite; }

    public Double getKetone() { return ketone; }
    public void setKetone(Double ketone) { this.ketone = ketone; }

    public Double getBilirubin() { return bilirubin; }
    public void setBilirubin(Double bilirubin) { this.bilirubin = bilirubin; }

    public Double getLeukocyteEsterase() { return leukocyteEsterase; }
    public void setLeukocyteEsterase(Double leukocyteEsterase) { this.leukocyteEsterase = leukocyteEsterase; }

    public Double getVitaminC() { return vitaminC; }
    public void setVitaminC(Double vitaminC) { this.vitaminC = vitaminC; }

    public Double getUrinePh() { return urinePh; }
    public void setUrinePh(Double urinePh) { this.urinePh = urinePh; }

    public Double getOccultBlood() { return occultBlood; }
    public void setOccultBlood(Double occultBlood) { this.occultBlood = occultBlood; }

    public Double getUrobilinogen() { return urobilinogen; }
    public void setUrobilinogen(Double urobilinogen) { this.urobilinogen = urobilinogen; }

    // 肝功能特征
    public Double getAlt() { return alt; }
    public void setAlt(Double alt) { this.alt = alt; }

    public Double getAst() { return ast; }
    public void setAst(Double ast) { this.ast = ast; }

    public Double getTotalBilirubin() { return totalBilirubin; }
    public void setTotalBilirubin(Double totalBilirubin) { this.totalBilirubin = totalBilirubin; }

    public Double getDirectBilirubin() { return directBilirubin; }
    public void setDirectBilirubin(Double directBilirubin) { this.directBilirubin = directBilirubin; }

    public Double getIndirectBilirubin() { return indirectBilirubin; }
    public void setIndirectBilirubin(Double indirectBilirubin) { this.indirectBilirubin = indirectBilirubin; }

    public Double getAlbumin() { return albumin; }
    public void setAlbumin(Double albumin) { this.albumin = albumin; }

    public Double getGlobulin() { return globulin; }
    public void setGlobulin(Double globulin) { this.globulin = globulin; }

    public Double getTotalProtein() { return totalProtein; }
    public void setTotalProtein(Double totalProtein) { this.totalProtein = totalProtein; }

    public Double getAgRatio() { return agRatio; }
    public void setAgRatio(Double agRatio) { this.agRatio = agRatio; }

    public Double getAstAltRatio() { return astAltRatio; }
    public void setAstAltRatio(Double astAltRatio) { this.astAltRatio = astAltRatio; }

    public Double getGgt() { return ggt; }
    public void setGgt(Double ggt) { this.ggt = ggt; }

    public Double getAlp() { return alp; }
    public void setAlp(Double alp) { this.alp = alp; }

    // 肾功能特征
    public Double getCreatinine() { return creatinine; }
    public void setCreatinine(Double creatinine) { this.creatinine = creatinine; }

    public Double getUreaNitrogen() { return ureaNitrogen; }
    public void setUreaNitrogen(Double ureaNitrogen) { this.ureaNitrogen = ureaNitrogen; }

    public Double getUricAcid() { return uricAcid; }
    public void setUricAcid(Double uricAcid) { this.uricAcid = uricAcid; }

    // 血糖特征（暂时注释掉）
    // public Double getBloodGlucose() { return bloodGlucose; }
    // public void setBloodGlucose(Double bloodGlucose) { this.bloodGlucose = bloodGlucose; }

    // 状态标志
    public Boolean getHasBloodData() { return hasBloodData; }
    public void setHasBloodData(Boolean hasBloodData) { this.hasBloodData = hasBloodData; }

    public Boolean getHasUrineData() { return hasUrineData; }
    public void setHasUrineData(Boolean hasUrineData) { this.hasUrineData = hasUrineData; }

    public Boolean getHasLiverData() { return hasLiverData; }
    public void setHasLiverData(Boolean hasLiverData) { this.hasLiverData = hasLiverData; }

    public Boolean getHasKidneyData() { return hasKidneyData; }
    public void setHasKidneyData(Boolean hasKidneyData) { this.hasKidneyData = hasKidneyData; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }

    // 辅助方法
    public boolean hasAnyData() {
        return Boolean.TRUE.equals(hasBloodData) ||
               Boolean.TRUE.equals(hasUrineData) ||
               Boolean.TRUE.equals(hasLiverData) ||
               Boolean.TRUE.equals(hasKidneyData);
    }

    @Override
    public String toString() {
        return "HealthFeatures{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", checkDate=" + checkDate +
                ", hasBloodData=" + hasBloodData +
                ", hasUrineData=" + hasUrineData +
                ", hasLiverData=" + hasLiverData +
                ", hasKidneyData=" + hasKidneyData +
                '}';
    }
}