package com.example.aihealthcheck.repository.user;

import com.example.aihealthcheck.entity.user.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.status IN ('confirmed', 'completed') ORDER BY a.appointmentDate DESC, a.appointmentId DESC LIMIT 1")
    Optional<Appointment> findLastSuccessfulAppointment(@Param("patientId") Integer patientId);
    
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.doctorId = :doctorId AND a.status IN ('confirmed', 'completed') ORDER BY a.appointmentDate DESC LIMIT 1")
    Optional<Appointment> findLastAppointmentWithDoctor(@Param("patientId") Integer patientId, @Param("doctorId") Integer doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status IN ('confirmed', 'completed')")
    java.util.List<Appointment> findByDoctorIdAndAppointmentDate(@Param("doctorId") Integer doctorId, @Param("date") java.time.LocalDate date);
}
