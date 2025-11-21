package com.usedcar.trading.domain.user.repository;

import com.usedcar.trading.domain.user.entity.Provider;
import com.usedcar.trading.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 회원가입 - 이메일 중복 체크
    boolean existsByEmail(String email);

    // 로그인 - 이메일로 찾기
    Optional<User> findByEmail(String email);

    // 아이디 찾기 - 이름, 전화번호
    Optional<User> findByNameAndPhone(String name, String phone);

    // 비밀번호 찾기 - 이메일, 이름, 전화번호
    Optional<User> findByEmailAndNameAndPhone(String email, String name, String phone);

    // 소셜 가입 여부 확인
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
