package com.cnu.docserver.submission.repository;

import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.entity.SubmissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Integer> {
    List<SubmissionHistory> findBySubmissionOrderByChangedAtAsc(Submission submission);
}
