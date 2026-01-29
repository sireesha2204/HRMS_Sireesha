package com.mentis.hrms.repository;

import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.model.LeaveRequest.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {

    List<LeaveRequest> findByEmployeeId(String employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatus(String employeeId, LeaveStatus status);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    List<LeaveRequest> findByStatusIn(List<LeaveStatus> statuses);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employeeId = :employeeId AND " +
            "((l.startDate BETWEEN :startDate AND :endDate) OR " +
            "(l.endDate BETWEEN :startDate AND :endDate) OR " +
            "(l.startDate <= :startDate AND l.endDate >= :endDate))")
    List<LeaveRequest> findOverlappingLeaves(@Param("employeeId") String employeeId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.employeeId = :employeeId " +
            "AND YEAR(l.startDate) = YEAR(CURRENT_DATE) " +
            "AND l.leaveType.id = :leaveTypeId AND l.status = 'APPROVED'")
    Long countApprovedLeavesByTypeAndYear(@Param("employeeId") String employeeId,
                                          @Param("leaveTypeId") Long leaveTypeId);
}