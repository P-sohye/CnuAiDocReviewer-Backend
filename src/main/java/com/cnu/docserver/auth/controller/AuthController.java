package com.cnu.docserver.auth.controller;

import com.cnu.docserver.auth.dto.LoginRequestDTO;
import com.cnu.docserver.auth.dto.LoginResponseDTO;
import com.cnu.docserver.auth.service.AuthService;
import com.cnu.docserver.user.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인 요청", description = "아이디와 비밀번호로 로그인 처리합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO requestDTO) {
        LoginResponseDTO response = authService.login(requestDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "현재 로그인된 사용자 정보 조회", description = "세션 또는 토큰 기반으로 로그인된 사용자 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> getMyInfo(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(LoginResponseDTO.from(member));
    }
}
