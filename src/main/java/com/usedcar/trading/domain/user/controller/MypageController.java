package com.usedcar.trading.domain.user.controller;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.favorite.service.FavoriteService;
import com.usedcar.trading.domain.review.service.ReviewService;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.service.UserService;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;
    private final TransactionRepository transactionRepository;
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;

    @GetMapping("/mypage")
    public String myPage(Model model, @AuthenticationPrincipal PrincipalDetails principal) {

        User user = principal.getUser();

        model.addAttribute("user", user);

        long activeListingsCount = 0;
        long favoriteCount = 0;
        long reviewCount = 0;
        Double avgRating = 0.0;

        if (user.getRole() == Role.COMPANY_OWNER || user.getRole() == Role.COMPANY_EMPLOYEE) {
            Company company = null;

            if (user.getRole() == Role.COMPANY_OWNER) {
                company = companyRepository.findByOwner_UserId(user.getUserId()).orElse(null);
            } else if (user.getEmployee() != null) {
                company = user.getEmployee().getCompany();
            }

            if (company != null) {
                activeListingsCount = vehicleRepository.countByCompanyAndVehicleStatus(company, VehicleStatus.SALE);

                avgRating = reviewService.getCompanyAverageRating(company.getCompanyId());
                if (avgRating == null) avgRating = 0.0;
            }
        }

        if (user.getRole() == Role.CUSTOMER) {
            favoriteCount = favoriteService.getMyFavoriteCount(user.getUserId());
            reviewCount = reviewService.getUserReviewCount(user.getUserId());
        }

        model.addAttribute("activeListingsCount", activeListingsCount);
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("avgRating", avgRating);

        return "mypage";
    }

    // 1. 정보 수정 페이지 보여주기
    @GetMapping("/mypage/settings/edit")
    public String editPage(Model model, @AuthenticationPrincipal PrincipalDetails principal) {
        User user = principal.getUser();
        model.addAttribute("user", user);
        return "user-edit-profile";
    }

    @GetMapping("/mypage/settings/social")
    public String settingsSocial(Model model, @AuthenticationPrincipal PrincipalDetails principal) {
        User user = principal.getUser();
        model.addAttribute("user", user);
        return "user-edit-social";
    }

    @GetMapping("/mypage/settings/dealer")
    public String settingsDealer(Model model, @AuthenticationPrincipal PrincipalDetails principal) {
        User user = principal.getUser();
        model.addAttribute("user", user);
        return "user-edit-dealer";
    }

    @GetMapping("/mypage/settings/delete")
    public String settingsDelete(Model model, @AuthenticationPrincipal PrincipalDetails principal) {
        User user = principal.getUser();
        model.addAttribute("user", user);
        return "user-edit-delete";
    }

    // 회원 탈퇴 처리
    @PostMapping("/mypage/withdraw")
    public String withdrawProcess(@AuthenticationPrincipal PrincipalDetails principal) {
        User user = principal.getUser();
        userService.withdrawUser(user.getUserId());
        return "redirect:/logout";
    }

    // 2. 정보 수정 처리
    @PostMapping("/mypage/update")
    public String updateProcess(@RequestParam String email,
                                @RequestParam String phone,
                                @AuthenticationPrincipal PrincipalDetails principal) {

        User user = principal.getUser();

        boolean isEmailChanged = !user.getEmail().equals(email);

        // 정보 수정 시도
        try {
            userService.updateUserInfo(user.getUserId(), email, phone);
        } catch (IllegalArgumentException e) {
            return "redirect:/mypage/settings/edit?error=" + e.getMessage();
        }

        if (isEmailChanged) {
            return "redirect:/logout";
        } else {
            return "redirect:/mypage";
        }
    }

    // 구매 내역 조회
    @GetMapping("/mypage/purchases")
    public String myPurchases(Model model,
                              @AuthenticationPrincipal PrincipalDetails principal,
                              @PageableDefault(size = 10) Pageable pageable) {
        User user = principal.getUser();

        List<Transaction> allTransactions = transactionRepository.findByBuyerOrderByCreatedAtDesc(user);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTransactions.size());
        List<Transaction> pagedList = (start > allTransactions.size()) ? List.of() : allTransactions.subList(start, end);

        Page<Transaction> transactionPage = new PageImpl<>(pagedList, pageable, allTransactions.size());

        model.addAttribute("transactions", transactionPage);

        int totalPages = transactionPage.getTotalPages();
        int nowPage = transactionPage.getNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if(endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        return "mypage/purchase-list";
    }
}