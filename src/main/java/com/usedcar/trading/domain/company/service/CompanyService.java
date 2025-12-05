package com.usedcar.trading.domain.company.service;

import com.usedcar.trading.domain.company.dto.CompanyRegisterRequest;
import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.entity.CompanyStatus;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.entity.EmployeePosition;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public void registerCompany(String email, CompanyRegisterRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == Role.COMPANY_OWNER) {
            throw new IllegalStateException("이미 판매자로 등록되어 있습니다.");
        }

        // 유저 권한 변경 (CUSTOMER -> COMPANY_OWNER)
        user.upgradeToOwner();

        // 회사 정보 생성 및 저장
        Company company = Company.builder()
                .owner(user)
                .businessName(request.getBusinessName())
                .businessNumber(request.getBusinessNumber())
                .address(request.getAddress())
                .companyStatus(CompanyStatus.ACTIVE)
                .build();
        companyRepository.save(company);

        // 사장님을 LEADER 직원으로 등록
        Employee ownerAsEmployee = Employee.builder()
                .user(user)
                .company(company)
                .employeePosition(EmployeePosition.LEADER)
                .isActive(true)
                .build();
        employeeRepository.save(ownerAsEmployee);
    }

    /**
     * 판매자(업체) 목록 페이징 조회 [SELLER-004]
     */
    @Transactional(readOnly = true)
    public Page<Company> getActiveCompanies(Pageable pageable) {
        return companyRepository.findByCompanyStatus(CompanyStatus.ACTIVE, pageable);
    }

    /**
     * 판매자 검색 + 페이징
     */
    @Transactional(readOnly = true)
    public Page<Company> searchCompanies(String keyword, Pageable pageable) {
        return companyRepository.findByBusinessNameContainingAndCompanyStatus(keyword, CompanyStatus.ACTIVE, pageable);
    }

    /**
     * 업체 상세 조회
     */
    @Transactional(readOnly = true)
    public Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
    }
}