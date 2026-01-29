package com.mentis.hrms.controller;

import com.mentis.hrms.dto.LeaveRequestDTO;
import com.mentis.hrms.dto.LeaveBalanceDTO;
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
@RequestMapping("/employee/leave")
public class EmployeeLeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/apply")
    public String showLeaveApplicationPage(HttpSession session, Model model) {
        String employeeId = (String) session.getAttribute("employeeId");
        if (employeeId == null) {
            return "redirect:/candidate/login";
        }

        List<LeaveBalanceDTO> leaveBalance = leaveService.getEmployeeLeaveBalance(employeeId);
        model.addAttribute("leaveBalance", leaveBalance);
        return "employee/apply-leave"; // Your existing HTML page
    }

    @PostMapping("/apply")
    @ResponseBody
    public ResponseEntity<?> applyForLeave(@RequestBody LeaveRequestDTO leaveRequestDTO,
                                           HttpSession session) {
        try {
            String employeeId = (String) session.getAttribute("employeeId");
            if (employeeId == null) {
                return ResponseEntity.badRequest().body("Session expired. Please login again.");
            }

            leaveRequestDTO.setEmployeeId(employeeId);
            LeaveRequest leaveRequest = leaveService.applyForLeave(leaveRequestDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request submitted successfully!");
            response.put("leaveId", leaveRequest.getLeaveId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/my-leaves")
    @ResponseBody
    public ResponseEntity<?> getMyLeaves(HttpSession session) {
        try {
            String employeeId = (String) session.getAttribute("employeeId");
            if (employeeId == null) {
                return ResponseEntity.badRequest().body("Session expired");
            }

            List<LeaveRequest> leaves = leaveService.getLeaveRequestsByEmployee(employeeId);
            return ResponseEntity.ok(leaves);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/cancel/{leaveId}")
    @ResponseBody
    public ResponseEntity<?> cancelLeaveRequest(@PathVariable String leaveId,
                                                HttpSession session) {
        try {
            String employeeId = (String) session.getAttribute("employeeId");
            if (employeeId == null) {
                return ResponseEntity.badRequest().body("Session expired");
            }

            boolean cancelled = leaveService.cancelLeaveRequest(leaveId, employeeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", cancelled);
            response.put("message", cancelled ? "Leave request cancelled successfully" : "Failed to cancel leave");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/balance")
    @ResponseBody
    public ResponseEntity<?> getLeaveBalance(HttpSession session) {
        try {
            String employeeId = (String) session.getAttribute("employeeId");
            if (employeeId == null) {
                return ResponseEntity.badRequest().body("Session expired");
            }

            List<LeaveBalanceDTO> balance = leaveService.getEmployeeLeaveBalance(employeeId);
            return ResponseEntity.ok(balance);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
