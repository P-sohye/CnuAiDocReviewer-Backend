package com.cnu.docserver.submission.repository;

import com.cnu.docserver.submission.entity.SubmissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Integer> {
}
