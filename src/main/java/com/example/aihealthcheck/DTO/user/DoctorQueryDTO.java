package com.example.aihealthcheck.dto;

import lombok.Data;

@Data
public class DoctorQueryDTO {
    private String deptCode;      // 科室编码筛选，'all'表示全部
    private String sortBy;        // 排序方式：appointments, fee, name
    private Integer page = 1;     // 当前页，默认1
    private Integer pageSize = 6; // 每页大小，默认6
    private String doctorName;    // 医生姓名搜索（模糊）

    public DoctorQueryDTO() {}

    public DoctorQueryDTO(String deptCode, String sortBy, Integer page, Integer pageSize) {
        this.deptCode = deptCode;
        this.sortBy = sortBy;
        this.page = page != null ? page : 1;
        this.pageSize = pageSize != null ? pageSize : 6;
    }
}
