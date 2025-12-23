package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.company.service.SalesService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/company/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @GetMapping
    public String salesDashboard(Model model,
                                 @AuthenticationPrincipal PrincipalDetails principal,
                                 @RequestParam(required = false, defaultValue = "ALL") String filter,
                                 @PageableDefault(size = 10) Pageable pageable) {

        User user = principal.getUser();

        Page<Vehicle> vehiclePage = salesService.getMySalesVehicles(user, filter, pageable);

        model.addAttribute("vehicles", vehiclePage);
        model.addAttribute("currentFilter", filter);

        int totalPages = vehiclePage.getTotalPages();
        int nowPage = vehiclePage.getNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if(endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        return "company/sales-list";
    }
}