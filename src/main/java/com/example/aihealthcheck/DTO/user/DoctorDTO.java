package com.example.aihealthcheck.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DoctorDTO {
    private Integer doctorId;
    private String doctorCode;
    private String name;
    private Integer deptId;
    private String deptCode;
    private String deptName;
    private String level;
    private String title;
    private BigDecimal normalFee;
    private BigDecimal urgentFee;
    private Integer appointments;
    private Double rating;
    private Boolean hasScheduleToday;

    public DoctorDTO() {}

    // 构造函数用于实体转换
    public DoctorDTO(Integer doctorId, String doctorCode, String name,
                    Integer deptId, String deptCode, String deptName,
                    String level, BigDecimal normalFee) {
        this.doctorId = doctorId;
        this.doctorCode = doctorCode;
        this.name = name;
        this.deptId = deptId;
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.level = level;
        this.title = "普通".equals(level) ? "普通医师" : "专家医师";
        this.normalFee = normalFee;
        this.urgentFee = normalFee.add(BigDecimal.valueOf(5.00)); // 加急费比普通费贵5元
        this.appointments = generateRandomAppointments();
        this.rating = generateRandomRating();
        this.hasScheduleToday = null;
    }

    private Integer generateRandomAppointments() {
        return 50 + (int)(Math.random() * 251); // 50-300之间的随机数
    }

    private Double generateRandomRating() {
        return 4.6 + Math.random() * 0.3; // 4.6-4.9之间的随机数
    }
}
