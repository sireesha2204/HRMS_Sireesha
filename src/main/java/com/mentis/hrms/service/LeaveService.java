package com.mentis.hrms.service;

import com.mentis.hrms.dto.LeaveRequestDTO;
import com.mentis.hrms.dto.LeaveApprovalDTO;
import com.mentis.hrms.dto.LeaveBalanceDTO;
import com.mentis.hrms.model.*;
import com.mentis.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private EmployeeLeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional
    public LeaveRequest applyForLeave(LeaveRequestDTO leaveRequestDTO) {
        // Generate leave ID
        String leaveId = "LV-" + Year.now().getValue() + "-" +
                String.format("%04d", (int)(Math.random() * 10000));

        // Get leave type
        LeaveType leaveType = leaveTypeRepository.findById(leaveRequestDTO.getLeaveTypeId())
                .orElseThrow(() -> new RuntimeException("Leave type not found"));

        // Calculate total days
        double totalDays = calculateTotalDays(leaveRequestDTO);

        // Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveId(leaveId);
        leaveRequest.setEmployeeId(leaveRequestDTO.getEmployeeId());
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(leaveRequestDTO.getStartDate());
        leaveRequest.setEndDate(leaveRequestDTO.getEndDate());
        leaveRequest.setTotalDays(totalDays);

        if ("first_half".equals(leaveRequestDTO.getHalfDayType())) {
            leaveRequest.setHalfDayType(LeaveRequest.HalfDayType.FIRST_HALF);
        } else if ("second_half".equals(leaveRequestDTO.getHalfDayType())) {
            leaveRequest.setHalfDayType(LeaveRequest.HalfDayType.SECOND_HALF);
        } else {
            leaveRequest.setHalfDayType(LeaveRequest.HalfDayType.FULL_DAY);
        }

        leaveRequest.setReason(leaveRequestDTO.getReason());
        leaveRequest.setContactNumber(leaveRequestDTO.getContactNumber());
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        leaveRequest.setAppliedDate(LocalDateTime.now());

        // Check for overlapping leaves
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findOverlappingLeaves(
                leaveRequestDTO.getEmployeeId(),
                leaveRequestDTO.getStartDate(),
                leaveRequestDTO.getEndDate()
        );

        if (!overlappingLeaves.isEmpty()) {
            throw new RuntimeException("You already have a leave request for the selected dates");
        }

        // Check leave balance
        checkLeaveBalance(leaveRequestDTO.getEmployeeId(), leaveType.getId(), totalDays);

        return leaveRequestRepository.save(leaveRequest);
    }

    private double calculateTotalDays(LeaveRequestDTO dto) {
        if (dto.getSelectedDates() != null && !dto.getSelectedDates().isEmpty()) {
            // For multiple days selection
            return dto.getSelectedDates().size();
        } else if (dto.getStartDate() != null && dto.getEndDate() != null) {
            // Calculate days between dates
            long daysBetween = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;

            // Adjust for half day
            if ("first_half".equals(dto.getHalfDayType()) || "second_half".equals(dto.getHalfDayType())) {
                return 0.5;
            }

            return (double) daysBetween;
        }
        return 1.0; // Default to 1 day
    }

    private void checkLeaveBalance(String employeeId, Long leaveTypeId, double requestedDays) {
        int currentYear = Year.now().getValue();

        EmployeeLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, currentYear)
                .orElseGet(() -> {
                    // Create default balance if not exists
                    EmployeeLeaveBalance newBalance = new EmployeeLeaveBalance();
                    newBalance.setEmployeeId(employeeId);
                    newBalance.setLeaveType(leaveTypeRepository.findById(leaveTypeId).orElse(null));
                    newBalance.setYear(currentYear);
                    // Set default allocation (you can customize this)
                    newBalance.setTotalAllocated(18); // Default 18 days
                    newBalance.setRemainingDays(18.0);
                    return leaveBalanceRepository.save(newBalance);
                });

        if (balance.getRemainingDays() < requestedDays) {
            throw new RuntimeException("Insufficient leave balance. Available: " +
                    balance.getRemainingDays() + " days");
        }
    }

    @Transactional
    public LeaveRequest processLeaveApproval(LeaveApprovalDTO approvalDTO, String approvedBy) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(approvalDTO.getLeaveId())
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if ("approve".equals(approvalDTO.getAction())) {
            leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
            leaveRequest.setApprovedBy(approvedBy);
            leaveRequest.setApprovedDate(LocalDate.now());

            // Update leave balance
            updateLeaveBalanceAfterApproval(leaveRequest);

        } else if ("reject".equals(approvalDTO.getAction())) {
            leaveRequest.setStatus(LeaveRequest.LeaveStatus.REJECTED);
            leaveRequest.setRejectionReason(approvalDTO.getRemarks());
        }

        return leaveRequestRepository.save(leaveRequest);
    }

    private void updateLeaveBalanceAfterApproval(LeaveRequest leaveRequest) {
        int currentYear = Year.now().getValue();

        EmployeeLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployeeId(),
                        leaveRequest.getLeaveType().getId(),
                        currentYear
                ).orElse(null);

        if (balance != null) {
            double newUsedDays = balance.getUsedDays() + leaveRequest.getTotalDays();
            double newRemaining = balance.getRemainingDays() - leaveRequest.getTotalDays();

            balance.setUsedDays(newUsedDays);
            balance.setRemainingDays(newRemaining);
            leaveBalanceRepository.save(balance);
        }
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING);
    }

    public List<LeaveRequest> getLeaveRequestsByEmployee(String employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveBalanceDTO> getEmployeeLeaveBalance(String employeeId) {
        List<EmployeeLeaveBalance> balances = leaveBalanceRepository.findCurrentYearBalance(employeeId);

        return balances.stream().map(balance -> {
            LeaveBalanceDTO dto = new LeaveBalanceDTO();
            dto.setLeaveTypeName(balance.getLeaveType().getTypeName());
            dto.setTotalAllocated(balance.getTotalAllocated());
            dto.setUsedDays(balance.getUsedDays());
            dto.setRemainingDays(balance.getRemainingDays());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public boolean cancelLeaveRequest(String leaveId, String employeeId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!leaveRequest.getEmployeeId().equals(employeeId)) {
            throw new RuntimeException("You are not authorized to cancel this leave request");
        }

        if (leaveRequest.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leave requests can be cancelled");
        }

        leaveRequest.setStatus(LeaveRequest.LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leaveRequest);
        return true;
    }
}
