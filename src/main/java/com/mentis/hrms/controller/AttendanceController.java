package com.mentis.hrms.controller;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.service.AttendanceService;
import com.mentis.hrms.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * Get today's attendance status (for employee dashboard timer)
     */
    @GetMapping("/today/{employeeId}")
    public ResponseEntity<?> getTodayAttendance(@PathVariable String employeeId, HttpSession session) {
        try {
            Attendance attendance = attendanceService.getTodayAttendance(employeeId);

            Map<String, Object> response = new HashMap<>();

            if (attendance != null) {
                response.put("success", true);
                response.put("status", attendance.getStatus());
                response.put("checkInTime", attendance.getCheckInTime());
                response.put("checkOutTime", attendance.getCheckOutTime());
                response.put("breakMinutes", attendance.getTotalBreakMinutes());
                response.put("workingTime", attendance.getCurrentSessionTime());

                // Calculate exact milliseconds for accurate timer
                if (attendance.getCheckInTime() != null) {
                    long checkInMillis = attendance.getCheckInTime().atZone(
                            java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    response.put("checkInTimeMillis", checkInMillis);
                }

                // Update session
                session.setAttribute("attendanceStatus", attendance.getStatus());
            } else {
                response.put("success", true);
                response.put("status", "OFFLINE");
                response.put("workingTime", "0h 00m");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching attendance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Manual Check-in (if not auto-checked in)
     */
    @PostMapping("/checkin/{employeeId}")
    public ResponseEntity<?> checkIn(@PathVariable String employeeId) {
        try {
            Attendance attendance = attendanceService.checkIn(employeeId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Checked in successfully");
            response.put("checkInTime", attendance.getCheckInTime());
            response.put("status", attendance.getStatus());

            if (attendance.getCheckInTime() != null) {
                long checkInMillis = attendance.getCheckInTime().atZone(
                        java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                response.put("checkInTimeMillis", checkInMillis);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Check-out
     */
    @PostMapping("/checkout/{employeeId}")
    public ResponseEntity<?> checkOut(@PathVariable String employeeId) {
        try {
            Attendance attendance = attendanceService.checkOut(employeeId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Checked out successfully",
                    "totalWorkingMinutes", attendance.getTotalWorkingMinutes(),
                    "checkOutTime", attendance.getCheckOutTime()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Toggle Break (Start/End)
     */
    @PostMapping("/break/{employeeId}")
    public ResponseEntity<?> toggleBreak(@PathVariable String employeeId, @RequestParam String action) {
        try {
            Attendance attendance;
            String message;

            if ("start".equalsIgnoreCase(action)) {
                attendance = attendanceService.startBreak(employeeId);
                message = "Break started";
            } else {
                attendance = attendanceService.endBreak(employeeId);
                message = "Break ended";
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message,
                    "status", attendance.getStatus(),
                    "totalBreakMinutes", attendance.getTotalBreakMinutes()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all employees' today's attendance (for HR Dashboard)
     */

    @GetMapping("/today-all")
    public ResponseEntity<?> getTodayAllAttendances() {
        try {
            List<Map<String, Object>> attendances = attendanceService.getTodayAllAttendances();
            logger.info("Fetched {} attendance records for today", attendances.size());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attendances", attendances,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Error fetching all attendances: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    /**
     * Get attendance history for employee
     */
    @GetMapping("/history/{employeeId}")
    public ResponseEntity<?> getHistory(@PathVariable String employeeId) {
        try {
            List<Attendance> history = attendanceService.getAttendanceHistory(employeeId);
            return ResponseEntity.ok(Map.of("success", true, "history", history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}