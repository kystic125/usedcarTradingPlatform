package com.usedcar.trading.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 상태입니다"),

    // 401 Unauthorized
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "아이디 또는 비밀번호가 일치하지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다"),
    INACTIVE_USER(HttpStatus.FORBIDDEN, "AUTH_005", "비활성화된 계정입니다"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다"),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_001", "존재하지 않는 업체입니다"),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMPLOYEE_001", "존재하지 않는 직원입니다"),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "VEHICLE_001", "존재하지 않는 매물입니다"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSACTION_001", "존재하지 않는 거래입니다"),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT_001", "존재하지 않는 정산입니다"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_001", "존재하지 않는 리뷰입니다"),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_001", "존재하지 않는 신고입니다"),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAVORITE_001", "존재하지 않는 찜입니다"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_001", "존재하지 않는 알림입니다"),

    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 사용중인 이메일입니다"),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "COMPANY_002", "이미 등록된 사업자번호입니다"),
    ALREADY_IN_TRANSACTION(HttpStatus.CONFLICT, "TRANSACTION_002", "이미 거래 진행 중인 매물입니다"),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "REVIEW_002", "이미 리뷰를 작성한 거래입니다"),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "REPORT_002", "이미 신고한 대상입니다"),
    ALREADY_FAVORITED(HttpStatus.CONFLICT, "FAVORITE_002", "이미 찜한 매물입니다"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
