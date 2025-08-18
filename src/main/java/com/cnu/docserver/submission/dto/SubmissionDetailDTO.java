package com.cnu.docserver.submission.dto;

import java.util.List;

public record SubmissionDetailDTO(
        Integer submissionId,
        String status,
        String submittedAt,
        String studentId,
        String studentName,
        String fileUrl,
        String docTypeName,
        String fileName,
        List<HistoryDTO> history
) {}
