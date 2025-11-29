package com.usedcar.trading.domain.report.controller;

import com.usedcar.trading.domain.report.entity.Report;
import com.usedcar.trading.domain.report.entity.ReportType;
import com.usedcar.trading.domain.report.service.ReportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

    /**
     * 신고 작성 폼
     */
    @GetMapping("/write")
    public String reportForm(@RequestParam ReportType type,
                             @RequestParam Long targetId,
                             Model model,
                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("reportType", type);
        model.addAttribute("targetId", targetId);
        return "report/write";
    }

    /**
     * 신고 등록 [RPT-001]
     */
    @PostMapping("/write")
    public String createReport(@RequestParam ReportType type,
                               @RequestParam Long targetId,
                               @RequestParam String description,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            reportService.createReport(userId, type, targetId, description);
            redirectAttributes.addFlashAttribute("message", "신고가 접수되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return getRedirectUrl(type, targetId);
    }

    /**
     * 신고 상세 조회 [RPT-002]
     */
    @GetMapping("/{id}")
    public String reportDetail(@PathVariable Long id, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Report report = reportService.getReport(id);

        // 본인 신고 또는 관리자만 조회 가능
        if (!report.getReporter().getUserId().equals(userId)) {
            // TODO: 관리자 권한 체크 추가
        }

        model.addAttribute("report", report);
        return "report/detail";
    }

    /**
     * 내 신고 목록 [RPT-003]
     */
    @GetMapping("/my")
    public String myReports(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        List<Report> reports = reportService.getMyReports(userId);
        model.addAttribute("reports", reports);

        return "report/my-reports";
    }

    private String getRedirectUrl(ReportType type, Long targetId) {
        return switch (type) {
            case VEHICLE -> "redirect:/vehicles/" + targetId;
            case COMPANY -> "redirect:/companies/" + targetId;
            case USER -> "redirect:/users/" + targetId;
        };
    }
}
