package com.cnu.docserver.deadline.controller;


import com.cnu.docserver.deadline.dto.DeadlineStatusDTO;
import com.cnu.docserver.deadline.service.DeadlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/deadline")
@Tag(name="DeadLine", description = "마감일 설정 API")
public class DeadlineController {

    private final DeadlineService deadlineService;


    @GetMapping
    @Operation(summary = "부서별 마감일 조회", description = "부서 ID에 해당하는 문서별 마감일을 반환합니다.")
    public List<DeadlineStatusDTO> getDeadlineByDepartment(@RequestParam Integer departmentId) {
        return deadlineService.getDeadlineByDepartment(departmentId);
    }


}
