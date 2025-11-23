package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CompanyApiController {

    private final CompanyRepository companyRepository;

    // 사업자 번호 중복 체크 API
    // 요청 주소: /api/check-business?number=123-45-67890
    @GetMapping("/api/check-business")
    public ResponseEntity<Boolean> checkBusinessNumber(@RequestParam String number) {
        boolean exists = companyRepository.existsByBusinessNumber(number);
        return ResponseEntity.ok(exists);
    }
}