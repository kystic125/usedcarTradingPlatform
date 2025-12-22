package com.usedcar.trading.domain.report.controller;

import com.usedcar.trading.domain.report.entity.Report;
import com.usedcar.trading.domain.report.entity.ReportType;
import com.usedcar.trading.domain.report.service.ReportService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    
    // 신고 작성 폼
    @GetMapping("/write")
    public String reportForm(@RequestParam ReportType type,
                             @RequestParam Long targetId,
                             Model model,
                             @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        model.addAttribute("reportType", type);
        model.addAttribute("targetId", targetId);
        return "report/write";
    }

    // 신고 등록
    @PostMapping("/write")
    public String createReport(@RequestParam ReportType type,
                               @RequestParam Long targetId,
                               @RequestParam String description,
                               @AuthenticationPrincipal PrincipalDetails principal,
                               RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = principal.getUser();

        try {
            reportService.createReport(user.getUserId(), type, targetId, description);
            redirectAttributes.addFlashAttribute("message", "신고가 접수되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return getRedirectUrl(type, targetId);
    }
    
    // 신고 상세 조회
    @GetMapping("/{id}")
    public String reportDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = principal.getUser();
        Report report = reportService.getReport(id);

        // 본인 신고 또는 관리자만 조회 가능
        if (!report.getReporter().getUserId().equals(user.getUserId())) {
            // TODO: 관리자 권한 체크 추가
            return "redirect:/";
        }

        model.addAttribute("report", report);
        return "report/detail";
    }

    // 내 신고 목록 조회
    @GetMapping("/my")
    public String myReports(Model model, @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = principal.getUser();

        List<Report> reports = reportService.getMyReports(user.getUserId());
        model.addAttribute("reports", reports);

        return "report/my-reports";
    }

    private String getRedirectUrl(ReportType type, Long targetId) {
        return switch (type) {
            case VEHICLE -> "redirect:/vehicles/" + targetId;
            case COMPANY -> "redirect:/company/sales";
            case USER -> "redirect:/";
        };
    }
}
