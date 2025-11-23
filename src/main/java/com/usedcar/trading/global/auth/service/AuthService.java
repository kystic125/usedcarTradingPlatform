package com.usedcar.trading.global.auth.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.entity.CompanyStatus;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.entity.EmployeePosition;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.user.dto.SignupRequest;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. 회원가입 로직
    @Transactional
    public Long signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 1) 유저 저장 (USERS 테이블)
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User savedUser = userRepository.save(request.toEntity(encodedPassword));

        // 2) 기업 회원(OWNER)이라면 -> 회사 정보도 저장 (COMPANY 테이블)
        if (savedUser.getRole() == Role.COMPANY_OWNER) {
            if (request.getBusinessNumber() == null) {
                throw new IllegalArgumentException("사업자 번호는 필수입니다.");
            }

            Company company = Company.builder()
                    .owner(savedUser)
                    .businessName(request.getBusinessName())
                    .businessNumber(request.getBusinessNumber())
                    .address(request.getAddress())
                    .companyStatus(CompanyStatus.ACTIVE)
                    .build();

            companyRepository.save(company);

            Employee ownerAsEmployee = Employee.builder()
                    .user(savedUser)
                    .company(company)
                    .employeePosition(EmployeePosition.LEADER)
                    .isActive(true)
                    .build();

            employeeRepository.save(ownerAsEmployee);
        }

        return savedUser.getUserId();
    }

    // 2. 로그인 로직
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return new com.usedcar.trading.global.auth.security.PrincipalDetails(user);
    }

    // 3. 회원 탈퇴
    @Transactional
    public void withdraw(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == Role.COMPANY_OWNER) {
            companyRepository.findByOwner_UserId(user.getUserId()).ifPresent(company -> {

                List<Employee> employees = employeeRepository.findByCompanyCompanyId(company.getCompanyId());
                for (Employee emp : employees) {
                    User empUser = emp.getUser();
                    employeeRepository.delete(emp);
                    if (!empUser.getUserId().equals(user.getUserId())) {
                        userRepository.delete(empUser);
                    }
                }

                companyRepository.delete(company);
            });
        } else if (user.getRole() == Role.COMPANY_EMPLOYEE) {
            employeeRepository.findByUserUserId(user.getUserId())
                    .ifPresent(employee -> employeeRepository.delete(employee));
        }

        userRepository.delete(user);
    }

    // 4. 소셜 연동 해제
    @Transactional
    public void unlinkSocial(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.unlinkSocial();
    }

    @Transactional
    public void unlinkSocialByProviderId(String providerId) {
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. (카카오 ID: " + providerId + ")"));

        user.unlinkSocial();
    }
}