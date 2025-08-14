package com.cnu.docserver.submission.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
@Tag(name = "Student Submission", description = "학생 학생 제출/수정/최종제출 API")
public class SubmissionController {
}
