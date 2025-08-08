package com.cnu.docserver.docmanger.service;

import com.cnu.docserver.docmanger.dto.DocTypeEditResponseDTO;
import com.cnu.docserver.docmanger.dto.DocTypeResponseDTO;
import com.cnu.docserver.docmanger.entity.Department;
import com.cnu.docserver.docmanger.entity.DocType;
import com.cnu.docserver.docmanger.entity.OriginalFile;
import com.cnu.docserver.docmanger.entity.RequiredField;
import com.cnu.docserver.docmanger.repository.DepartmentRepository;
import com.cnu.docserver.docmanger.repository.DocTypeRepository;
import com.cnu.docserver.docmanger.repository.OriginalFileRepository;
import com.cnu.docserver.docmanger.repository.RequiredFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocTypeService {

    private final DocTypeRepository docTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final RequiredFieldRepository requiredFieldRepository;
    private final OriginalFileRepository originalFileRepository;
    private final FileStorageService fileStorageService;


    @Transactional
    public DocType registerDocType(Integer departmentId, String title, List<String> requiredFields, List<String> exampleValues, MultipartFile file) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(()-> new IllegalArgumentException("부서를 찾을 수 없습니다."));

        //1. 서류 유형 저장
        DocType docType = DocType.builder()
                .department(department)
                .title(title)
                .build();
        docTypeRepository.save(docType);

        //2. 필수항목 저장
        List<RequiredField> fieldEntities = new ArrayList<>();
        for (int i = 0; i < requiredFields.size(); i++) {
            RequiredField field = RequiredField.builder()
                    .docType(docType)
                    .fieldName(requiredFields.get(i))
                    .exampleValue(exampleValues.get(i))
                    .build();
            fieldEntities.add(field);
        }
        requiredFieldRepository.saveAll(fieldEntities);

        // 3. 파일 저장
        if (file != null && !file.isEmpty()) {
            String fileUrl = fileStorageService.save(file);
            OriginalFile originalFile = OriginalFile.builder()
                    .docType(docType)
                    .fileUrl(fileUrl)
                    .build();
            originalFileRepository.save(originalFile);
        }

        return docType;


    }

    @Transactional
    public List<DocTypeResponseDTO> getDocTypesByDepartment(Integer departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다."));

        List<DocType> docTypes = docTypeRepository.findByDepartment(department);

        return docTypes.stream()
                .map(docType -> new DocTypeResponseDTO(
                        docType.getDocTypeId(),
                        docType.getTitle(),
                        requiredFieldRepository.findByDocType(docType)
                                .stream()
                                .map(RequiredField::getFieldName)
                                .toList()
                )).toList();
    }

    @Transactional
    public DocTypeEditResponseDTO getDocTypeForEdit(Integer docTypeId) {
        DocType docType = docTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        List<RequiredField> requiredFields = requiredFieldRepository.findByDocType(docType);

        return DocTypeEditResponseDTO.builder()
                .title(docType.getTitle())
                .fileUrl(docType.getOriginalFile() != null ? docType.getOriginalFile().getFileUrl() : null)
                .requiredFields(requiredFields.stream()
                        .map(RequiredField::getFieldName)
                        .toList())
                .exampleValues(requiredFields.stream()
                        .map(RequiredField::getExampleValue)
                        .toList())
                .build();
    }


}


