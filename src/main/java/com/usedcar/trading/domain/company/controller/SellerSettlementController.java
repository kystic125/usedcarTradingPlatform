package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/company/settlements")
@RequiredArgsConstructor
public class SellerSettlementController {

    private final SettlementRepository settlementRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String settlementList(Model model,
                                 @AuthenticationPrincipal Object principal,
                                 @PageableDefault(size = 10, sort = "settledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = findUser(principal);
        Company company;

        if (user.getRole() == Role.COMPANY_OWNER) {
            company = companyRepository.findByOwner_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("업체 정보가 없습니다."));
        } else if (user.getRole() == Role.COMPANY_EMPLOYEE) {
            company = employeeRepository.findByUserUserId(user.getUserId())
                    .map(Employee::getCompany)
                    .orElseThrow(() -> new IllegalArgumentException("소속된 회사가 없습니다."));
        } else {
            throw new IllegalStateException("판매자 권한이 없습니다.");
        }

        List<Settlement> allSettlements = settlementRepository.findByCompany(company);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allSettlements.size());
        List<Settlement> pagedList = (start > allSettlements.size()) ? List.of() : allSettlements.subList(start, end);

        Page<Settlement> settlementPage = new PageImpl<>(pagedList, pageable, allSettlements.size());

        model.addAttribute("settlements", settlementPage);

        int totalPages = settlementPage.getTotalPages();
        int nowPage = settlementPage.getNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if(endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        return "company/settlement-list";
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow();
        }
        throw new IllegalArgumentException("로그인이 필요합니다.");
    }
}