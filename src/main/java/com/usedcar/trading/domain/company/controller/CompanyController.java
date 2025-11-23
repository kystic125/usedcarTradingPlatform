package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.company.dto.CompanyRegisterRequest;
import com.usedcar.trading.domain.company.service.CompanyService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final UserRepository userRepository;

    // 1. 판매자 전환 신청 페이지 보여주기
    @GetMapping("/register")
    public String registerPage() {
        return "company/company-register";
    }

    // 2. 판매자 전환 처리
    @PostMapping("/register")
    public String registerProcess(CompanyRegisterRequest request, @AuthenticationPrincipal Object principal) {
        String email = getEmailFromPrincipal(principal);
        companyService.registerCompany(email, request);

        return "redirect:/logout";
    }

    // 이메일 추출 헬퍼 메서드
    private String getEmailFromPrincipal(Object principal) {
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            java.util.Map<String, Object> kakaoAccount = (java.util.Map<String, Object>) oauthUser.getAttributes().get("kakao_account");
            String email = (String) kakaoAccount.get("email");
            if (email == null) {
                String providerId = String.valueOf(oauthUser.getAttributes().get("id"));
                email = providerId + "@kakao.com";
            }
            return email;
        }
        throw new IllegalArgumentException("로그인 정보가 없습니다.");
    }
}