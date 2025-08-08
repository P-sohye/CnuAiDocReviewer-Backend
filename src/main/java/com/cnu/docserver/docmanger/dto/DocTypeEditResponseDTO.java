package com.cnu.docserver.docmanger.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocTypeEditResponseDTO {
    private String title;
    private String fileUrl;
    private List<String> requiredFields;
    private List<String> exampleValues;
}
