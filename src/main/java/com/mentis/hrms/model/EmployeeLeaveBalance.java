package com.mentis.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "employee_leave_balance")
@Data
public class EmployeeLeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "total_allocated")
    private Integer totalAllocated = 0;

    @Column(name = "used_days")
    private Double usedDays = 0.0;

    @Column(name = "remaining_days")
    private Double remainingDays = 0.0;

    @Column(name = "year")
    private Integer year;
}
