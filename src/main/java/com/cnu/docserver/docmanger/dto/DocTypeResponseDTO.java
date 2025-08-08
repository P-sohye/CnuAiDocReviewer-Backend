package com.cnu.docserver.docmanger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class DocTypeResponseDTO {

    @Schema(description = "문서 유형 ID", example = "1")
    private Integer docTypeId;

    @Schema(description = "문서 제목", example = "장학금 신청서")
    private String title;

    @Schema(description = "필수 항목 목록", example = "[\"이름\", \"학번\"]")
    private List<String> requiredFields;

}