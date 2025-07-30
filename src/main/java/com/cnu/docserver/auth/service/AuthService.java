package com.cnu.docserver.auth.service;

import com.cnu.docserver.auth.dto.LoginRequestDTO;
import com.cnu.docserver.auth.dto.LoginResponseDTO;
import com.cnu.docserver.user.entity.Member;
import com.cnu.docserver.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    public LoginResponseDTO login(LoginRequestDTO request) {
        // 사용자 존재 확인
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // 비밀번호 검증
        if (!member.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("비밀번호 불일치");
        }

        // 성공 시 응답 DTO 생성
        return LoginResponseDTO.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
                .role(member.getRole().name())
                .build();
    }
}
