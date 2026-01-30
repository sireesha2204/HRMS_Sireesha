package com.mentis.hrms.service;

import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.model.LeaveStatus;
import com.mentis.hrms.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    public LeaveRequest saveLeaveRequest(LeaveRequest request) {
        if (request.getAppliedDateTime() == null) {
            request.setAppliedDateTime(LocalDateTime.now());
        }
        return leaveRequestRepository.save(request);
    }

    public LeaveRequest approveLeave(String leaveId, String approvedBy, String remarks) {
        LeaveRequest leave = leaveRequestRepository.findByLeaveId(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + leaveId));

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);
        leave.setApprovedDate(LocalDateTime.now());
        leave.setApprovalRemarks(remarks);

        return leaveRequestRepository.save(leave);
    }

    public LeaveRequest rejectLeave(String leaveId, String rejectedBy, String reason) {
        LeaveRequest leave = leaveRequestRepository.findByLeaveId(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + leaveId));

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectedBy(rejectedBy);
        leave.setRejectedDate(LocalDateTime.now());
        leave.setRejectionReason(reason);

        return leaveRequestRepository.save(leave);
    }

    public List<LeaveRequest> getPendingRequests() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
    }

    public List<LeaveRequest> getEmployeeLeaves(String employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getApprovedLeaves(String employeeId) {
        return leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);
    }
}