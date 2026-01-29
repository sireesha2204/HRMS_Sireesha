package com.mentis.hrms.controller;
// Add these imports
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.util.List;
import com.mentis.hrms.service.DashboardService;
import com.mentis.hrms.service.OfferService;
import jakarta.servlet.http.HttpSession; // ✅ Ensure this import is present
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
// Add these imports at the top
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.EmployeeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Arrays;
@Controller
@RequestMapping("/dashboard/hr")
public class HrDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(HrDashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OfferService offerService;
    @Autowired
    private EmployeeService employeeService; // Add this

    /* ========== HR DASHBOARD HOME ========== */
    @GetMapping
    public String hrDashboard(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR DASHBOARD ACCESS REQUESTED ===");
        logger.debug("Session state - userId: {}, role: {}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        // Authentication check
        if (session.getAttribute("userId") == null) {
            ra.addFlashAttribute("error", "Please login to access HR Dashboard");
            return "redirect:/candidate/login"; // ✅ FIXED: Correct URL
        }

        String role = (String) session.getAttribute("userRole");
        if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return "redirect:/candidate/login?error=Unauthorized";
        }

        // Load dashboard data using service
        dashboardService.loadDashboardData(model);

        // Add HR-specific attributes
        model.addAttribute("isHR", true);
        model.addAttribute("isSuperAdmin", "SUPER_ADMIN".equals(role));
        model.addAttribute("userRole", role);
        model.addAttribute("userName", session.getAttribute("userName"));

        logger.info("✅ HR Dashboard loaded for user: {} (Role: {})",
                session.getAttribute("userName"), role);
        return "dashboard";
    }
    /* ========== SAVE EMPLOYEE ========== */
    @PostMapping("/save-employee")
    public String saveEmployee(@ModelAttribute Employee employee,
                               @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                               @RequestParam(value = "benefits", required = false) List<String> benefits,
                               HttpSession session,
                               RedirectAttributes ra) {
        logger.info("=== SAVING EMPLOYEE: {} {} ===", employee.getFirstName(), employee.getLastName());

        try {
            // 1. Check authorization
            String role = (String) session.getAttribute("userRole");
            if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
                ra.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/candidate/login";
            }

            // 2. Handle profile picture upload
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + profilePicture.getOriginalFilename();
                String uploadDir = "C:/hrms/uploads/employee-profiles/";

                // Create directory if not exists
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Save file
                File file = new File(dir.getAbsolutePath() + File.separator + fileName);
                profilePicture.transferTo(file);

                // Set profile picture path relative to uploads
                employee.setProfilePicture("employee-profiles/" + fileName);
            }

            // 3. Handle benefits
            if (benefits != null && !benefits.isEmpty()) {
                employee.setBenefits(benefits);
            }

            // 4. Set employee name from first and last name
            employee.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());

            // 5. Set email (use personal email if work email not provided)
            if (employee.getEmail() == null || employee.getEmail().isEmpty()) {
                employee.setEmail(employee.getPersonalEmail());
            }

            // 6. Set default onboarding status
            employee.setOnboardingStatus("NOT_STARTED");
            employee.setCredentialsCreated(false);
            employee.setActive(true);
            employee.setStatus("Active");

            // 7. Save employee
            Employee savedEmployee = employeeService.saveEmployee(employee);

            // 8. Set success message
            ra.addFlashAttribute("showSuccessPopup", true);
            ra.addFlashAttribute("employeeId", savedEmployee.getEmployeeId());
            ra.addFlashAttribute("employeeName", savedEmployee.getEmployeeName());
            ra.addFlashAttribute("message", "Employee added successfully!");

            logger.info("✅ Employee saved successfully: {} (ID: {})",
                    savedEmployee.getEmployeeName(), savedEmployee.getEmployeeId());

            return "redirect:/dashboard/hr/add-employee";

        } catch (Exception e) {
            logger.error("❌ Error saving employee: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to save employee: " + e.getMessage());
            return "redirect:/dashboard/hr/add-employee";
        }
    }

    /* ========== APPLICATIONS ========== */
    @GetMapping("/applications")
    public String hrApplications(@RequestParam(value = "status", required = false) String status,
                                 Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR APPLICATIONS PAGE REQUESTED ===");
        logger.debug("DEBUG: Session state - userId={}, role={}, path=/dashboard/hr/applications",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            logger.error("DEBUG: Auth failed in hrApplications - redirecting to login");
            return "redirect:/candidate/login";
        }

        logger.info("DEBUG: Forwarding to /dashboard/applications with status: {}", status);
        return "forward:/dashboard/applications" + (status != null ? "?status=" + status : "");
    }

    @GetMapping("/viewApplication/{id}")
    public String hrViewApplication(@PathVariable("id") Long applicationId,
                                    Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR VIEW APPLICATION {} REQUESTED ===", applicationId);
        logger.debug("DEBUG: View application - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/viewApplication/" + applicationId;
    }

    @GetMapping("/interview-schedule/{id}")
    public String hrInterviewSchedule(@PathVariable("id") Long applicationId,
                                      Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR INTERVIEW SCHEDULE PAGE FOR APPLICATION {} ===", applicationId);
        logger.debug("DEBUG: Interview schedule - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/interview-schedule/" + applicationId;
    }

    /* ========== JOB MANAGEMENT ========== */
    @GetMapping("/job-form")
    public String hrJobForm(Model model, HttpSession session, RedirectAttributes ra,
                            @RequestParam(value = "id", required = false) Long jobId) {
        logger.info("=== HR JOB FORM REQUESTED ===");
        logger.debug("DEBUG: Job form - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/job-form" + (jobId != null ? "?id=" + jobId : "");
    }

    @GetMapping("/viewJob/{id}")
    public String hrViewJob(@PathVariable("id") Long jobId, Model model,
                            HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR VIEW JOB {} REQUESTED ===", jobId);
        logger.debug("DEBUG: View job - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/viewJob/" + jobId;
    }

    /* ========== OFFER MANAGEMENT ========== */
    @GetMapping("/offer-candidates")
    public String hrOfferCandidates(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR OFFER CANDIDATES PAGE REQUESTED ===");
        logger.debug("DEBUG: Offer candidates - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/offer-candidates";
    }

    @GetMapping("/generate-offer/{id}")
    public String hrGenerateOffer(@PathVariable("id") String id,
                                  @RequestParam(value = "type", defaultValue = "APPLICATION") String type,
                                  Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR GENERATE OFFER PAGE REQUESTED ===");
        logger.debug("DEBUG: Generate offer - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/generate-offer/" + id + "?type=" + type;
    }

    /* ========== EMPLOYEE MANAGEMENT ========== */
    @GetMapping("/add-employee")
    public String hrAddEmployee(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR ADD EMPLOYEE PAGE REQUESTED ===");
        logger.debug("DEBUG: Add employee - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        // Add HR-specific data
        model.addAttribute("isSuperAdmin", "SUPER_ADMIN".equals(session.getAttribute("userRole")));
        model.addAttribute("offerCount", offerService.getOfferCount());

        // CHANGE THIS: Return the template directly instead of forwarding
        return "addemployee"; // This will render templates/addemployee.html
    }

    @GetMapping("/employees")
    public String hrEmployees(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR EMPLOYEES LIST PAGE REQUESTED ===");
        logger.debug("DEBUG: Employees list - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/dashboard/employees";
    }

    /* ========== ONBOARDING ========== */
    @GetMapping("/onboarding")
    public String hrOnboarding(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR ONBOARDING DASHBOARD REQUESTED ===");
        logger.debug("DEBUG: Onboarding - userId={}, role={}",
                session.getAttribute("userId"), session.getAttribute("userRole"));

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        return "forward:/onboarding";
    }

    /* ========== AUTHORIZATION HELPER ========== */
    private boolean isAuthorized(HttpSession session, RedirectAttributes ra) {
        String userId = (String) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId == null) {
            logger.warn("❌ Auth check failed: userId not in session");
            ra.addFlashAttribute("error", "Please login to access HR Dashboard");
            return false;
        }

        if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            logger.warn("❌ Auth check failed: User {} has invalid role: {}", userId, role);
            ra.addFlashAttribute("error", "Access denied: HR or Super Admin only");
            return false;
        }

        logger.debug("✅ Auth check passed for user: {} (Role: {})", userId, role);
        return true;
    }
}