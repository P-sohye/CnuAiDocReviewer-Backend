package com.cnu.docserver.submission.controller;

import com.cnu.docserver.submission.dto.SubmissionSummaryDTO;
import com.cnu.docserver.submission.dto.SubmitRequestDTO;
import com.cnu.docserver.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
@Tag(name = "Student Submission", description = "학생 학생 제출/수정/최종제출 API")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
            summary = "최초 제출",
            description = "문서 유형 ID, 필드 JSON, 파일을 멀티파트로 제출합니다. 마감일 당일까지 허용."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제출 성공",
                    content = @Content(schema = @Schema(implementation = SubmissionSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "유효성 오류(파일 누락/마감 초과 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionSummaryDTO create(
            @Parameter(description = "문서 유형 ID", required = true, example = "101")
            @RequestParam Integer docTypeId,

            @Parameter(description = "필드 값 JSON (예: [{\"label\":\"학번\",\"value\":\"20231234\"}])")
            @RequestParam(required = false) String fieldsJson,

            @Parameter(description = "제출 파일", required = true)
            @RequestPart("file") MultipartFile file,

            HttpSession session
    ) {
        return submissionService.create(docTypeId, fieldsJson, file, session);
    }
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
            summary = "반려 후 수정(임시저장)",
            description = "파일/필드 일부 또는 전체를 덮어씁니다. 상태는 즉시 제출로 바뀌지 않습니다."
    )
    @PutMapping(value="/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionSummaryDTO update(
            @Parameter(description = "제출 ID", example = "12") @PathVariable Integer submissionId,
            @Parameter(description = "필드 값 JSON") @RequestParam(required=false) String fieldsJson,
            @Parameter(description = "수정 파일") @RequestPart(value="file", required=false) MultipartFile file
    ) {
        return submissionService.update(submissionId, fieldsJson, file);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
            summary = "제출(바로/최종)",
            description = "mode=FINAL이면 최종 확정 제출, mode=DIRECT면 AI 검증을 건너뛰고 관리자 검토로 바로 제출합니다. 마감일 당일까지 허용."
    )
    @PostMapping(value="/{submissionId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SubmissionSummaryDTO submit(
            @Parameter(description = "제출 ID", example = "12") @PathVariable Integer submissionId,
            @RequestBody SubmitRequestDTO body
    ) {
        return submissionService.submit(submissionId, body);
    }


}
