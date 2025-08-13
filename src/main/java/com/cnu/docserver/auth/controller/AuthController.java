package com.cnu.docserver.auth.controller;

import com.cnu.docserver.auth.dto.LoginRequestDTO;
import com.cnu.docserver.auth.dto.LoginResponseDTO;
import com.cnu.docserver.auth.service.AuthService;
import com.cnu.docserver.user.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 처리: ID, PW 기반 로그인 후 세션에 사용자 저장
     */
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하고 세션에 사용자 정보를 저장합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody LoginRequestDTO requestDTO,
            HttpSession session
    ) {
        // 로그인 처리 및 사용자 정보 획득
        LoginResponseDTO response = authService.login(requestDTO);

        // 로그인한 사용자 정보를 세션에 저장
        Member loginMember = authService.findMemberById(response.getMemberId());
        session.setAttribute("loginUser", loginMember);

        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인된 사용자 정보 반환 (세션 기반)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> getMyInfo(HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginUser");

        if (loginMember == null) {
            return ResponseEntity.status(401).build(); // UNAUTHORIZED
        }

        return ResponseEntity.ok(LoginResponseDTO.from(loginMember));
    }
}
