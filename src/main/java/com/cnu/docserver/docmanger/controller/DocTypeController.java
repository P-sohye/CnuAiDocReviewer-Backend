package com.cnu.docserver.docmanger.controller;

import com.cnu.docserver.docmanger.dto.DocTypeEditResponseDTO;
import com.cnu.docserver.docmanger.dto.DocTypeRequestDTO;
import com.cnu.docserver.docmanger.dto.DocTypeResponseDTO;
import com.cnu.docserver.docmanger.service.DocTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/documents")
@Tag(name = "DocType", description = "서류 유형 및 필수 항목 등록 API")
public class DocTypeController {

    private final DocTypeService docTypeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "서류 유형 등록", description = "부서 ID, 제목, 필수 항목 및 파일을 등록합니다.")
    public String registerDocType(@ModelAttribute @Valid DocTypeRequestDTO request) {
        docTypeService.registerDocType(
                request.getDepartmentId(),
                request.getTitle(),
                request.getRequiredFields(),
                request.getExampleValues(),
                request.getFile()
        );
        return "등록 완료";
    }

    @GetMapping
    @Operation(summary = "부서별 문서 목록 조회", description = "부서 ID에 해당하는 모든 서류 제목 및 필수 항목을 반환합니다.")
    public List<DocTypeResponseDTO> getDocTypesByDepartmentId(
            @RequestParam("departmentId") Integer departmentId
    ) {
        return docTypeService.getDocTypesByDepartment(departmentId);
    }

    @GetMapping("/{docTypeId}")
    @Operation(summary = "문서 수정용 데이터 조회(단건 조회)", description = "문서 ID에 해당하는 제목, 파일 URL, 필수 항목, 예시 목록을 반환합니다.")
    public DocTypeEditResponseDTO getDocTypeForEdit(@PathVariable Integer docTypeId) {
        return docTypeService.getDocTypeForEdit(docTypeId);
    }
}
