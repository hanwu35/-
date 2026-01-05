package com.example.aihealthcheck.service;

import com.example.aihealthcheck.dto.DepartmentDTO;
import com.example.aihealthcheck.entity.Department;
import com.example.aihealthcheck.repository.user.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<DepartmentDTO> getAllDepartments() {
        List<Department> departments = departmentRepository.findAllByOrderByDeptNameAsc();
        return departments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentByCode(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public DepartmentDTO getDepartmentById(Integer deptId) {
        return departmentRepository.findById(deptId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDeptId(department.getDeptId());
        dto.setDeptCode(department.getDeptCode());
        dto.setDeptName(department.getDeptName());
        dto.setDeptType(department.getDeptType());
        dto.setIntroduction(department.getIntroduction());
        return dto;
    }
}