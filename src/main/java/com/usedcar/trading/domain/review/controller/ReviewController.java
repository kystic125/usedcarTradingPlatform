package com.usedcar.trading.domain.review.controller;

import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.review.service.ReviewService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    /**
     * 리뷰 작성 폼
     */
    @GetMapping("/write/{transactionId}")
    public String reviewForm(@PathVariable Long transactionId,
                             Model model,
                             @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("transactionId", transactionId);
        return "review/write";
    }

    /**
     * 리뷰 작성 처리 [REV-001]
     */
    @PostMapping("/write")
    public String createReview(@RequestParam Long transactionId,
                               @RequestParam int rating,
                               @RequestParam(required = false) String content,
                               @AuthenticationPrincipal Object principal,
                               RedirectAttributes redirectAttributes) {
        User user = findUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            reviewService.createReview(transactionId, user.getUserId(), rating, content);
            redirectAttributes.addFlashAttribute("message", "리뷰가 등록되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mypage/purchases";
    }

    /**
     * 리뷰 상세 조회 [REV-002]
     */
    @GetMapping("/{id}")
    public String reviewDetail(@PathVariable Long id, Model model) {
        Review review = reviewService.getReview(id);
        model.addAttribute("review", review);
        return "review/detail";
    }

    /**
     * 업체 리뷰 목록
     */
    @GetMapping("/company/{companyId}")
    public String companyReviews(@PathVariable Long companyId, Model model) {
        List<Review> reviews = reviewService.getCompanyReviews(companyId);
        Double avgRating = reviewService.getCompanyAverageRating(companyId);
        int reviewCount = reviewService.getCompanyReviewCount(companyId);

        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("companyId", companyId);

        return "review/company-reviews";
    }

    /**
     * 내가 작성한 리뷰 목록
     */
    @GetMapping("/my")
    public String myReviews(Model model,
                            @AuthenticationPrincipal Object principal,
                            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = findUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        Page<Review> reviewPage = reviewService.getUserReviews(user.getUserId(), pageable);

        model.addAttribute("reviews", reviewPage);

        int totalPages = reviewPage.getTotalPages();
        int nowPage = reviewPage.getNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if (endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        return "review/my-reviews";
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElse(null);
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            String providerId = String.valueOf(oauthUser.getAttributes().get("id"));
            return userRepository.findByProviderId(providerId).orElse(null);
        }
        return null;
    }
}
