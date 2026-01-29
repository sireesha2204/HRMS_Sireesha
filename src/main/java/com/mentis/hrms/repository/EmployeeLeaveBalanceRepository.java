package com.mentis.hrms.repository;

import com.mentis.hrms.model.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {

    List<EmployeeLeaveBalance> findByEmployeeId(String employeeId);

    Optional<EmployeeLeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(String employeeId, Long leaveTypeId, Integer year);

    @Query("SELECT b FROM EmployeeLeaveBalance b WHERE b.employeeId = :employeeId AND b.year = YEAR(CURRENT_DATE)")
    List<EmployeeLeaveBalance> findCurrentYearBalance(@Param("employeeId") String employeeId);
}
