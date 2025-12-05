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
                        // 1. 정적 리소스 및 공용 페이지 (누구나 접근)
                        .requestMatchers("/css/**", "/images/**", "/icons/**", "/js/**", "/libs/**", "/data/**", "/uploads/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/search/**", "/vehicles", "/vehicles/{id}", "/auth/**", "/oauth2/**", "/signup", "/login", "/api/**").permitAll()

                        // 2. 관리자 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 3. 판매자 (사장님 + 직원) 전용
                        .requestMatchers("/company/sales/**", "/vehicles/register/**", "/vehicles/{id}/edit", "/vehicles/{id}/delete").hasAnyRole("COMPANY_OWNER", "COMPANY_EMPLOYEE")

                        // 4. 사장님 전용 (직원 관리 등)
                        .requestMatchers("/company/employees/**", "/company/settlements/**").hasRole("COMPANY_OWNER")

                        // 5. 구매자 전용 (찜, 리뷰 작성, 신고)
                        // (참고: 신고는 누구나 가능하게 할 수도 있지만, 보통 로그인한 유저만 가능)
                        .requestMatchers("/favorites/**", "/reviews/write/**", "/reports/write").hasRole("CUSTOMER")

                        // 6. 공통 기능 (로그인한 누구나)
                        .requestMatchers("/mypage/**", "/notifications/**", "/transactions/**").authenticated()

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
                        .successHandler(customLoginSuccessHandler)
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