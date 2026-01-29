package com.mentis.hrms.service;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.AttendanceRepository;
import com.mentis.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Check-in employee (called on login or manual check-in)
     */
    public Attendance checkIn(String employeeId) {
        LocalDate today = LocalDate.now();
        logger.info("====== checkIn() CALLED ======");
        logger.info("Employee: {}, Date: {}", employeeId, today);
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today);

        if (existing.isPresent()) {
            Attendance attendance = existing.get();
            // If already checked in and active, return existing
            if ("ACTIVE".equals(attendance.getStatus()) || "BREAK".equals(attendance.getStatus())) {
                return attendance;
            }
            // If checked out earlier today, prevent re-checkin (optional rule)
            if (attendance.getCheckOutTime() != null) {
                logger.warn("Employee {} already checked out today", employeeId);
                return attendance;
            }
            // Resume from break or offline
            attendance.setStatus("ACTIVE");
            attendance.setUpdatedAt(LocalDateTime.now());
            return attendanceRepository.save(attendance);
        }

        // Create new attendance record
        Attendance attendance = new Attendance();
        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus("ACTIVE");

        // Update employee presence status
        updateEmployeePresence(employeeId, "ACTIVE");
        logger.info("====== checkIn() SUCCESS ======");
        logger.info("Saved attendance: ID={}, Status={}, Time={}",
                attendance.getId(), attendance.getStatus(), attendance.getCheckInTime());

        return attendanceRepository.save(attendance);

    }

    /**
     * Check-out employee
     */
    public Attendance checkOut(String employeeId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No active attendance found for today"));

        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Already checked out for today");
        }

        // End any active break
        if ("BREAK".equals(attendance.getStatus()) && attendance.getBreakStartTime() != null) {
            attendance.setBreakEndTime(LocalDateTime.now());
            long breakMinutes = Duration.between(attendance.getBreakStartTime(), attendance.getBreakEndTime()).toMinutes();
            attendance.setTotalBreakMinutes(attendance.getTotalBreakMinutes() + (int) breakMinutes);
        }

        // Calculate total working time
        attendance.setCheckOutTime(LocalDateTime.now());
        long totalMinutes = Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();
        int workingMinutes = (int) (totalMinutes - attendance.getTotalBreakMinutes());
        attendance.setTotalWorkingMinutes(workingMinutes);
        attendance.setStatus("OFFLINE");

        // Update employee presence
        updateEmployeePresence(employeeId, "OFFLINE");

        logger.info("Employee {} checked out. Working minutes: {}", employeeId, workingMinutes);
        return attendanceRepository.save(attendance);
    }

    /**
     * Start break
     */
    public Attendance startBreak(String employeeId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("Must check in first"));

        if (!"ACTIVE".equals(attendance.getStatus())) {
            throw new RuntimeException("Must be active to take a break");
        }

        attendance.setStatus("BREAK");
        attendance.setBreakStartTime(LocalDateTime.now());

        // Update employee presence
        updateEmployeePresence(employeeId, "BREAK");

        logger.info("Employee {} started break at {}", employeeId, attendance.getBreakStartTime());
        return attendanceRepository.save(attendance);
    }

    /**
     * End break
     */
    public Attendance endBreak(String employeeId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No active attendance found"));

        if (!"BREAK".equals(attendance.getStatus())) {
            throw new RuntimeException("Not on break");
        }

        attendance.setBreakEndTime(LocalDateTime.now());
        long breakMinutes = Duration.between(attendance.getBreakStartTime(), attendance.getBreakEndTime()).toMinutes();
        attendance.setTotalBreakMinutes(attendance.getTotalBreakMinutes() + (int) breakMinutes);
        attendance.setStatus("ACTIVE");

        // Update employee presence
        updateEmployeePresence(employeeId, "ACTIVE");

        logger.info("Employee {} ended break. Break minutes: {}", employeeId, breakMinutes);
        return attendanceRepository.save(attendance);
    }

    /**
     * Get today's attendance for employee with current session time
     */
    public Attendance getTodayAttendance(String employeeId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> opt = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today);

        if (opt.isPresent()) {
            Attendance attendance = opt.get();
            calculateCurrentSessionTime(attendance);
            return attendance;
        }
        return null;
    }

    /**
     * Calculate current working time for active sessions
     */
    private void calculateCurrentSessionTime(Attendance attendance) {
        if (attendance.getCheckInTime() == null) return;

        LocalDateTime now = LocalDateTime.now();
        long totalMinutes;

        if (attendance.getCheckOutTime() != null) {
            // Already checked out
            totalMinutes = attendance.getTotalWorkingMinutes();
        } else if ("BREAK".equals(attendance.getStatus()) && attendance.getBreakStartTime() != null) {
            // Currently on break - calculate up to break start
            totalMinutes = Duration.between(attendance.getCheckInTime(), attendance.getBreakStartTime()).toMinutes()
                    - attendance.getTotalBreakMinutes();
        } else {
            // Currently active
            totalMinutes = Duration.between(attendance.getCheckInTime(), now).toMinutes()
                    - attendance.getTotalBreakMinutes();
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        attendance.setCurrentSessionTime(String.format("%dh %02dm", hours, minutes));
    }

    /**
     * Get all employees' today's attendance for HR dashboard
     */
    public List<Map<String, Object>> getTodayAllAttendances() {
        LocalDate today = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByAttendanceDate(today);

        return attendances.stream().map(att -> {
            Map<String, Object> map = new HashMap<>();
            map.put("employeeId", att.getEmployeeId());
            map.put("status", att.getStatus());
            map.put("checkInTime", att.getCheckInTime());
            map.put("checkOutTime", att.getCheckOutTime());
            map.put("totalBreakMinutes", att.getTotalBreakMinutes());

            // Calculate current working time
            calculateCurrentSessionTime(att);
            map.put("currentWorkingTime", att.getCurrentSessionTime());

            // Get employee details - use ifPresent with defaults
            Optional<Employee> empOpt = employeeRepository.findByEmployeeId(att.getEmployeeId());
            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                map.put("employeeName", emp.getFirstName() + " " + emp.getLastName());
                map.put("department", emp.getDepartment());
                map.put("designation", emp.getDesignation());
            } else {
                // Provide defaults to prevent null in JSON
                map.put("employeeName", "Unknown Employee");
                map.put("department", "N/A");
                map.put("designation", "N/A");
            }

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Get attendance history for employee
     */
    public List<Attendance> getAttendanceHistory(String employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
    }

    /**
     * Update employee presence status in Employee table (for real-time status)
     */
    private void updateEmployeePresence(String employeeId, String status) {
        Optional<Employee> empOpt = employeeRepository.findByEmployeeId(employeeId);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            emp.setPresenceStatus(status);
            emp.setLastPresenceUpdate(LocalDateTime.now());
            employeeRepository.save(emp);
        }
    }
}