package com.cnu.docserver.submission.service;

import com.cnu.docserver.deadline.repository.DeadlineRepository;
import com.cnu.docserver.docmanger.entity.DocType;
import com.cnu.docserver.docmanger.entity.RequiredField;
import com.cnu.docserver.docmanger.repository.DocTypeRepository;
import com.cnu.docserver.docmanger.repository.RequiredFieldRepository;
import com.cnu.docserver.docmanger.service.FileStorageService;
import com.cnu.docserver.submission.dto.FieldValueInputDTO;
import com.cnu.docserver.submission.dto.SubmissionSummaryDTO;
import com.cnu.docserver.submission.dto.SubmitRequestDTO;
import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.entity.SubmissionFieldValue;
import com.cnu.docserver.submission.entity.SubmissionFile;
import com.cnu.docserver.submission.entity.SubmissionHistory;
import com.cnu.docserver.submission.enums.HistoryAction;
import com.cnu.docserver.submission.enums.SubmissionStatus;
import com.cnu.docserver.submission.repository.SubmissionFieldValueRepository;
import com.cnu.docserver.submission.repository.SubmissionFileRepository;
import com.cnu.docserver.submission.repository.SubmissionHistoryRepository;
import com.cnu.docserver.submission.repository.SubmissionRepository;
import com.cnu.docserver.user.entity.Admin;
import com.cnu.docserver.user.entity.Member;
import com.cnu.docserver.user.entity.Student;
import com.cnu.docserver.user.repository.StudentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final StudentRepository studentRepository;
    private final DocTypeRepository docTypeRepository;
    private final SubmissionRepository submissionRepository;


    private final DeadlineRepository deadlineRepository;

    private final FileStorageService fileStorageService;
    private final RequiredFieldRepository requiredFieldRepository;

    private final SubmissionFileRepository submissionFileRepository;
    private final SubmissionFieldValueRepository submissionFieldValueRepository;
    private final SubmissionHistoryRepository submissionHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // === 1) 최초 제출 ===
    @Transactional
    public SubmissionSummaryDTO create(Integer docTypeId, String fieldsJson, MultipartFile file, HttpSession session) {
        // 1) 로그인 학생 조회
        String studentId = currentStudentId(session);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 정보를 찾을 수 없습니다."));

        // 2) 문서 유형
        DocType docType = docTypeRepository.findById(docTypeId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "문서 유형을 찾을 수 없습니다."));

        // 3)  마감일 1차 체크
        ensureNotPastDeadline(docType);

        // 4) 파일 필수
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일은 필수입니다.");
        }

        // 5) 제출 생성(DRAFT)
        Submission submission = Submission.builder()
                .student(student)
                .docType(docType)
                .status(SubmissionStatus.DRAFT)
                .build();
        submissionRepository.save(submission);

        // 6) 파일/필드 upsert
        upsertFile(submission,file);
        upsertFieldValues(submission, parseFields(fieldsJson),docType);

        // 7) 제출 전이 (학생 제출) → SUBMITTED
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        // 8) 이력 기록: CREATE (학생 제출)
        writeHistory(submission, null, HistoryAction.CREATE, "학생 제출");

        // 9) 서버가 곧바로 UNDER_REVIEW로 전환
        submission.setStatus(SubmissionStatus.UNDER_REVIEW);
        submissionRepository.save(submission);

        return toSummary(submission);
    }

    // === 2) 반려 후 수정(덮어쓰기) ===
    @Transactional
    public SubmissionSummaryDTO update(Integer submissionId, String fieldsJson, MultipartFile file) {
        Submission s = requireSubmission(submissionId);

        mustBeOneOf(s, SubmissionStatus.DRAFT, SubmissionStatus.REJECTED);

        if (file != null && !file.isEmpty()) {
            upsertFile(s, file);
        }
        if (fieldsJson != null && !fieldsJson.isBlank()) {
            upsertFieldValues(s, parseFields(fieldsJson), s.getDocType());
        }
        // 상태는 여기서 바꾸지 않음(임시 저장 성격)
        return toSummary(s);
    }


    // === 3) 최종 제출 ===
    @Transactional
    public SubmissionSummaryDTO submit(Integer submissionId, SubmitRequestDTO body) {
        Submission s = requireSubmission(submissionId);

        mustBeOneOf(s, SubmissionStatus.DRAFT, SubmissionStatus.REJECTED);

        if (body == null || body.getMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제출 모드가 필요합니다.");
        }

        // 마감 체크
        ensureNotPastDeadline(s.getDocType());

        // 제출 전이
        s.setStatus(SubmissionStatus.SUBMITTED);
        s.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(s);

        String memo = (body.getMode() == SubmitRequestDTO.SubmitMode.FINAL)
                ? "학생 최종제출(FINAL)"
                : "학생 바로제출(DIRECT, AI 검증 건너뜀)";
        writeHistory(s, null, HistoryAction.CREATE, "학생 재제출");

        // 즉시 관리자 검토 대기
        s.setStatus(SubmissionStatus.UNDER_REVIEW);
        submissionRepository.save(s);

        // TODO: 내부 큐/이벤트로 봇 검토 트리거
        return toSummary(s);
    }

    // ───────────────────────── 내부 유틸 ─────────────────────────

    // 로그인 학생 검증
    private String currentStudentId(HttpSession session) {
        Object currentUser = session.getAttribute("currentUser");
        if(!(currentUser instanceof Member member)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return studentRepository.findByMember(member)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 정보를 찾을 수 없습니다."))
                .getStudentId();
    }

    //  마감일 검증: deadline이 없거나(null) 오늘이 마감일보다 늦지 않으면(<=) 통과
    private void ensureNotPastDeadline(DocType docType) {
        deadlineRepository.findByDocType(docType).ifPresent(deadline -> {
            LocalDate d = deadline.getDeadline();
            LocalDate today = LocalDate.now(KST);
            if (d != null && today.isAfter(d)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "마감일이 지났습니다.");
            }
        });
    }

    // 이력 기록
    private void writeHistory(Submission s, Admin admin, HistoryAction action, String memo) {
        submissionHistoryRepository.save(
                SubmissionHistory.builder()
                        .submission(s)
                        .admin(admin) // 학생/시스템 액션이면 null
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

    private Submission requireSubmission(Integer id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출을 찾을 수 없습니다."));
    }

    private void mustBeOneOf(Submission s, SubmissionStatus... allowed) {
        for (SubmissionStatus a : allowed) if (s.getStatus() == a) return;
        throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 상태에서 허용되지 않는 작업입니다.");
    }

    private void upsertFile(Submission submission, MultipartFile file) {

        String newUrl = fileStorageService.saveSubmission(submission.getSubmissionId(),file);

        submissionFileRepository.findBySubmission(submission).ifPresentOrElse(existing->{
            safeDelete(existing.getFileUrl());
            existing.setFileUrl(newUrl);
            existing.setUploadedAt(LocalDateTime.now());
            submissionFileRepository.save(existing);
        },()->{
            submissionFileRepository.save(
                    SubmissionFile.builder()
                            .submission(submission)
                            .fileUrl(newUrl)
                            .uploadedAt(LocalDateTime.now())
                            .build()
            );
        });

    }
    private void safeDelete(String url){
        try{
            fileStorageService.deleteByUrl(url);
        }catch(Exception ignored){}
    }

    private void upsertFieldValues(Submission submission, List<FieldValueInputDTO> inputs, DocType docType ){

        submissionFieldValueRepository.deleteBySubmission(submission);
        if (inputs ==null || inputs.isEmpty()) return;

        List<RequiredField> defined = requiredFieldRepository.findByDocType(docType);
        Map<Integer, RequiredField> byId = new HashMap<>();
        Map<String, RequiredField> byName = new HashMap<>();
        for (RequiredField rf : defined) {
            byId.put(rf.getRequiredFieldId(), rf);
            if (rf.getFieldName() != null) byName.put(rf.getFieldName(), rf);
        }

        List<SubmissionFieldValue> rows = new ArrayList<>();
        for (FieldValueInputDTO in : inputs) {
            RequiredField rf = null;
            if (in.getRequiredFieldId() != null) {
                rf = byId.get(in.getRequiredFieldId());
            } else if (in.getLabel() != null){
                rf = byName.get(in.getLabel());
            }

            rows.add(SubmissionFieldValue.builder()
                    .submission(submission)
                    .requiredField(rf)
                    .fieldName(in.getLabel())
                    .fieldValue(in.getValue())
                    .build());
        }
        submissionFieldValueRepository.saveAll(rows);
    }

    private List<FieldValueInputDTO> parseFields(String fieldsJson) {
        try {
            if (fieldsJson == null || fieldsJson.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(fieldsJson, new TypeReference<List<FieldValueInputDTO>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fieldsJson 파싱 실패", e);
        }
    }



}


