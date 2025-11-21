package com.usedcar.trading.domain.user.dto;

import com.usedcar.trading.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 * 
 * 사용자 정보 조회 시 반환
 * Entity의 민감한 정보(password 등)를 제외하고 필요한 것만 반환
 */
@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String email;
    private String name;
    private String phone;
    private String role;
    private String status;
    private String provider;  // LOCAL, KAKAO 등
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환 정적 팩토리 메서드
     *
     * 장점:
     * - 변환 로직을 한 곳에서 관리
     * - Entity 변경 시 이 메서드만 수정하면 됨
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getUserStatus().name())
                .provider(user.getProvider().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
