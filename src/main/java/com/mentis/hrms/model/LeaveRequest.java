package com.mentis.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Data
public class LeaveRequest {
    @Id
    @Column(name = "id")
    private String leaveId; // Format: LV-YYYY-XXXX

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_type")
    private HalfDayType halfDayType = HalfDayType.FULL_DAY;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "contact_number")
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "applied_date")
    private LocalDateTime appliedDate = LocalDateTime.now();

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Enum for half day type
    public enum HalfDayType {
        FIRST_HALF, SECOND_HALF, FULL_DAY
    }

    // Enum for leave status
    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }
}
