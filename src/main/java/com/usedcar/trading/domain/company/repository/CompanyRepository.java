package com.usedcar.trading.domain.company.repository;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.entity.CompanyStatus;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {

    // 사업자번호로 업체 조회
    Optional<Company> findByBusinessNumber(String businessNumber);

    // 사업자번호 중복 확인
    boolean existsByBusinessNumber(String businessNumber);

    // 업체명 검색
    List<Company> findByBusinessNameContaining(String businessName);

    // User(owner)로 업체 조회 (내 업체 정보)
    Optional<Company> findByOwnerUserId(Long userId);

    // 영업 상태별 조회 (선택)
    List<Company> findByStatus(CompanyStatus status);
}
