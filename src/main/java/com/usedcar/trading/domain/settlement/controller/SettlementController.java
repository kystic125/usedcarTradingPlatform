package com.usedcar.trading.domain.settlement.controller;

import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.service.SettlementService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final UserRepository userRepository;

    /**
     * 내 업체 정산 목록 [STL-004]
     */
    @GetMapping
    public String mySettlements(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

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

    /**
     * 정산 상세 조회
     */
    @GetMapping("/{id}")
    public String settlementDetail(@PathVariable Long id, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Settlement settlement = settlementService.getSettlement(id);
        model.addAttribute("settlement", settlement);

        return "settlement/detail";
    }

    /**
     * 기간별 정산 조회 [STL-005]
     */
    @GetMapping("/period")
    public String settlementsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

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

    /**
     * 총 정산액 조회 (AJAX)
     */
    @GetMapping("/total")
    @ResponseBody
    public BigDecimal getTotalAmount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return BigDecimal.ZERO;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getCompany() == null) {
            return BigDecimal.ZERO;
        }

        return settlementService.getTotalSettlementAmount(user.getCompany().getCompanyId());
    }
}
