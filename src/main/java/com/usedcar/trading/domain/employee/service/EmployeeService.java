package com.usedcar.trading.domain.employee.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.dto.EmployeeRegisterRequest;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.entity.EmployeePosition;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.user.entity.Provider;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.entity.UserStatus;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehicleRepository vehicleRepository;

    // 1. 직원 등록
    @Transactional
    public void registerEmployee(Long ownerId, EmployeeRegisterRequest request) {

        Company company = companyRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        // 직원용 'User' 계정 생성 (로그인용)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User employeeUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(Role.COMPANY_EMPLOYEE)
                .provider(Provider.LOCAL)
                .userStatus(UserStatus.ACTIVE)
                .build();

        userRepository.save(employeeUser);

        // 'Employee' 정보 생성 (회사 소속 연결)
        Employee employee = Employee.builder()
                .user(employeeUser)
                .company(company)
                .employeePosition(EmployeePosition.DEALER)
                .isActive(true)
                .build();

        employeeRepository.save(employee);
    }

    // 2. 우리 회사 직원 목록 조회
    @Transactional(readOnly = true)
    public Page<Employee> getMyEmployees(Long ownerId, Pageable pageable) {
        Company company = companyRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        return employeeRepository.findByCompanyCompanyId(company.getCompanyId(), pageable);
    }

    // 3. 직원 해고
    @Transactional
    public void fireEmployee(Long ownerId, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));

        Company myCompany = companyRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        if (!employee.getCompany().getCompanyId().equals(myCompany.getCompanyId())) {
            throw new IllegalArgumentException("당신의 직원이 아닙니다.");
        }

        Employee ownerEmployee = employeeRepository.findByUserUserId(ownerId)
                .orElseThrow(() -> new IllegalStateException("사장님의 직원 정보를 찾을 수 없습니다."));

        List<Vehicle> employeeVehicles = vehicleRepository.findByRegisteredBy(employee);
        for (Vehicle vehicle : employeeVehicles) {
            vehicle.setRegisteredBy(ownerEmployee);
        }

        User user = employee.getUser();
        employeeRepository.delete(employee);
        userRepository.delete(user);
    }
}