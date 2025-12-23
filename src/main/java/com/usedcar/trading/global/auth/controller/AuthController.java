package com.usedcar.trading.global.auth.controller;

import com.usedcar.trading.domain.user.dto.SignupRequest;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import com.usedcar.trading.global.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public String withdraw(@AuthenticationPrincipal PrincipalDetails principal) {

        String email = principal.getUser().getEmail();
        authService.withdraw(email);

        return "redirect:/logout";
    }

    // 연동 해제 요청
    @PostMapping("/auth/unlink")
    public String unlinkSocial(@AuthenticationPrincipal PrincipalDetails principal) {

        User user = principal.getUser();

        if (user.getProviderId() != null) {
            authService.unlinkSocialByProviderId(user.getProviderId());
        } else {
            authService.unlinkSocial(user.getEmail());
        }

        return "redirect:/mypage";
    }
}