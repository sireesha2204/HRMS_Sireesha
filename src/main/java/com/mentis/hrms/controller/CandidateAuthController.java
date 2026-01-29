package com.mentis.hrms.controller;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.AttendanceService;  // ADD THIS IMPORT
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/candidate/auth")
public class CandidateAuthController {

    private static final Logger logger = LoggerFactory.getLogger(CandidateAuthController.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PasswordUtil passwordUtil;

    // ADD THIS FIELD
    @Autowired
    private AttendanceService attendanceService;

    /* ==================== LOGIN METHOD UPDATED ==================== */
    @PostMapping("/login")
    public String processLogin(
            @RequestParam String employeeId,
            @RequestParam(required = false) String password,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes ra) {

        logger.info("=== LOGIN ATTEMPT: {} ===", employeeId);

        try {
            // 1. Fetch Employee
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "No account found with this Employee ID");
                return "redirect:/candidate/login";
            }

            Employee employee = employeeOpt.get();

            // 2. Check Account Status
            if (!employee.isActive() && employee.getPassword() != null) {
                ra.addFlashAttribute("error", "Your account is deactivated. Please contact HR.");
                return "redirect:/candidate/login";
            }

            // 3. Check for First Time Login (No password set yet)
            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                logger.info("First time login detected for: {}", employeeId);
                // We use a clean session for password creation
                jakarta.servlet.http.HttpSession tempSession = request.getSession(true);
                tempSession.setAttribute("tempEmployeeId", employee.getEmployeeId());
                return "redirect:/candidate/create-password";
            }

            // 4. Validate Password for Returning Users
            if (password == null || password.trim().isEmpty()) {
                ra.addFlashAttribute("error", "Please enter your password");
                return "redirect:/candidate/login";
            }

            if (!passwordUtil.matchesPassword(password, employee.getPassword())) {
                ra.addFlashAttribute("error", "Invalid Employee ID or password");
                return "redirect:/candidate/login";
            }

