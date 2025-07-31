package com.cnu.docserver.auth.dto;

import lombok.*;;


@Getter
@Builder
public class LoginResponseDTO {

    private String memberId;
    private String name;
    private String role;
    private String department;
    private String academicStatus;
}
