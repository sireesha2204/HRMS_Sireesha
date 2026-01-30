package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leave_id", unique = true, nullable = false)
    private String leaveId; // Matches frontend format: LV-2026-0012

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Double totalDays;

    private String halfDayType; // first_half, second_half

    @Column(length = 1000)
    private String reason;

    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    private LocalDateTime appliedDateTime;

    // Approval Details
    private String approvedBy;
    private LocalDateTime approvedDate;
    @Column(length = 1000)
    private String approvalRemarks;

    // Rejection Details
    private String rejectedBy;
    private LocalDateTime rejectedDate;
    @Column(length = 1000)
    private String rejectionReason;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLeaveId() { return leaveId; }
    public void setLeaveId(String leaveId) { this.leaveId = leaveId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getTotalDays() { return totalDays; }
    public void setTotalDays(Double totalDays) { this.totalDays = totalDays; }

    public String getHalfDayType() { return halfDayType; }
    public void setHalfDayType(String halfDayType) { this.halfDayType = halfDayType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public LocalDateTime getAppliedDateTime() { return appliedDateTime; }
    public void setAppliedDateTime(LocalDateTime appliedDateTime) { this.appliedDateTime = appliedDateTime; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDateTime approvedDate) { this.approvedDate = approvedDate; }

    public String getApprovalRemarks() { return approvalRemarks; }
    public void setApprovalRemarks(String approvalRemarks) { this.approvalRemarks = approvalRemarks; }

    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }

    public LocalDateTime getRejectedDate() { return rejectedDate; }
    public void setRejectedDate(LocalDateTime rejectedDate) { this.rejectedDate = rejectedDate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}