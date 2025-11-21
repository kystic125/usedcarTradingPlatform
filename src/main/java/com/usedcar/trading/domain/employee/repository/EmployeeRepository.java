package com.usedcar.trading.domain.employee.repository;

import com.usedcar.trading.domain.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // 특정 업체의 직원 목록 조회
    List<Employee> findByCompanyCompanyId(Long companyId);

    // 특정 업체의 활동중인 직원만 조회
    List<Employee> findByCompanyCompanyIdAndIsActive(Long companyId, boolean isActive);

    // 특정 회원의 Employee 정보 조회
    Optional<Employee> findByUserUserId(Long userId);

}
