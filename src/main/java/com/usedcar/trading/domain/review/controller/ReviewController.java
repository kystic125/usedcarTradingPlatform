package com.usedcar.trading.domain.review.controller;

import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.review.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

    /**
     * 리뷰 작성 폼
     */
    @GetMapping("/write/{transactionId}")
    public String reviewForm(@PathVariable Long transactionId, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
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
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            reviewService.createReview(transactionId, userId, rating, content);
            redirectAttributes.addFlashAttribute("message", "리뷰가 등록되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/transactions/" + transactionId;
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
    public String myReviews(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        List<Review> reviews = reviewService.getUserReviews(userId);
        model.addAttribute("reviews", reviews);

        return "review/my-reviews";
    }
}
