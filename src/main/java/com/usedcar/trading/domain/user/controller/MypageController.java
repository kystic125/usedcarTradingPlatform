package com.usedcar.trading.domain.user.controller;

import com.usedcar.trading.domain.user.entity.Provider;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/mypage")
    public String myPage(Model model, @AuthenticationPrincipal Object principal) {
        User user = null;

        // 로그인한 사용자 이메일 찾기
        if (principal instanceof UserDetails) {
            // 일반 로그인 (UserDetails)
            String email = ((UserDetails) principal).getUsername();
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        } else if (principal instanceof OAuth2User) {
            // 카카오 로그인 (OAuth2User)
            OAuth2User oauthUser = (OAuth2User) principal;
            String providerId = String.valueOf(oauthUser.getAttributes().get("id"));

            Optional<User> byProviderId = userRepository.findByProviderId(providerId);

            if (byProviderId.isPresent()) {
                user = byProviderId.get();
            } else {
                Long dbId = (Long) oauthUser.getAttributes().get("db_id");

                if (dbId != null) {
                    user = userRepository.findById(dbId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. (DB ID: " + dbId + ")"));
                } else {
                    // 만약 db_id도 없다면? (거의 없겠지만) -> 최후의 수단으로 이메일 시도
                    java.util.Map<String, Object> kakaoAccount = (java.util.Map<String, Object>) oauthUser.getAttributes().get("kakao_account");
                    String email = (String) kakaoAccount.get("email");
                    if (email == null) email = providerId + "@kakao.com";

                    user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. (계정 유실)"));
                }
            }
        }

        model.addAttribute("user", user);

        return "mypage";
    }

    // 1. 정보 수정 페이지 보여주기
    @GetMapping("/mypage/edit")
    public String editPage(Model model, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        model.addAttribute("user", user);
        return "user-edit";
    }

    // 2. 정보 수정 처리
    @PostMapping("/mypage/update")
    public String updateProcess(@RequestParam String email,
                                @RequestParam String phone,
                                @AuthenticationPrincipal Object principal) {

        User user = findUser(principal);

        boolean isEmailChanged = !user.getEmail().equals(email);

        // 정보 수정 시도
        try {
            userService.updateUserInfo(user.getUserId(), email, phone);
        } catch (IllegalArgumentException e) {
            return "redirect:/mypage/edit?error=" + e.getMessage();
        }

        if (isEmailChanged) {
            return "redirect:/logout";
        } else {
            return "redirect:/mypage";
        }
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            Long dbId = (Long) oauthUser.getAttributes().get("db_id");
            if (dbId != null) return userRepository.findById(dbId).orElseThrow();
            String providerId = String.valueOf(oauthUser.getAttributes().get("id"));
            return userRepository.findByProviderId(providerId).orElseThrow();
        }
        throw new IllegalArgumentException("로그인 정보 없음");
    }
}