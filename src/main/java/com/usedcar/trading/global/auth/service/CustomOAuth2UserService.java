package com.usedcar.trading.global.auth.service;

import com.usedcar.trading.domain.user.entity.Provider;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.entity.UserStatus;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 1. 카카오 정보 가져오기
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String providerId = String.valueOf(attributes.get("id"));
        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");

        // 이메일 없으면 가짜 이메일 생성
        if (email == null || email.isEmpty()) {
            email = providerId + "@kakao.com";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user;

        // 로그인 상태라면? -> 기존 계정에 카카오 연결하기 (Link)
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String currentEmail = auth.getName(); // 현재 로그인한 사람의 이메일(ID)
            log.info("계정 연동 요청: 기존계정({}) + 카카오({})", currentEmail, providerId);
            user = linkAccount(currentEmail, providerId);
        }
        // 로그인 상태가 아니라면? -> 그냥 로그인하거나 회원가입 (Login or Signup)
        else {
            log.info("소셜 로그인/가입 요청: {}", email);
            user = saveOrLogin(email, name, providerId);
        }

        Map<String, Object> mutableAttributes = new java.util.HashMap<>(attributes);
        mutableAttributes.put("db_id", user.getUserId());

        return new com.usedcar.trading.global.auth.security.PrincipalDetails(user, attributes);
    }

    private User linkAccount(String currentEmail, String providerId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Optional<User> existingLink = userRepository.findByProviderId(providerId);

        if (existingLink.isPresent()) {
            User oldUser = existingLink.get();

            if (oldUser.getUserId().equals(currentUser.getUserId())) {
                return currentUser;
            }

            log.info("기존에 존재하던 카카오 계정(User ID={})을 삭제하고 현재 계정에 연동합니다.", oldUser.getUserId());
            userRepository.delete(oldUser);

            userRepository.flush();
        }

        currentUser.linkSocial(Provider.KAKAO, providerId);
        return userRepository.save(currentUser);
    }

    private User saveOrLogin(String email, String name, String providerId) {
        return userRepository.findByProviderId(providerId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .password(UUID.randomUUID().toString())
                            .phone("010-0000-0000")
                            .role(Role.CUSTOMER)
                            .provider(Provider.KAKAO)
                            .providerId(providerId)
                            .userStatus(UserStatus.ACTIVE)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}