            // 5. SESSION SECURITY: Invalidate old session and create a fresh one
            // This prevents "Overlap" where one user's tab affects another's
            jakarta.servlet.http.HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                existingSession.invalidate();
            }

            jakarta.servlet.http.HttpSession newSession = request.getSession(true);

            // 6. Set Standardized Session Attributes (Used by RoleInterceptor)
            newSession.setAttribute("userId", employee.getEmployeeId());
            newSession.setAttribute("userRole", employee.getRole().name()); // e.g., "HR", "EMPLOYEE"
            newSession.setAttribute("userName", employee.getFirstName() + " " + employee.getLastName());

            // Internal attributes for specific page logic
            newSession.setAttribute("candidateEmployeeId", employee.getEmployeeId());
            newSession.setMaxInactiveInterval(1800); // 30 minutes

            logger.info("✅ Login Successful. User: {}, Role: {}", employeeId, employee.getRole());

            /* ==================== ATTENDANCE AUTO CHECK-IN ==================== */
            /* ADD THIS BLOCK - After session creation, before redirect */
            /* ==================== ATTENDANCE AUTO CHECK-IN ==================== */
            logger.info(">>> STEP 1: ENTERING AUTO CHECK-IN BLOCK for employee: {}", employeeId);
            logger.info(">>> STEP 2: Employee role is: {}", employee.getRole().name());

            try {
                String roleName = employee.getRole().name();
                logger.info(">>> STEP 3: Checking if role '{}' is eligible", roleName);

                boolean isEmployee = "EMPLOYEE".equals(roleName);
                boolean isHR = "HR".equals(roleName);
                logger.info(">>> STEP 4: isEmployee={}, isHR={}", isEmployee, isHR);

                if (isEmployee || isHR) {
                    logger.info(">>> STEP 5: Role is ELIGIBLE, calling checkIn()");

                    Attendance attendance = attendanceService.checkIn(employeeId);

                    logger.info(">>> STEP 6: checkIn() returned successfully");
                    logger.info(">>> STEP 7: Attendance ID: {}", attendance.getId());
                    logger.info(">>> STEP 8: Check-in time: {}", attendance.getCheckInTime());
                    logger.info(">>> STEP 9: Status: {}", attendance.getStatus());

                    newSession.setAttribute("todayAttendance", attendance);
                    newSession.setAttribute("attendanceStatus", attendance.getStatus());

                    logger.info(">>> STEP 10: Session attributes set successfully");
                } else {
                    logger.warn(">>> STEP 5: Role '{}' NOT eligible for auto check-in", roleName);
                }
            } catch (Exception e) {
                logger.error(">>> ERROR in auto check-in: {}", e.getMessage());
                logger.error(">>> EXCEPTION: ", e);  // This prints full stack trace
            }
            /* ==================== END ATTENDANCE BLOCK ==================== */
            /* ==================== END ATTENDANCE BLOCK ==================== */

            // 7. Explicit Role-Based Redirection
            // This ensures the user lands on the correct dashboard immediately
            switch (employee.getRole()) {
                case SUPER_ADMIN:
                    return "redirect:/dashboard/admin";

                case HR:
                    return "redirect:/dashboard/hr";

                case EMPLOYEE:
                    return "redirect:/candidate/dashboard/" + employee.getEmployeeId();

                default:
                    logger.warn("Unknown role for user {}, defaulting to Employee dashboard", employeeId);
                    return "redirect:/candidate/dashboard/" + employee.getEmployeeId();
            }

        } catch (Exception e) {
            logger.error("❌ Login System Error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An internal error occurred. Please try again.");
            return "redirect:/candidate/login";
        }
    }

    /* ==================== HELPER METHOD (ADD THIS) ==================== */
    /**
     * Handles post-login attendance check-in (kept for manual use if needed)
     */
    private void handlePostLoginAttendance(String employeeId, HttpSession session) {
        try {
            logger.info("Manual check-in triggered for: {}", employeeId);
            Attendance attendance = attendanceService.checkIn(employeeId);

            session.setAttribute("todayAttendance", attendance);
            session.setAttribute("attendanceStatus", attendance.getStatus());

            if (attendance.getCheckInTime() != null) {
                long checkInMillis = attendance.getCheckInTime()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                session.setAttribute("checkInTimeMillis", checkInMillis);
            }
        } catch (Exception e) {
            logger.error("Manual check-in failed for {}: {}", employeeId, e.getMessage());
        }
    }

    /* ==================== OTHER METHODS (keep existing) ==================== */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "candidate/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetLink(@RequestParam String email,
                                @RequestParam(required = false) String employeeId,
                                RedirectAttributes ra) {

        logger.info("Forgot password request for email: {}, employeeId: {}", email, employeeId);

        try {
            Optional<Employee> employeeOpt;

            if (employeeId != null && !employeeId.trim().isEmpty()) {
                employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId.trim());
            } else {
                employeeOpt = employeeService.getAllEmployees().stream()
                        .filter(e -> email.equalsIgnoreCase(e.getEmail()) ||
                                email.equalsIgnoreCase(e.getPersonalEmail()))
                        .findFirst();
            }

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "No account found with provided email/employee ID.");
                return "redirect:/candidate/auth/forgot-password";
            }

            Employee employee = employeeOpt.get();

            if (!employee.isActive()) {
                ra.addFlashAttribute("error", "Account is deactivated. Please contact HR.");
                return "redirect:/candidate/auth/forgot-password";
            }

            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                ra.addFlashAttribute("error", "You haven't created your credentials yet. Please use the login page first.");
                return "redirect:/candidate/auth/forgot-password";
            }

            String resetToken = UUID.randomUUID().toString();
            employee.setResetToken(resetToken);
            employee.setTokenExpiry(LocalDateTime.now().plusHours(24));

            employeeService.saveEmployee(employee);

            String resetLink = "http://localhost:8080/candidate/auth/reset-password?token=" + resetToken;

            ra.addFlashAttribute("resetLink", resetLink);
            ra.addFlashAttribute("successMessage", "Password reset link generated successfully!");

            logger.info("Reset token generated for {}: {}", employee.getEmployeeId(), resetToken);

            return "redirect:/candidate/auth/forgot-password";

        } catch (Exception e) {
            logger.error("Error in forgot password: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/candidate/auth/forgot-password";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, Model model) {
        String employeeId = (String) session.getAttribute("userId");
        if (employeeId == null) {
            return "redirect:/candidate/auth/login?error=Please+login+first";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return "redirect:/candidate/auth/login?error=Employee+not+found";
        }

        model.addAttribute("employee", employeeOpt.get());
        return "candidate/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        String employeeId = (String) session.getAttribute("userId");
        if (employeeId == null) {
            return "redirect:/candidate/auth/login?error=Session+expired";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return "redirect:/candidate/auth/login?error=Employee+not+found";
        }

        Employee employee = employeeOpt.get();

        if (!passwordUtil.matchesPassword(currentPassword, employee.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/candidate/auth/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/candidate/auth/change-password";
        }

        if (newPassword.length() < 8) {
            ra.addFlashAttribute("error", "New password must be at least 8 characters");
            return "redirect:/candidate/auth/change-password";
        }

        employee.setPassword(passwordUtil.encodePassword(newPassword));
        employeeService.saveEmployee(employee);

        ra.addFlashAttribute("success", "Password changed successfully!");
        return "redirect:/candidate/dashboard/" + employee.getEmployeeId();
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {
        try {
            logger.info("Processing reset password request with token: {}", token);

            Optional<Employee> employeeOpt = employeeService.getAllEmployees().stream()
                    .filter(e -> token.equals(e.getResetToken()) &&
                            e.getTokenExpiry() != null &&
                            e.getTokenExpiry().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                logger.warn("Invalid or expired reset token: {}", token);
                model.addAttribute("error", "Invalid or expired reset token.");
                return "candidate/reset-password-error";
            }

            Employee employee = employeeOpt.get();
            model.addAttribute("token", token);
            model.addAttribute("employeeId", employee.getEmployeeId());
            model.addAttribute("employeeName", employee.getFirstName() + " " + employee.getLastName());

            logger.info("Reset password page loaded for employee: {}", employee.getEmployeeId());
            return "candidate/reset-password";

        } catch (Exception e) {
            logger.error("Error showing reset password page: {}", e.getMessage(), e);
            model.addAttribute("error", "Invalid reset token.");
            return "candidate/reset-password-error";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes ra) {

        try {
            Optional<Employee> employeeOpt = employeeService.getAllEmployees().stream()
                    .filter(e -> token.equals(e.getResetToken()) &&
                            e.getTokenExpiry() != null &&
                            e.getTokenExpiry().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Invalid or expired reset token.");
                return "redirect:/candidate/login";
            }

            if (!password.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Passwords do not match.");
                return "redirect:/candidate/auth/reset-password?token=" + token;
            }

            if (password.length() < 8) {
                ra.addFlashAttribute("error", "Password must be at least 8 characters long.");
                return "redirect:/candidate/auth/reset-password?token=" + token;
            }

            Employee employee = employeeOpt.get();

            employee.setPassword(passwordUtil.encodePassword(password));
            employee.setResetToken(null);
            employee.setTokenExpiry(null);

            employeeService.saveEmployee(employee);

            ra.addFlashAttribute("success", "Password reset successfully! You can now login with your new password.");
            return "redirect:/candidate/login";

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/candidate/auth/reset-password?token=" + token;
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Optional: Check-out on logout (uncomment if you want auto checkout on logout)
        /*
        String employeeId = (String) session.getAttribute("userId");
        if (employeeId != null) {
            try {
                attendanceService.checkOut(employeeId);
                logger.info("Auto check-out on logout for: {}", employeeId);
            } catch (Exception e) {
                logger.error("Auto check-out failed: {}", e.getMessage());
            }
        }
        */

        if (session != null) {
            session.invalidate();
        }
        return "redirect:/candidate/login?logout=success";
    }
}