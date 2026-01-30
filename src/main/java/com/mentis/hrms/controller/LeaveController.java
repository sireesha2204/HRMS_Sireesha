package com.mentis.hrms.controller;

import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.service.LeaveService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.mentis.hrms.service.LeaveService;
import com.mentis.hrms.model.LeaveRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    // Create leave request (optional - if you want to save requests immediately)
    @PostMapping("/request")
    public ResponseEntity<?> createLeaveRequest(@RequestBody LeaveRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Verify employee is logged in
            String employeeId = (String) session.getAttribute("employeeId");
            if (employeeId == null) {
                response.put("success", false);
                response.put("message", "Not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            request.setEmployeeId(employeeId);
            LeaveRequest saved = leaveService.saveLeaveRequest(request);

            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // HR: Approve leave
    @PostMapping("/approve/{leaveId}")
    public ResponseEntity<?> approveLeave(
            @PathVariable String leaveId,
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Verify HR role
            String role = (String) session.getAttribute("userRole");
            if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            String approvedBy = (String) session.getAttribute("userName");
            String remarks = payload.getOrDefault("remarks", "");

            LeaveRequest approved = leaveService.approveLeave(leaveId, approvedBy, remarks);

            response.put("success", true);
            response.put("message", "Leave approved successfully");
            response.put("data", approved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // HR: Reject leave
    @PostMapping("/reject/{leaveId}")
    public ResponseEntity<?> rejectLeave(
            @PathVariable String leaveId,
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            String role = (String) session.getAttribute("userRole");
            if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            String rejectedBy = (String) session.getAttribute("userName");
            String reason = payload.getOrDefault("reason", "");

            LeaveRequest rejected = leaveService.rejectLeave(leaveId, rejectedBy, reason);

            response.put("success", true);
            response.put("message", "Leave rejected");
            response.put("data", rejected);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get pending leaves for HR
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLeaves(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        List<LeaveRequest> pending = leaveService.getPendingRequests();
        response.put("success", true);
        response.put("leaves", pending);
        return ResponseEntity.ok(response);
    }

    // Get employee leaves
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeLeaves(@PathVariable String employeeId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // Security check - can only view own leaves unless HR
        String currentUser = (String) session.getAttribute("employeeId");
        String role = (String) session.getAttribute("userRole");

        if (!employeeId.equals(currentUser) && !"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        List<LeaveRequest> leaves = leaveService.getEmployeeLeaves(employeeId);
        response.put("success", true);
        response.put("leaves", leaves);
        return ResponseEntity.ok(response);
    }
}