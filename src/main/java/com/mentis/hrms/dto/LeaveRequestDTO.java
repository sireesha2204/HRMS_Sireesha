package com.mentis.hrms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveRequestDTO {
    private String leaveId;
    private String employeeId;
    private String employeeName;
    private String department;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalDays;
    private String halfDayType; // "first_half", "second_half", "full_day"
    private String reason;
    private String contactNumber;
    private String status;
    private String appliedDate;
    private List<String> selectedDates; // For multiple days selection
}
