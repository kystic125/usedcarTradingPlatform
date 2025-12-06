package com.usedcar.trading.domain.review.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.review.repository.ReviewRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    /**
     * 리뷰 작성 [REV-001]
     */
    @Transactional
    public Review createReview(Long transactionId, Long userId, int rating, String content) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 거래 완료 상태인지 확인
        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("완료된 거래에만 리뷰를 작성할 수 있습니다.");
        }

        // 구매자인지 확인
        if (!transaction.getBuyer().getUserId().equals(userId)) {
            throw new IllegalStateException("해당 거래의 구매자만 리뷰를 작성할 수 있습니다.");
        }

        // 이미 리뷰를 작성했는지 확인
        if (reviewRepository.existsByTransactionTransactionId(transactionId)) {
            throw new IllegalStateException("이미 리뷰를 작성한 거래입니다.");
        }

        // 평점 유효성 검사
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }

        Review review = Review.builder()
                .transaction(transaction)
                .user(user)
                .company(transaction.getCompany())
                .rating(rating)
                .content(content)
                .build();

        Review savedReview = reviewRepository.save(review);

        Company company = transaction.getCompany();
        User dealer = transaction.getVehicle().getRegisteredBy().getUser();
        User boss = company.getOwner();

        String message = String.format("새로운 리뷰가 등록되었습니다. (평점: %d점)", rating);
        String link = "/mypage";

        // 1. 딜러에게 알림
        notificationService.createNotification(dealer, NotificationType.REVIEW_RECEIVED, message, link);

        // 2. 사장님에게 알림
        if (!dealer.getUserId().equals(boss.getUserId())) {
            notificationService.createNotification(boss, NotificationType.REVIEW_RECEIVED, message, link);
        }

        log.info("리뷰 작성 완료...");
        return savedReview;
    }

    /**
     * 리뷰 조회 [REV-002]
     */
    public Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
    }

    /**
     * 업체 리뷰 목록 조회
     */
    public List<Review> getCompanyReviews(Long companyId) {
        return reviewRepository.findByCompanyCompanyId(companyId);
    }

    /**
     * 사용자가 작성한 리뷰 목록 조회
     */
    public List<Review> getUserReviews(Long userId) {
        return reviewRepository.findByUserUserId(userId);
    }

    /**
     * 업체 평균 평점 조회
     */
    public Double getCompanyAverageRating(Long companyId) {
        Double avgRating = reviewRepository.findAverageRatingByCompanyId(companyId);
        return avgRating != null ? avgRating : 0.0;
    }

    /**
     * 업체 리뷰 개수 조회
     */
    public int getCompanyReviewCount(Long companyId) {
        return reviewRepository.countByCompanyCompanyId(companyId);
    }

    /**
     * 거래 리뷰 조회
     */
    public Review getReviewByTransaction(Long transactionId) {
        return reviewRepository.findByTransactionTransactionId(transactionId)
                .orElse(null);
    }

    // 판매자 리뷰 수 조회
    public long getUserReviewCount(Long userId) {
        return reviewRepository.countByUserUserId(userId);
    }

    public Page<Review> getCompanyReviews(Long companyId, Pageable pageable) {
        return reviewRepository.findByCompanyCompanyId(companyId, pageable);
    }

    public Page<Review> getUserReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserUserId(userId, pageable);
    }
}
