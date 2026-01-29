package com.mentis.hrms.controller;

import com.mentis.hrms.dto.LeaveApprovalDTO;
import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.service.LeaveService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/hr/leave")
public class HrLeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/approvals")
    public String showLeaveApprovalsPage(Model model) {
        List<LeaveRequest> pendingLeaves = leaveService.getPendingLeaveRequests();
        model.addAttribute("pendingLeaves", pendingLeaves);
        return "hr/leave-approvals"; // Your existing HR approvals tab
    }

    @GetMapping("/pending")
    @ResponseBody
    public ResponseEntity<?> getPendingLeaveRequests() {
        try {
            List<LeaveRequest> pendingLeaves = leaveService.getPendingLeaveRequests();
            return ResponseEntity.ok(pendingLeaves);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/process")
    @ResponseBody
    public ResponseEntity<?> processLeaveApproval(@RequestBody LeaveApprovalDTO approvalDTO,
                                                  HttpSession session) {
        try {
            String approvedBy = (String) session.getAttribute("userName");
            if (approvedBy == null) {
                approvedBy = "HR Manager";
            }

            LeaveRequest processed = leaveService.processLeaveApproval(approvalDTO, approvedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request " + approvalDTO.getAction() + "d successfully!");
            response.put("leaveRequest", processed);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<?> getAllLeaveRequests(@RequestParam(required = false) String status) {
        try {
            List<LeaveRequest> leaves;
            if (status != null && !status.isEmpty()) {
                leaves = leaveService.getPendingLeaveRequests(); // Filter by status if needed
            } else {
                leaves = leaveService.getPendingLeaveRequests();
            }
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}")
    @ResponseBody
    public ResponseEntity<?> getEmployeeLeaves(@PathVariable String employeeId) {
        try {
            List<LeaveRequest> leaves = leaveService.getLeaveRequestsByEmployee(employeeId);
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
