// com/cnu/docserver/ocr/SubmissionReviewOrchestrator.java
package com.cnu.docserver.ocr;

import com.cnu.docserver.docmanger.service.FileStorageService;
import com.cnu.docserver.ocr.dto.Finding;
import com.cnu.docserver.ocr.repository.OCRReviewResultRepository;
import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.entity.SubmissionFile;
import com.cnu.docserver.submission.entity.SubmissionHistory;
import com.cnu.docserver.submission.enums.HistoryAction;
import com.cnu.docserver.submission.enums.SubmissionStatus;
import com.cnu.docserver.submission.repository.SubmissionFileRepository;
import com.cnu.docserver.submission.repository.SubmissionHistoryRepository;
import com.cnu.docserver.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionReviewOrchestrator {

    private final SubmissionRepository submissionRepo;
    private final SubmissionHistoryRepository historyRepo;
    private final SubmissionFileRepository fileRepo;
    private final OcrClient ocrClient;
    private final FileStorageService fileStorageService;
    private final OCRReviewResultRepository ocrReviewResultRepo;
    @Async
    public void runBotReview(Integer submissionId) {
        try {
            doRunBotReview(submissionId); // 성공 경로
        } catch (Throwable t) {
            // 어떤 예외든 여기서 'NEEDS_FIX'로 고정 저장 (롤백 방지용)
            saveAsNeedsFix(submissionId, "자동 검토 실패: 시스템 오류 - " + firstLine(t.getMessage()));
        }
    }

    @Transactional
    protected void doRunBotReview(Integer submissionId) {
        Submission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String fileUrl = fileRepo.findTopBySubmissionOrderBySubmissionFileIdDesc(s)
                .map(SubmissionFile::getFileUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 없음"));

        byte[] fileBytes = fileStorageService.readBytes(fileUrl);
        OcrClient.OcrResult res = ocrClient.review(fileBytes, "submission.pdf");

        String verdict = Optional.ofNullable(res.getVerdict()).orElse("").toUpperCase();

        switch (verdict) {
            case "PASS" -> {
                s.setStatus(SubmissionStatus.SUBMITTED);
                submissionRepo.saveAndFlush(s);
                historyRepo.save(SubmissionHistory.builder()
                        .submission(s)
                        .action(HistoryAction.MODIFIED)
                        .memo("자동 검토 통과, 관리자 검토 대기")
                        .build());
            }
            case "NEEDS_FIX" -> {
                s.setStatus(SubmissionStatus.NEEDS_FIX);
                submissionRepo.saveAndFlush(s);
                String reasonMsg = (res.getFindings() == null || res.getFindings().isEmpty())
                        ? (res.getReason() == null ? "사유 미기재" : res.getReason())
                        : res.getFindings().stream()
                        .map(f -> f.getLabel() + ": " + f.getMessage())
                        .limit(10).collect(Collectors.joining("; "));
                historyRepo.save(SubmissionHistory.builder()
                        .submission(s)
                        .action(HistoryAction.MODIFIED)
                        .memo("자동 검토 실패: " + reasonMsg)
                        .build());
            }
            case "REJECT" -> {
                s.setStatus(SubmissionStatus.REJECTED);
                submissionRepo.saveAndFlush(s);
                historyRepo.save(SubmissionHistory.builder()
                        .submission(s)
                        .action(HistoryAction.REJECTED)
                        .memo("자동 검토 실패: " + (res.getReason() == null ? "사유 미기재" : res.getReason()))
                        .build());
            }
            default -> {
                s.setStatus(SubmissionStatus.NEEDS_FIX);
                submissionRepo.saveAndFlush(s);
                historyRepo.save(SubmissionHistory.builder()
                        .submission(s)
                        .action(HistoryAction.MODIFIED)
                        .memo("자동 검토 실패: OCR 응답 이상")
                        .build());
            }
        }

        //“스위치 처리 직후 공통 저장” — findings 포함
        OCRReviewResult entity = OCRReviewResult.builder()
                .submission(s)
                .verdict(verdict)               // PASS / NEEDS_FIX / REJECT
                .reason(res.getReason())
                .debugText(res.getDebugText())  // FastAPI가 주는 debug_text 매핑
                .build();

        // findings 는 편의 세터 통해 JSON 직렬화
        entity.setFindings(res.getFindings());

        ocrReviewResultRepo.save(entity);



    }

    /**
     * 실패 시에는 반드시 상태를 저장하기 위해 별도 트랜잭션 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveAsNeedsFix(Integer submissionId, String memo) {
        Submission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        s.setStatus(SubmissionStatus.NEEDS_FIX);
        submissionRepo.saveAndFlush(s);

        historyRepo.save(SubmissionHistory.builder()
                .submission(s)
                .action(HistoryAction.MODIFIED)
                .memo(memo)
                .build());
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        int p = s.indexOf('\n');
        return p >= 0 ? s.substring(0, p) : s;
    }
}