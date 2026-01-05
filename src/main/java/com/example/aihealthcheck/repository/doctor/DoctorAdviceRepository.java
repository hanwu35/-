package com.example.aihealthcheck.repository.doctor;

import com.example.aihealthcheck.entity.doctor.DoctorAdvice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorAdviceRepository extends JpaRepository<DoctorAdvice, Long> {
    @Query("SELECT da FROM DoctorAdvice da WHERE da.patientUserId = :patientUserId ORDER BY da.updatedAt DESC")
    List<DoctorAdvice> findByPatientUserId(@Param("patientUserId") Integer patientUserId);

    @Query("SELECT da FROM DoctorAdvice da WHERE da.patientUserId = :patientUserId AND da.doctorUserId = :doctorUserId ORDER BY da.updatedAt DESC")
    List<DoctorAdvice> findByPatientAndDoctor(@Param("patientUserId") Integer patientUserId, @Param("doctorUserId") Integer doctorUserId);

    @Query("SELECT da FROM DoctorAdvice da WHERE da.patientUserId = :patientUserId AND da.status = 'DRAFT' ORDER BY da.updatedAt DESC")
    Optional<DoctorAdvice> findLatestDraftByPatient(@Param("patientUserId") Integer patientUserId);

    Optional<DoctorAdvice> findTopByPatientUserIdOrderByUpdatedAtDesc(Integer patientUserId);
}
