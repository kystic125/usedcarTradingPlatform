package com.usedcar.trading.domain.review.repository;

import com.usedcar.trading.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 고객이 작성한 리뷰 목록 조회
    List<Review> findByUserUserId(Long userId);

    // 업체가 받은 리뷰 목록 조회
    List<Review> findByCompanyCompanyId(Long CompanyId);

    // 업체 리뷰 평균 점수 조회
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.company.companyId = :companyId")
    Double findAverageRatingByCompanyId(@Param("companyId") Long companyId);

    /**
     * 1. @Query: "이 JPQL로 평균 구해줘"
     * 2. :companyId: "이게 뭔데?"
     * 3. @Param("companyId"): ":companyId는 companyId야"
     * 4. Long companyId: "그리고 companyId는 내가 호출할 때 넣어줄게"
     */

    // 특정 거래 평점 조회
    Optional<Review> findByTransactionTransactionId(Long transactionId);

    // 거래 리뷰가 존재하는지 조회
    boolean existsByTransactionTransactionId(Long transactionId);

    // 업체 리뷰 갯수 조회
    int countByCompanyCompanyId(Long companyId);

    // 업체의 특정 평점 리뷰만 조회
    List<Review> findByCompanyCompanyIdAndRating(Long companyId, int rating);

    long countByUserUserId(Long userId);

    Page<Review> findByCompanyCompanyId(Long companyId, Pageable pageable);
}
