package com.mentis.hrms.dto;

import lombok.Data;

@Data
public class LeaveApprovalDTO {
    private String leaveId;
    private String action; // "approve" or "reject"
    private String remarks;
    private String approvedBy;
}