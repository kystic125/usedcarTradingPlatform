package com.usedcar.trading.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    TRANSACTION_REQUEST("거래 요청", "새로운 거래 요청이 있습니다"),
    TRANSACTION_APPROVED("거래 승인", "거래가 승인되었습니다"),
    TRANSACTION_REJECTED("거래 거절", "거래가 거절되었습니다"),
    TRANSACTION_COMPLETED("거래 완료", "거래가 완료되었습니다"),
    TRANSACTION_CANCELLED("거래 취소", "거래가 취소되었습니다"),

    VEHICLE_APPROVED("매물 승인", "매물이 승인되었습니다"),
    VEHICLE_REJECTED("매물 반려", "매물이 반려되었습니다"),
    VEHICLE_EXPIRING("매물 만료 임박", "매물이 곧 만료됩니다"),
    VEHICLE_EXPIRED("매물 만료", "매물이 만료되었습니다"),

    SETTLEMENT_COMPLETED("정산 완료", "정산이 완료되었습니다"),

    REVIEW_RECEIVED("리뷰 등록", "새로운 리뷰가 등록되었습니다"),

    REPORT_PROCESSED("신고 처리", "신고가 처리되었습니다"),

    SYSTEM("시스템 알림", "시스템 알림");

    private final String title;
    private final String defaultMessage;
}
