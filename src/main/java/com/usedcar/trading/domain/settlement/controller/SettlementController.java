package com.usedcar.trading.domain.settlement.controller;

import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.service.SettlementService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // 내 업체 정산 목록
    @GetMapping
    public String mySettlements(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
        User user = principal.getUser();

        if (user.getCompany() == null) {
            model.addAttribute("error", "소속된 업체가 없습니다.");
            return "settlement/list";
        }

        Long companyId = user.getCompany().getCompanyId();
        List<Settlement> settlements = settlementService.getCompanySettlements(companyId);
        BigDecimal totalAmount = settlementService.getTotalSettlementAmount(companyId);

        model.addAttribute("settlements", settlements);
        model.addAttribute("totalAmount", totalAmount);

        return "settlement/list";
    }

    // 정산 상세 조회
    @GetMapping("/{id}")
    public String settlementDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal PrincipalDetails principal) {

        Settlement settlement = settlementService.getSettlement(id);
        model.addAttribute("settlement", settlement);

        return "settlement/detail";
    }

    // 기간별 정산 조회
    @GetMapping("/period")
    public String settlementsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal PrincipalDetails principal,
            Model model) {

        User user = principal.getUser();

        if (user.getCompany() == null) {
            model.addAttribute("error", "소속된 업체가 없습니다.");
            return "settlement/list";
        }

        Long companyId = user.getCompany().getCompanyId();
        List<Settlement> settlements = settlementService.getCompanySettlementsByPeriod(companyId, start, end);
        BigDecimal totalAmount = settlementService.getTotalSettlementAmountByPeriod(companyId, start, end);

        model.addAttribute("settlements", settlements);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);

        return "settlement/list";
    }

    // 총 정산액 조회
    @GetMapping("/total")
    @ResponseBody
    public BigDecimal getTotalAmount(@AuthenticationPrincipal PrincipalDetails principal) {

        User user = principal.getUser();
        if (user.getCompany() == null) {
            return BigDecimal.ZERO;
        }

        return settlementService.getTotalSettlementAmount(user.getCompany().getCompanyId());
    }
}
