package com.cnu.docserver.submission.service;

import com.cnu.docserver.submission.dto.SubmissionSummaryDTO;
import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.entity.SubmissionHistory;
import com.cnu.docserver.submission.enums.HistoryAction;
import com.cnu.docserver.submission.enums.SubmissionStatus;
import com.cnu.docserver.submission.repository.SubmissionHistoryRepository;
import com.cnu.docserver.submission.repository.SubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// BotSubmissionService.java (예시)
@Service
@RequiredArgsConstructor
public class BotSubmissionService {
    private final SubmissionRepository submissionRepository;
    private final SubmissionHistoryRepository historyRepo;

    @Transactional
    public SubmissionSummaryDTO pass(Integer id) {
        Submission s = require(id, SubmissionStatus.BOT_REVIEW);
        s.setStatus(SubmissionStatus.SUBMITTED); // 관리자 대기
        submissionRepository.save(s);
        historyRepo.save(SubmissionHistory.builder()
                .submission(s).action(HistoryAction.MODIFIED).memo("봇 검수 통과").build());
        return toSummary(s);
    }

    @Transactional
    public SubmissionSummaryDTO needsFix(Integer id, String reason) {
        Submission s = require(id, SubmissionStatus.BOT_REVIEW);
        s.setStatus(SubmissionStatus.NEEDS_FIX);
        submissionRepository.save(s);
        historyRepo.save(SubmissionHistory.builder()
                .submission(s).action(HistoryAction.MODIFIED).memo("봇 보정요청: " + reason).build());
        return toSummary(s);
    }

    @Transactional
    public SubmissionSummaryDTO reject(Integer id, String reason) {
        Submission s = require(id, SubmissionStatus.BOT_REVIEW);
        s.setStatus(SubmissionStatus.REJECTED);
        submissionRepository.save(s);
        historyRepo.save(SubmissionHistory.builder()
                .submission(s).action(HistoryAction.REJECTED).memo("봇 반려: " + reason).build());
        return toSummary(s);
    }

    private Submission require(Integer id, SubmissionStatus expected) {
        Submission s = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (s.getStatus()!=expected) throw new ResponseStatusException(HttpStatus.CONFLICT);
        return s;
    }

    private SubmissionSummaryDTO toSummary(Submission s) {
        return SubmissionSummaryDTO.builder()
                .submissionId(s.getSubmissionId())
                .status(s.getStatus())
                .submittedAt(s.getSubmittedAt()==null? null : s.getSubmittedAt().toString())
                .build();
    }
}
