package com.cnu.docserver.deadline.repository;

import com.cnu.docserver.deadline.entity.Deadline;
import com.cnu.docserver.docmanger.entity.DocType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeadlineRepository extends JpaRepository<Deadline,Integer> {

    // 1. docType 기준으로 마감일 조회
    Optional<Deadline> findByDocType(DocType docType);

    // 2. 존재 여부 확인 (중복 등록 방지) -> 이미 있으면 update, 없으면 insert
    boolean existsByDocType(DocType docType);

    // 3. docType 기준 삭제
    void deleteByDocType(DocType docType);
}
