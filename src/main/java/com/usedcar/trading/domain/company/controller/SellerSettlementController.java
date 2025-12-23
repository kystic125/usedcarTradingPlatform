package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.service.SettlementService;
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
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/company/settlements")
@RequiredArgsConstructor
public class SellerSettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public String settlementList(Model model,
                                 @AuthenticationPrincipal PrincipalDetails principal,
                                 @PageableDefault(size = 10, sort = "settledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = principal.getUser();

        Page<Settlement> settlementPage = settlementService.getMySettlements(user, pageable);

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
}