package com.usedcar.trading.domain.admin.controller;

import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.entity.SettlementStatus;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final SettlementRepository settlementRepository;

    // 정산 대기 목록 조회
    @GetMapping
    public String settlementList(Model model,
                                 @RequestParam(required = false, defaultValue = "PENDING") SettlementStatus status) {

        List<Settlement> settlements = settlementRepository.findBySettlementStatus(status);

        model.addAttribute("settlements", settlements);
        model.addAttribute("currentStatus", status);


        long pendingCount = settlementRepository.countBySettlementStatus(SettlementStatus.PENDING);
        model.addAttribute("pendingCount", pendingCount);

        return "admin/settlement-list";
    }

    // 정산 승인 (입금 완료 처리)
    @PostMapping("/{id}/approve")
    @Transactional
    public String approveSettlement(@PathVariable Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("정산 건이 없습니다."));

        if (settlement.getSettlementStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 정산입니다.");
        }

        settlement.complete();

        return "redirect:/admin/settlements";
    }
}