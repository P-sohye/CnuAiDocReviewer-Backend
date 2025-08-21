// com/cnu/docserver/ocr/OcrClient.java
package com.cnu.docserver.ocr;

import com.cnu.docserver.ocr.dto.Finding;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OcrClient {

    private final RestTemplate ocrRestTemplate;

    public OcrClient(@Qualifier("ocrRestTemplate") RestTemplate ocrRestTemplate) {
        this.ocrRestTemplate = ocrRestTemplate;
    }

    @Value("${ocr.base-url:http://localhost:8000}")
    private String baseUrl;

    public OcrResult review(byte[] fileBytes, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            ByteArrayResource filePart = new ByteArrayResource(fileBytes) {
                @Override public String getFilename() { return filename; }
            };

            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new HttpEntity<>(filePart, partHeaders));

            var req = new HttpEntity<>(body, headers);
            ResponseEntity<OcrResult> res =
                    ocrRestTemplate.postForEntity(baseUrl + "/ocr/review", req, OcrResult.class);
            OcrResult out = res.getBody();
            if (out == null) throw new IllegalStateException("Empty OCR response");
            return out;
        } catch (Exception e) {
            OcrResult fb = new OcrResult();
            fb.setVerdict("NEEDS_FIX");
            fb.setReason("OCR 호출 오류: " + e.getMessage());
            fb.setFindings(List.of());
            fb.setDebugText("OCR 호출 오류로 보정요청 전환");
            return fb;
        }
    }

    @Data
    public static class OcrResult {
        private String verdict;              // PASS | NEEDS_FIX | REJECT
        private List<Finding> findings;      // [{label,message}]
        private String reason;               // REJECT 사유

        private Map<String,Object> details;  // 상세 딕셔너리
        @JsonProperty("section_counts") private List<Map<String,Object>> sectionCounts;
        @JsonProperty("processing_time") private String processingTime;
        @JsonProperty("debug_text") private String debugText;             // 사람이 읽기 쉬운 로그 문자열
    }
}
