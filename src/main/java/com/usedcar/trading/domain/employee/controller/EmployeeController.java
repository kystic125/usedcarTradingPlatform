package com.usedcar.trading.domain.employee.controller;

import com.usedcar.trading.domain.employee.dto.EmployeeRegisterRequest;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.service.EmployeeService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/company/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final UserRepository userRepository;

    // 1. 직원 목록 페이지
    @GetMapping
    public String employeeList(Model model, @AuthenticationPrincipal Object principal) {
        User owner = findUser(principal);

        List<Employee> employees = employeeService.getMyEmployees(owner.getUserId());
        model.addAttribute("employees", employees);

        return "company/employee-list";
    }

    // 2. 직원 등록 페이지
    @GetMapping("/register")
    public String registerPage() {
        return "company/employee-register";
    }

    // 3. 직원 등록 처리
    @PostMapping("/register")
    public String registerProcess(EmployeeRegisterRequest request, @AuthenticationPrincipal Object principal) {
        User owner = findUser(principal);

        employeeService.registerEmployee(owner.getUserId(), request);

        return "redirect:/company/employees";
    }

    // 4. 직원 삭제 (해고) 처리
    @PostMapping("/delete/{employeeId}")
    public String fireEmployee(@PathVariable Long employeeId, @AuthenticationPrincipal Object principal) {
        User owner = findUser(principal);

        employeeService.fireEmployee(owner.getUserId(), employeeId);

        return "redirect:/company/employees";
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            // 일반 로그인
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보 없음 (Local)"));
        } else if (principal instanceof OAuth2User) {
            // 카카오 로그인
            OAuth2User oauthUser = (OAuth2User) principal;

            Long dbId = (Long) oauthUser.getAttributes().get("db_id");
            if (dbId != null) {
                return userRepository.findById(dbId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자 정보 없음 (DB ID)"));
            }

            String providerId = String.valueOf(oauthUser.getAttributes().get("id"));
            return userRepository.findByProviderId(providerId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보 없음 (Kakao ID)"));
        }
        throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
    }
}