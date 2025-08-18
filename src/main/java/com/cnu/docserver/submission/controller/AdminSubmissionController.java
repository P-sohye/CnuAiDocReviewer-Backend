package com.cnu.docserver.submission.controller;

import com.cnu.docserver.submission.dto.AdminDecisionRequestDTO;
import com.cnu.docserver.submission.dto.SubmissionDetailDTO;
import com.cnu.docserver.submission.dto.SubmissionSummaryDTO;
import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.enums.SubmissionStatus;
import com.cnu.docserver.submission.service.AdminSubmissionService;
import com.cnu.docserver.user.entity.Member;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Review", description = "관리자 제출 검토 API")
@RestController
@RequestMapping("/api/admin/submissions")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final AdminSubmissionService adminSubmissionService;

    // 검토 대기 목록
    @GetMapping
    public List<SubmissionSummaryDTO> listForAdmin(@RequestParam Integer departmentId) {
        return adminSubmissionService.listAdminQueue(departmentId);
    }

    // 상세 조회 (필요시)
    @GetMapping("/{id}")
    public SubmissionDetailDTO getOne(@PathVariable Integer id) {
        return adminSubmissionService.getDetail(id); // DTO로 매핑해서 리턴
    }

    // 승인
    @PostMapping("/{id}/approve")
    public SubmissionSummaryDTO approve(@PathVariable Integer id) {
        Member adminMember = (Member) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return adminSubmissionService.approve(id, adminMember); // memo 인자 제거
    }
    // 반려
    @PostMapping("/{id}/reject")
    public SubmissionSummaryDTO reject(@PathVariable Integer id, @RequestBody AdminDecisionRequestDTO body) {
        Member adminMember = (Member) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String reason = (body != null && body.getMemo()!=null) ? body.getMemo() : "사유 미기재";
        return adminSubmissionService.reject(id, adminMember, reason);
    }
}
