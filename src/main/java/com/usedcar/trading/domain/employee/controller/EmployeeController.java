package com.usedcar.trading.domain.employee.controller;

import com.usedcar.trading.domain.employee.dto.EmployeeRegisterRequest;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.service.EmployeeService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/company/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // 1. 직원 목록 페이지
    @GetMapping
    public String employeeList(Model model, @AuthenticationPrincipal PrincipalDetails principal, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User owner = principal.getUser();

        Page<Employee> employees = employeeService.getMyEmployees(owner.getUserId(), pageable);

        int nowPage = employees.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, employees.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("user", owner);
        model.addAttribute("employees", employees);
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "company/employee-list";
    }

    // 2. 직원 등록 페이지
    @GetMapping("/register")
    public String registerPage(Model model, @AuthenticationPrincipal PrincipalDetails principal) {
        User owner = principal.getUser();
        model.addAttribute("user", owner);
        return "company/employee-register";
    }

    // 3. 직원 등록 처리
    @PostMapping("/register")
    public String registerProcess(EmployeeRegisterRequest request, @AuthenticationPrincipal PrincipalDetails principal) {
        User owner = principal.getUser();

        employeeService.registerEmployee(owner.getUserId(), request);

        return "redirect:/company/employees";
    }

    // 4. 직원 삭제 (해고) 처리
    @PostMapping("/delete/{employeeId}")
    public String fireEmployee(@PathVariable Long employeeId, @AuthenticationPrincipal PrincipalDetails principal) {
        User owner = principal.getUser();

        employeeService.fireEmployee(owner.getUserId(), employeeId);

        return "redirect:/company/employees";
    }
}