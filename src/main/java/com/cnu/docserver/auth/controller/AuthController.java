package com.cnu.docserver.auth.controller;

import com.cnu.docserver.auth.dto.LoginRequestDTO;
import com.cnu.docserver.auth.dto.LoginResponseDTO;
import com.cnu.docserver.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO requestDTO) {
        LoginResponseDTO response = authService.login(requestDTO);
        return ResponseEntity.ok(response);

    }
}
