package com.cnu.docserver.docmanger.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    public String save(Integer docTypeId, MultipartFile file) {
        try {
            Path dir = uploadDir.resolve(String.valueOf(docTypeId));
            Files.createDirectories(dir);

            String original = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");
            original = original.replace("\\", "/");
            original = original.substring(original.lastIndexOf('/') + 1);
            if (original.isBlank()) original = "unknown";
            if (original.length() > 255) original = original.substring(original.length() - 255);

            Path target = dir.resolve(original).normalize();
            // dir 밖으로 벗어나지 못하게 가드
            if (!target.startsWith(dir)) throw new SecurityException("Invalid path");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // URL 인코딩(공백/한글 안전)
            String encoded = java.net.URLEncoder.encode(original, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return "/uploads/" + docTypeId + "/" + encoded;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }
    }

    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;
        try {
            String relative = java.net.URLDecoder.decode(
                    fileUrl.substring("/uploads/".length()),
                    java.nio.charset.StandardCharsets.UTF_8
            );
            Path target = uploadDir.resolve(relative).normalize();
            // uploads 폴더 밖이면 차단
            if (!target.startsWith(uploadDir)) return;
            Files.deleteIfExists(target);
        } catch (IOException ignored) {}
    }
}
