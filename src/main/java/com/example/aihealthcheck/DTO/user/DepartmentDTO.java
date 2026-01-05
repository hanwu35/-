package com.example.aihealthcheck.dto;

import lombok.Data;

@Data
public class DepartmentDTO {
    private Integer deptId;
    private String deptCode;
    private String deptName;
    private String deptType;
    private String introduction;

    public DepartmentDTO() {}

    public DepartmentDTO(Integer deptId, String deptCode, String deptName, String deptType) {
        this.deptId = deptId;
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.deptType = deptType;
    }
}