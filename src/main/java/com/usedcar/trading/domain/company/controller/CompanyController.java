package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.company.dto.CompanyRegisterRequest;
import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.service.CompanyService;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // 판매자 전환 신청 페이지 보여주기
    @GetMapping("/register")
    public String registerPage() {
        return "company/company-register";
    }

    // 판매자 전환 처리
    @PostMapping("/register")
    public String registerProcess(CompanyRegisterRequest request, @AuthenticationPrincipal PrincipalDetails principal) {
        String email = principal.getUser().getEmail();
        companyService.registerCompany(email, request);

        return "redirect:/logout";
    }

    // 관리자 목록 페이징/정렬
    @GetMapping("/list")
    public String companyList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String keyword,
            Model model) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Company> companies;
        if (keyword != null && !keyword.trim().isEmpty()) {
            companies = companyService.searchCompanies(keyword, pageRequest);
        } else {
            companies = companyService.getActiveCompanies(pageRequest);
        }

        model.addAttribute("companies", companies);
        model.addAttribute("currentPage", page);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("keyword", keyword);

        return "company/list";
    }

    // 업체 상세 조회
    @GetMapping("/{id}")
    public String companyDetail(@PathVariable Long id, Model model) {
        Company company = companyService.getCompany(id);
        model.addAttribute("company", company);
        return "company/detail";
    }
}