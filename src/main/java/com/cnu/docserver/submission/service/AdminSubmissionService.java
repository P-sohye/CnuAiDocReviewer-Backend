package com.cnu.docserver.submission.service;

import com.cnu.docserver.submission.dto.HistoryDTO;
import com.cnu.docserver.submission.dto.SubmissionDetailDTO;
import com.cnu.docserver.submission.dto.SubmissionSummaryDTO;
import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.entity.SubmissionFile;
import com.cnu.docserver.submission.entity.SubmissionHistory;
import com.cnu.docserver.submission.enums.HistoryAction;
import com.cnu.docserver.submission.enums.SubmissionStatus;
import com.cnu.docserver.submission.repository.SubmissionFileRepository;
import com.cnu.docserver.submission.repository.SubmissionHistoryRepository;
import com.cnu.docserver.submission.repository.SubmissionRepository;
import com.cnu.docserver.user.entity.Admin;
import com.cnu.docserver.user.entity.Member;
import com.cnu.docserver.user.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;
    private final AdminRepository adminRepository;
    private final SubmissionFileRepository submissionFileRepository; // ★ 추가

    @Transactional(readOnly = true)
    public SubmissionDetailDTO getDetail(Integer id) {
        Submission s = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출을 찾을 수 없습니다."));

        // 파일 URL / 파일명
        String fileUrl = submissionFileRepository.findBySubmission(s)
                .map(SubmissionFile::getFileUrl)
                .orElse(null);
        String fileName = extractFileName(fileUrl);

        // 문서 유형명 (엔티티 필드명에 맞게)
        String docTypeName = (s.getDocType() != null ? s.getDocType().getTitle() : null);

        // 히스토리
        List<HistoryDTO> history = submissionHistoryRepository.findBySubmissionOrderByChangedAtAsc(s)
                .stream()
                .map(h -> new HistoryDTO(
                        h.getSubmissionHistoryId(),
                        h.getAction() == null ? null : h.getAction().name(),
                        h.getMemo(),
                        resolveAdminName(h.getAdmin()),
                        h.getChangedAt() == null ? null : h.getChangedAt().toString()
                ))
                .toList();

        // 학생 정보 (엔티티 구조에 맞춰 안전하게 꺼내기)
        String studentId   = s.getStudent() == null ? null : s.getStudent().getStudentId();
        String studentName = (s.getStudent() != null && s.getStudent().getMember() != null)
                ? s.getStudent().getMember().getName() : null;

        return new SubmissionDetailDTO(
                s.getSubmissionId(),
                s.getStatus() == null ? null : s.getStatus().name(),
                s.getSubmittedAt() == null ? null : s.getSubmittedAt().toString(),
                studentId,
                studentName,
                fileUrl,
                docTypeName,
                fileName,
                history
        );
    }

    private String resolveAdminName(Admin admin) {
        if (admin == null) return "학생/시스템";
        // Admin -> Member -> 이름(또는 memberId) 경로에 맞게 꺼내세요.
        if (admin.getMember() != null && admin.getMember().getName() != null) {
            return admin.getMember().getName();
        }
        return admin.getAdminId() != null ? admin.getAdminId().toString() : "관리자";
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        int idx = Math.max(fileUrl.lastIndexOf('/'), fileUrl.lastIndexOf('\\'));
        return (idx >= 0 && idx < fileUrl.length() - 1) ? fileUrl.substring(idx + 1) : fileUrl;
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummaryDTO> listByStatus(SubmissionStatus status) {
        return submissionRepository.findByStatusOrderBySubmittedAtAsc(status)
                .stream().map(this::toSummary).toList();
    }

    @Transactional
    public SubmissionSummaryDTO approve(Integer submissionId, Member adminMember, String memo) {
        Submission s = requireReviewable(submissionId);
        Admin admin = adminRepository.findByMember(adminMember)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다."));

        s.setStatus(SubmissionStatus.APPROVED);
        submissionRepository.save(s);
        saveHistory(s, admin, HistoryAction.APPROVED, memo == null ? "관리자 승인" : memo);

        return toSummary(s);
    }

    @Transactional
    public SubmissionSummaryDTO reject(Integer submissionId, Member adminMember, String reason) {
        Submission s = requireReviewable(submissionId);
        Admin admin = adminRepository.findByMember(adminMember)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다."));

        s.setStatus(SubmissionStatus.REJECTED);
        submissionRepository.save(s);
        saveHistory(s, admin, HistoryAction.REJECTED, "반려 사유: " + (reason == null ? "사유 미기재" : reason));

        return toSummary(s);
    }

    // --- helpers ---
    private Submission requireReviewable(Integer id) {
        Submission s = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출을 찾을 수 없습니다."));
        if (s.getStatus() != SubmissionStatus.SUBMITTED && s.getStatus() != SubmissionStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "검토 가능한 상태가 아닙니다.");
        }
        if (s.getStatus() == SubmissionStatus.SUBMITTED) {
            s.setStatus(SubmissionStatus.UNDER_REVIEW); // 컨벤션상 검토진입 표시
        }
        return s;
    }

    private void saveHistory(Submission s, Admin admin, HistoryAction action, String memo) {
        submissionHistoryRepository.save(
                SubmissionHistory.builder()
                        .submission(s)
                        .admin(admin)          // ★ admin_id NOT NULL 보장
                        .action(action)
                        .memo(memo)
                        .build()
        );
    }

    private SubmissionSummaryDTO toSummary(Submission s) {
        String fileUrl = submissionFileRepository.findBySubmission(s)
                .map(SubmissionFile::getFileUrl).orElse(null);
        return SubmissionSummaryDTO.builder()
                .submissionId(s.getSubmissionId())
                .status(s.getStatus())
                .fileUrl(fileUrl)
                .submittedAt(s.getSubmittedAt() == null ? null : s.getSubmittedAt().toString())
                .build();
    }
}
