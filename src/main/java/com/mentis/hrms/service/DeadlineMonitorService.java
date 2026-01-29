package com.mentis.hrms.service;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
@Service
@Transactional
public class DeadlineMonitorService {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private NotificationService notificationService;

    // ✅ RUNS EVERY 5 MINUTES FOR TESTING (change to 0 */30 * * * * for production)
    @Scheduled(cron = "0 */5 * * * *")
    public void checkDeadlines() {
        System.out.println("🔄 DEADLINE MONITOR STARTED: " + LocalDateTime.now());

        try {
            List<String> activeStatuses = Arrays.asList(
                    "NOT_STARTED", "DOCUMENTS_PENDING", "DOCUMENTS_SUBMITTED", "IN_PROGRESS"
            );

            List<Employee> employees = employeeRepository.findByDocumentDeadlineIsNotNullAndOnboardingStatusIn(activeStatuses);
            System.out.println("📋 Found " + employees.size() + " employees with active deadlines");

            for (Employee employee : employees) {
                System.out.println("🔍 Checking: " + employee.getEmployeeId() +
                        " | Docs: " + employee.getSubmittedDocuments() + "/" + employee.getTotalDocuments() +
                        " | Deadline: " + employee.getDocumentDeadline() +
                        " | Flags: warning=" + employee.isDeadlineWarningSent() + ", final=" + employee.isDeadlineFinalSent());
                processEmployeeDeadline(employee);
            }
        } catch (Exception e) {
            System.err.println("❌ Deadline monitor system error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    protected void processEmployeeDeadline(Employee employee) {
        if (employee.getDocumentDeadline() == null) return;

        // Skip if all documents uploaded
        if (employee.getSubmittedDocuments() >= employee.getTotalDocuments() && employee.getTotalDocuments() > 0) {
            System.out.println("✅ Employee " + employee.getEmployeeId() + " has completed all documents - skipping");
            return;
        }

        LocalDateTime deadline = employee.getDocumentDeadline();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(LocalDateTime.now(), deadline);
        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDateTime.now(), deadline);

        System.out.println("⏱️  Hours until deadline: " + hoursUntilDeadline);

        boolean warningSent = employee.isDeadlineWarningSent();
        boolean finalSent = employee.isDeadlineFinalSent();
        boolean needsUpdate = false;

        // ✅ FIXED: Send notifications at 24h and overdue
        if (hoursUntilDeadline <= 0 && !finalSent) {
            // ⏰ OVERDUE
            System.out.println("🚨 OVERDUE - sending notification to: " + employee.getEmployeeId());
            notificationService.notifyDeadlineReached(employee);
            warningSent = true;
            finalSent = true;
            needsUpdate = true;
        } else if (hoursUntilDeadline <= 24 && !finalSent) {
            // ⚠️ 24 HOURS LEFT
            System.out.println("⚠️  24-hour warning - sending notification to: " + employee.getEmployeeId());
            notificationService.notifyDeadlineApproaching(employee, 24);
            warningSent = true;
            finalSent = true; // Prevent duplicate warnings
            needsUpdate = true;
        }

        if (needsUpdate) {
            updateDeadlineFlags(employee, warningSent, finalSent);
        }
    }

    private void updateDeadlineFlags(Employee employee, boolean warningSent, boolean finalSent) {
        employee.setDeadlineWarningSent(warningSent);
        employee.setDeadlineFinalSent(finalSent);
        employeeRepository.save(employee);
        System.out.println("📝 Flags updated for " + employee.getEmployeeId());
    }
}