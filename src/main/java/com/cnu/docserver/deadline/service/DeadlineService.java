package com.cnu.docserver.deadline.service;

import com.cnu.docserver.deadline.dto.DeadlineStatusDTO;
import com.cnu.docserver.deadline.entity.Deadline;
import com.cnu.docserver.deadline.repository.DeadlineRepository;
import com.cnu.docserver.docmanger.entity.Department;
import com.cnu.docserver.docmanger.entity.DocType;
import com.cnu.docserver.docmanger.repository.DepartmentRepository;
import com.cnu.docserver.docmanger.repository.DocTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final DepartmentRepository departmentRepository;
    private final DocTypeRepository docTypeRepository;
    private final DeadlineRepository deadlineRepository;



    @Transactional
    public List<DeadlineStatusDTO> getDeadlineByDepartment(Integer departmentId){
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(()->new RuntimeException("부서를 찾을 수 없습니다."));

        List<DocType> docTypes = docTypeRepository.findByDepartment(department);

        return docTypes.stream()
                .map(docType-> {
                    Optional<Deadline> deadlineOptional = deadlineRepository.findByDocType(docType);
                    return DeadlineStatusDTO.builder()
                            .docTypeId(docType.getDocTypeId())
                            .title(docType.getTitle())
                            .deadline(deadlineOptional.map(Deadline::getDeadline).orElse(null))
                            .build();
                })
                .toList();
    }


}
