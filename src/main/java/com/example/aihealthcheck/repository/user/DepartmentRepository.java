package com.example.aihealthcheck.repository.user;

import com.example.aihealthcheck.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    Optional<Department> findByDeptCode(String deptCode);

    Optional<Department> findByDeptName(String deptName);

    List<Department> findAllByOrderByDeptNameAsc();

    @Query("SELECT d FROM Department d WHERE d.deptName LIKE %:keyword% OR d.deptCode LIKE %:keyword%")
    List<Department> searchByKeyword(String keyword);

    boolean existsByDeptCode(String deptCode);
}
