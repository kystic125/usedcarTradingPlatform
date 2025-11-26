package com.usedcar.trading.global.config;

import com.usedcar.trading.global.auth.handler.CustomLoginSuccessHandler;
import com.usedcar.trading.global.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 1. 누구나 접근 가능 (메인, 검색, 차량 상세 보기 등)
                        .requestMatchers("/", "/search/**", "/vehicles/**", "/auth/**", "/oauth2/**", "/signup", "/login", "/api/**").permitAll()
                        .requestMatchers("/css/**", "/images/**", "/js/**", "/libs/**", "/data/**", "/h2-console/**", "/favicon.ico").permitAll()

                        // 2. 관리자 전용 (사이트 전체 관리)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 3. 업체 사장님 전용 (직원 관리, 업체 정보 수정 등 - 직원 접근 불가)
                        .requestMatchers("/company/management/**", "/company/employees/**").hasRole("COMPANY_OWNER")

                        // 4. 판매 업무 (사장님 + 직원 모두 가능 - 차량 등록, 판매 관리)
                        .requestMatchers("/company/sales/**", "/vehicles/register/**").hasAnyRole("COMPANY_OWNER", "COMPANY_EMPLOYEE")

                        // 5. 구매자 전용 (구매 요청, 찜하기, 리뷰 등)
                        // (단, 판매자도 차를 살 수 있게 할지 정책에 따라 다르지만, 보통은 분리하거나 모두 허용)
                        .requestMatchers("/buy/**", "/reviews/**").hasAnyRole("CUSTOMER", "COMPANY_OWNER", "COMPANY_EMPLOYEE", "ADMIN")

                        // 6. 그 외 마이페이지 등은 로그인한 누구나
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .successHandler(customLoginSuccessHandler)
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            response.sendRedirect("/");
                        })
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}