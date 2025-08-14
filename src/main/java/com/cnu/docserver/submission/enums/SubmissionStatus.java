package com.cnu.docserver.submission.enums;

public enum SubmissionStatus {
    DRAFT,        // 학생 임시저장/수정 중
    SUBMITTED,    // 학생이 최종 제출 완료(접수)
    UNDER_REVIEW, // (서버/봇 시작 시) 검토 중
    NEEDS_FIX,    // 보정 필요(봇/관리자 피드백)
    APPROVED,     // 관리자 승인
    REJECTED      // 관리자 반려
}
