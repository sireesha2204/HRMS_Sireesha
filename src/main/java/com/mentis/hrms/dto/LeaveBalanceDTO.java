package com.mentis.hrms.dto;

import lombok.Data;

@Data
public class LeaveBalanceDTO {
    private String leaveTypeName;
    private Integer totalAllocated;
    private Double usedDays;
    private Double remainingDays;
}
