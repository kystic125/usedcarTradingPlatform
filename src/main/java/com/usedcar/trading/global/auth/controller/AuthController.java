package com.usedcar.trading.global.auth.controller;

import com.usedcar.trading.domain.user.dto.SignupRequest;
import com.usedcar.trading.global.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인 페이지 보여주기
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 회원가입 페이지 보여주기
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // 회원가입 처리
    @PostMapping("/auth/signup")
    public String signupProcess(SignupRequest request, Model model) {
        try {
            authService.signup(request);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }
    }

    // 회원 탈퇴 처리
    @PostMapping("/auth/withdraw")
    public String withdraw(@AuthenticationPrincipal Object principal) {
        String email = "";

        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            java.util.Map<String, Object> kakaoAccount = (java.util.Map<String, Object>) oauthUser.getAttributes().get("kakao_account");
            email = (String) kakaoAccount.get("email");
            if (email == null) {
                String providerId = String.valueOf(oauthUser.getAttributes().get("id"));
                email = providerId + "@kakao.com";
            }
        }

        authService.withdraw(email);

        return "redirect:/logout";
    }

    // 연동 해제 요청
    @PostMapping("/auth/unlink")
    public String unlinkSocial(@AuthenticationPrincipal Object principal) {

        if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            String providerId = String.valueOf(oauthUser.getAttributes().get("id"));

            authService.unlinkSocialByProviderId(providerId);
        } else if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            authService.unlinkSocial(email);
        }

        return "redirect:/mypage";
    }
}