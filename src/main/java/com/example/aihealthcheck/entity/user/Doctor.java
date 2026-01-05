package com.example.aihealthcheck.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")
    private Integer doctorId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "doctor_code", nullable = false, unique = true, length = 20)
    private String doctorCode;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "normal_fee", precision = 10, scale = 2)
    private BigDecimal normalFee = BigDecimal.valueOf(10.00);

    @Column(name = "urgent_fee", precision = 10, scale = 2)
    private BigDecimal urgentFee = BigDecimal.valueOf(15.00);

    // Constructors
    public Doctor() {
    }

    public Doctor(String doctorCode, String realName, Department department, String level) {
        this.doctorCode = doctorCode;
        this.realName = realName;
        this.department = department;
        this.level = level;
    }

    // Getters and Setters
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public BigDecimal getNormalFee() {
        return normalFee;
    }

    public void setNormalFee(BigDecimal normalFee) {
        this.normalFee = normalFee;
    }

    public BigDecimal getUrgentFee() {
        return urgentFee;
    }

    public void setUrgentFee(BigDecimal urgentFee) {
        this.urgentFee = urgentFee;
    }
}