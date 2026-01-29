package com.mentis.hrms.repository;

import com.mentis.hrms.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(String employeeId, LocalDate date);

    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(String employeeId);

    List<Attendance> findByAttendanceDate(LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = ?1 AND a.status = 'ACTIVE'")
    List<Attendance> findActiveAttendancesByDate(LocalDate date);

    boolean existsByEmployeeIdAndAttendanceDate(String employeeId, LocalDate date);
}