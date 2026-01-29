package com.mentis.hrms.seeders;

import com.mentis.hrms.model.LeaveType;
import com.mentis.hrms.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LeaveTypeSeeder implements CommandLineRunner {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Override
    public void run(String... args) throws Exception {
        if (leaveTypeRepository.count() == 0) {
            createLeaveType("Earned Leave", 18, "Annual earned leave");
            createLeaveType("Sick Leave", 12, "Medical leave with certificate");
            createLeaveType("Casual Leave", 7, "Casual or personal leave");
            createLeaveType("Maternity Leave", 180, "Maternity leave for female employees");
            createLeaveType("Paternity Leave", 15, "Paternity leave for male employees");
            createLeaveType("Bereavement Leave", 5, "Leave for family bereavement");
            createLeaveType("Compensatory Leave", 0, "Compensatory off");
        }
    }

    private void createLeaveType(String name, int days, String description) {
        LeaveType leaveType = new LeaveType();
        leaveType.setTypeName(name);
        leaveType.setTotalDays(days);
        leaveType.setDescription(description);
        leaveTypeRepository.save(leaveType);
    }
}