package com.cnu.docserver.submission.repository;

import com.cnu.docserver.department.entity.Department;
import com.cnu.docserver.submission.entity.Submission;
import com.cnu.docserver.submission.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    // 기존: 상태 목록으로 필터
    List<Submission> findByDocType_DepartmentAndStatusInOrderBySubmittedAtDesc(
            Department department,
            List<SubmissionStatus> statuses
    );

    // 신규: 상태 제한 없이 전부 (부서 기준 최신순)
    List<Submission> findByDocType_DepartmentOrderBySubmittedAtDesc(Department department);


    @Query("""
      select s
      from Submission s
      join fetch s.student st
      join fetch st.member m
      left join fetch s.docType dt
      where s.submissionId = :id
    """)
    Optional<Submission> findDetailById(Integer id);

}
