package com.usedcar.trading.domain.review.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.review.repository.ReviewRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    // 테스트용 공통 객체
    private User buyer;
    private User seller;
    private Company company;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // 판매자 (회사 소유자)
        seller = User.builder()
                .userId(1L)
                .email("seller@test.com")
                .name("판매자")
                .password("password")
                .phone("010-1234-5678")
                .role(Role.COMPANY_OWNER)
                .build();

        // 구매자 (일반 고객)
        buyer = User.builder()
                .userId(2L)
                .email("buyer@test.com")
                .name("구매자")
                .password("password")
                .phone("010-9876-5432")
                .role(Role.CUSTOMER)
                .build();

        // 회사
        company = Company.builder()
                .companyId(1L)
                .businessName("테스트 중고차")
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .owner(seller)
                .build();

        // 완료된 거래
        transaction = Transaction.builder()
                .transactionId(1L)
                .buyer(buyer)
                .company(company)
                .transactionStatus(TransactionStatus.COMPLETED)
                .build();
    }

    // ==================== 리뷰 작성 테스트 ====================
    @Nested
    @DisplayName("리뷰 작성 (createReview)")
    class CreateReview {

        @Test
        @DisplayName("성공: 완료된 거래에 리뷰 작성")
        void 리뷰작성_성공() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
            given(userRepository.findById(2L)).willReturn(Optional.of(buyer));
            given(reviewRepository.existsByTransactionTransactionId(1L)).willReturn(false);
            given(reviewRepository.save(any(Review.class)))
                    .willAnswer(invocation -> {
                        Review saved = invocation.getArgument(0);
                        return Review.builder()
                                .reviewId(100L)
                                .transaction(saved.getTransaction())
                                .user(saved.getUser())
                                .company(saved.getCompany())
                                .rating(saved.getRating())
                                .content(saved.getContent())
                                .build();
                    });

            // when
            Review result = reviewService.createReview(1L, 2L, 5, "좋은 거래였습니다!");

            // then
            assertThat(result.getReviewId()).isEqualTo(100L);
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getContent()).isEqualTo("좋은 거래였습니다!");
            verify(reviewRepository, times(1)).save(any(Review.class));
            verify(notificationService, times(1)).createNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 완료되지 않은 거래")
        void 리뷰작성_실패_완료되지않은거래() {
            // given
            Transaction pendingTransaction = Transaction.builder()
                    .transactionId(2L)
                    .buyer(buyer)
                    .company(company)
                    .transactionStatus(TransactionStatus.APPROVED)
                    .build();

            given(transactionRepository.findById(2L)).willReturn(Optional.of(pendingTransaction));
            given(userRepository.findById(2L)).willReturn(Optional.of(buyer));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(2L, 2L, 5, "좋은 거래였습니다!"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("완료된 거래에만 리뷰를 작성");
        }

        @Test
        @DisplayName("실패: 구매자가 아닌 사용자")
        void 리뷰작성_실패_구매자아님() {
            // given
            User otherUser = User.builder()
                    .userId(999L)
                    .email("other@test.com")
                    .name("다른사용자")
                    .password("password")
                    .phone("010-0000-0000")
                    .role(Role.CUSTOMER)
                    .build();

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
            given(userRepository.findById(999L)).willReturn(Optional.of(otherUser));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 999L, 5, "좋은 거래였습니다!"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("구매자만 리뷰를 작성");
        }

        @Test
        @DisplayName("실패: 이미 리뷰 작성된 거래")
        void 리뷰작성_실패_중복리뷰() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
            given(userRepository.findById(2L)).willReturn(Optional.of(buyer));
            given(reviewRepository.existsByTransactionTransactionId(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 2L, 5, "좋은 거래였습니다!"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 리뷰를 작성한 거래");
        }

        @Test
        @DisplayName("실패: 평점이 1 미만")
        void 리뷰작성_실패_평점범위_미만() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
            given(userRepository.findById(2L)).willReturn(Optional.of(buyer));
            given(reviewRepository.existsByTransactionTransactionId(1L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 2L, 0, "별로였습니다"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("평점은 1~5 사이");
        }

        @Test
        @DisplayName("실패: 평점이 5 초과")
        void 리뷰작성_실패_평점범위_초과() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
            given(userRepository.findById(2L)).willReturn(Optional.of(buyer));
            given(reviewRepository.existsByTransactionTransactionId(1L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 2L, 6, "최고였습니다!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("평점은 1~5 사이");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 거래")
        void 리뷰작성_실패_거래없음() {
            // given
            given(transactionRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(999L, 2L, 5, "좋은 거래였습니다!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("거래를 찾을 수 없습니다");
        }

            @Test
            @DisplayName("실패: 존재하지 않는 사용자")
            void 리뷰작성_실패_사용자없음() {
                // given
                given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
                given(userRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> reviewService.createReview(1L, 999L, 5, "좋은 거래였습니다!"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("사용자를 찾을 수 없습니다");
            }
    }

    // ==================== 리뷰 조회 테스트 ====================
    @Nested
    @DisplayName("리뷰 조회 (getReview)")
    class GetReview {

        @Test
        @DisplayName("성공: 리뷰 조회")
        void 리뷰조회_성공() {
            // given
            Review review = Review.builder()
                    .reviewId(1L)
                    .transaction(transaction)
                    .user(buyer)
                    .company(company)
                    .rating(5)
                    .content("좋은 거래였습니다!")
                    .build();

            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when
            Review result = reviewService.getReview(1L);

            // then
            assertThat(result.getReviewId()).isEqualTo(1L);
            assertThat(result.getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 리뷰")
        void 리뷰조회_실패_리뷰없음() {
            // given
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getReview(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("리뷰를 찾을 수 없습니다");
        }
    }

    // ==================== 업체 평균 평점 테스트 ====================
    @Nested
    @DisplayName("업체 평균 평점 조회 (getCompanyAverageRating)")
    class GetCompanyAverageRating {

        @Test
        @DisplayName("성공: 평균 평점 조회")
        void 평균평점조회_성공() {
            // given
            given(reviewRepository.findAverageRatingByCompanyId(1L)).willReturn(4.5);

            // when
            Double result = reviewService.getCompanyAverageRating(1L);

            // then
            assertThat(result).isEqualTo(4.5);
        }

        @Test
        @DisplayName("성공: 리뷰 없을 때 0.0 반환")
        void 평균평점조회_리뷰없음() {
            // given
            given(reviewRepository.findAverageRatingByCompanyId(1L)).willReturn(null);

            // when
            Double result = reviewService.getCompanyAverageRating(1L);

            // then
            assertThat(result).isEqualTo(0.0);
        }
    }

    // ==================== 거래 리뷰 조회 테스트 ====================
    @Nested
    @DisplayName("거래 리뷰 조회 (getReviewByTransaction)")
    class GetReviewByTransaction {

        @Test
        @DisplayName("성공: 거래 리뷰 조회")
        void 거래리뷰조회_성공() {
            // given
            Review review = Review.builder()
                    .reviewId(1L)
                    .transaction(transaction)
                    .user(buyer)
                    .company(company)
                    .rating(5)
                    .content("좋은 거래였습니다!")
                    .build();

            given(reviewRepository.findByTransactionTransactionId(1L)).willReturn(Optional.of(review));

            // when
            Review result = reviewService.getReviewByTransaction(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getReviewId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공: 리뷰 없을 때 null 반환")
        void 거래리뷰조회_리뷰없음() {
            // given
            given(reviewRepository.findByTransactionTransactionId(1L)).willReturn(Optional.empty());

            // when
            Review result = reviewService.getReviewByTransaction(1L);

            // then
            assertThat(result).isNull();
        }
    }
}
