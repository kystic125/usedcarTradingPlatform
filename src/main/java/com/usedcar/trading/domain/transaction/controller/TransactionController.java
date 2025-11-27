package com.usedcar.trading.domain.transaction.controller;

import com.usedcar.trading.domain.transaction.service.TransactionService;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    // 구매 요청
    @PostMapping("/request")
    public String requestTransaction(@RequestParam Long vehicleId,
                                     @AuthenticationPrincipal Object principal,
                                     HttpServletResponse response) throws IOException {
        User buyer = findUser(principal);

        try {
            transactionService.requestTransaction(vehicleId, buyer);
        } catch (IllegalStateException e) {
            return showAlertAndBack(response, e.getMessage());
        }

        return "redirect:/vehicles/" + vehicleId;
    }

    // 거래 승인
    @PostMapping("/{id}/approve")
    public String approveTransaction(@PathVariable Long id,
                                     @AuthenticationPrincipal Object principal,
                                     HttpServletResponse response) throws IOException {
        User seller = findUser(principal);
        try {
            transactionService.approveTransaction(id, seller);
        } catch (IllegalStateException e) {
            return showAlertAndBack(response, e.getMessage());
        }
        return "redirect:/company/sales";
    }

    // 거래 거부
    @PostMapping("/{id}/reject")
    public String rejectTransaction(@PathVariable Long id,
                                    @AuthenticationPrincipal Object principal,
                                    HttpServletResponse response) throws IOException {
        User seller = findUser(principal);
        try {
            transactionService.rejectTransaction(id, seller);
        } catch (IllegalStateException e) {
            return showAlertAndBack(response, e.getMessage());
        }
        return "redirect:/company/sales";
    }

    // 거래 완료
    @PostMapping("/{id}/complete")
    public String completeTransaction(@PathVariable Long id,
                                      @AuthenticationPrincipal Object principal,
                                      HttpServletResponse response) throws IOException {
        User seller = findUser(principal);
        try {
            transactionService.completeTransaction(id, seller);
        } catch (IllegalStateException e) {
            return showAlertAndBack(response, e.getMessage());
        }
        return "redirect:/company/sales";
    }

    // 거래 취소
    @PostMapping("/{id}/cancel")
    public String cancelTransaction(@PathVariable Long id,
                                    @AuthenticationPrincipal Object principal,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        User user = findUser(principal);
        try {
            transactionService.cancelTransaction(id, user);
        } catch (IllegalStateException e) {
            return showAlertAndBack(response, e.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }

        if (user.getRole() == Role.COMPANY_OWNER || user.getRole() == Role.COMPANY_EMPLOYEE) {
            return "redirect:/company/sales";
        }
        return "redirect:/mypage/purchases";
    }

    private String showAlertAndBack(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<script>alert('" + message + "'); history.go(-1);</script>");
        out.flush();
        return null;
    }

    // 유저 추출 헬퍼
    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow();
        }
        throw new IllegalArgumentException("로그인이 필요합니다.");
    }
}