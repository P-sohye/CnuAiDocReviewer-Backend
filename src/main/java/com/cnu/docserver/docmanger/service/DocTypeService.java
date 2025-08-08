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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocTypeService {

    private final DocTypeRepository docTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final RequiredFieldRepository requiredFieldRepository;
    private final OriginalFileRepository originalFileRepository;
    private final FileStorageService fileStorageService;

    //서류 등록
    @Transactional
    public DocType registerDocType(Integer departmentId, String title,
                                   List<String> requiredFields, List<String> exampleValues,
                                   MultipartFile file) {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다."));

        // 1. 서류 유형 저장
        DocType docType = DocType.builder()
                .department(department)
                .title(title)
                .build();
        docTypeRepository.save(docType);

        // 2. 필수 항목 저장
        saveRequiredFields(docType, requiredFields, exampleValues);

        // 3. 단일 파일 저장
        if (file != null && !file.isEmpty()) {
            saveSingleFile(docType, file);
        }

        return docType;
    }

    //서류 수정
    @Transactional
    public void updateDocType(Integer docTypeId, String title,
                              List<String> requiredFields, List<String> exampleValues,
                              MultipartFile file) {

        DocType docType = docTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        // 1. 제목 수정
        docType.setTitle(title);

        // 2. 단일 파일 교체
        if (file != null && !file.isEmpty()) {
            deleteExistingFile(docType);
            saveSingleFile(docType, file);
        }

        // 3. 필수 항목 업데이트
        syncRequiredFields(docType, requiredFields, exampleValues);
    }


    //부서별 전체 문서 조회
    @Transactional
    public List<DocTypeResponseDTO> getDocTypesByDepartment(Integer departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다."));

        return docTypeRepository.findByDepartment(department).stream()
                .map(docType -> new DocTypeResponseDTO(
                        docType.getDocTypeId(),
                        docType.getTitle(),
                        requiredFieldRepository.findByDocType(docType).stream()
                                .map(RequiredField::getFieldName)
                                .toList()
                ))
                .toList();
    }


    //수정용 단건 조회
    @Transactional
    public DocTypeEditResponseDTO getDocTypeForEdit(Integer docTypeId) {
        DocType docType = docTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        List<RequiredField> requiredFields = requiredFieldRepository.findByDocType(docType);

        // 단일 파일 기준이므로 get(0)만 조회
        List<OriginalFile> fileList = originalFileRepository.findByDocType(docType);
        String fileUrl = fileList.isEmpty() ? null : fileList.get(0).getFileUrl();

        return DocTypeEditResponseDTO.builder()
                .title(docType.getTitle())
                .fileUrl(fileUrl)
                .requiredFields(requiredFields.stream().map(RequiredField::getFieldName).toList())
                .exampleValues(requiredFields.stream().map(RequiredField::getExampleValue).toList())
                .build();
    }


    //---내부 메서드 ---
    //파일 저장
    private void saveRequiredFields(DocType docType, List<String> requiredFields, List<String> exampleValues) {
        List<RequiredField> fields = new ArrayList<>();
        for (int i = 0; i < requiredFields.size(); i++) {
            fields.add(RequiredField.builder()
                    .docType(docType)
                    .fieldName(requiredFields.get(i))
                    .exampleValue(exampleValues.get(i))
                    .build());
        }
        requiredFieldRepository.saveAll(fields);
    }

    private void deleteExistingFile(DocType docType) {
        List<OriginalFile> existing = originalFileRepository.findByDocType(docType);
        originalFileRepository.deleteAll(existing);
    }

    private void saveSingleFile(DocType docType, MultipartFile file) {
        String fileUrl = fileStorageService.save(file);
        OriginalFile originalFile = OriginalFile.builder()
                .docType(docType)
                .fileUrl(fileUrl)
                .build();
        originalFileRepository.save(originalFile);
    }

    private void syncRequiredFields(DocType docType, List<String> newFields, List<String> newExamples) {
        List<RequiredField> currentFields = requiredFieldRepository.findByDocType(docType);

        Set<String> newFieldNames = new HashSet<>(newFields);
        Map<String, String> newFieldMap = new HashMap<>();
        for (int i = 0; i < newFields.size(); i++) {
            newFieldMap.put(newFields.get(i), newExamples.get(i));
        }

        // 서류 내용
        // 삭제
        List<RequiredField> toDelete = currentFields.stream()
                .filter(f -> !newFieldNames.contains(f.getFieldName()))
                .toList();
        requiredFieldRepository.deleteAll(toDelete);


        // 추가
        Set<String> existingNames = currentFields.stream()
                .map(RequiredField::getFieldName)
                .collect(Collectors.toSet());

        List<RequiredField> toAdd = new ArrayList<>();
        for (int i = 0; i < newFields.size(); i++) {
            String name = newFields.get(i);
            if (!existingNames.contains(name)) {
                toAdd.add(RequiredField.builder()
                        .docType(docType)
                        .fieldName(name)
                        .exampleValue(newExamples.get(i))
                        .build());
            }
        }
        requiredFieldRepository.saveAll(toAdd);
    }
}



