package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.dto.DepartmentDTO;
import com.example.aihealthcheck.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{deptCode}")
    public ResponseEntity<DepartmentDTO> getDepartmentByCode(@PathVariable String deptCode) {
        DepartmentDTO department = departmentService.getDepartmentByCode(deptCode);
        if (department != null) {
            return ResponseEntity.ok(department);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/id/{deptId}")
    public ResponseEntity<DepartmentDTO> getDepartmentById(@PathVariable Integer deptId) {
        DepartmentDTO department = departmentService.getDepartmentById(deptId);
        if (department != null) {
            return ResponseEntity.ok(department);
        }
        return ResponseEntity.notFound().build();
    }
}