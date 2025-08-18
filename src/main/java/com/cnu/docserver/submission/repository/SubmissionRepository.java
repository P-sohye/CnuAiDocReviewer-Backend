package com.cnu.docserver.submission.repository;

import com.cnu.docserver.submission.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {


}
