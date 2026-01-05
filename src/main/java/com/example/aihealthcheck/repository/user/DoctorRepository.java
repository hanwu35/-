package com.example.aihealthcheck.repository.user;

import com.example.aihealthcheck.entity.Department;
import com.example.aihealthcheck.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    Optional<Doctor> findByDoctorCode(String doctorCode);

    Optional<Doctor> findByUserId(Integer userId);

    List<Doctor> findByDepartment(Department department);

    List<Doctor> findByLevel(String level);

    Page<Doctor> findByDepartment(Department department, Pageable pageable);

    // 修复的查询方法：正确处理全部科室的情况
    @Query("SELECT d FROM Doctor d WHERE " +
           "(:deptCode IS NULL OR :deptCode = '' OR :deptCode = 'all' OR d.department.deptCode = :deptCode)")
    Page<Doctor> findByDeptCode(@Param("deptCode") String deptCode, Pageable pageable);
    
    @Query("SELECT d FROM Doctor d WHERE " +
            "(:deptCode IS NULL OR :deptCode = '' OR :deptCode = 'all' OR d.department.deptCode = :deptCode) AND " +
            "(:name IS NULL OR :name = '' OR d.realName LIKE %:name%)")
    Page<Doctor> findByDeptCodeAndName(@Param("deptCode") String deptCode, @Param("name") String name, Pageable pageable);

    @Query("SELECT d FROM Doctor d ORDER BY d.realName ASC")
    List<Doctor> findAllOrderByName();

    @Query("SELECT d FROM Doctor d ORDER BY d.normalFee ASC")
    List<Doctor> findAllOrderByFee();

    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.department.deptId = :deptId")
    long countByDeptId(@Param("deptId") Integer deptId);
}
