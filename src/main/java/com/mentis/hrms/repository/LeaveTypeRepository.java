package com.mentis.hrms.repository;

import com.mentis.hrms.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    LeaveType findByTypeName(String typeName);
}
