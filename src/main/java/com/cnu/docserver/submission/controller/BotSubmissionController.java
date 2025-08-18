package com.cnu.docserver.submission.controller;

import com.cnu.docserver.submission.dto.SubmissionSummaryDTO;
import com.cnu.docserver.submission.service.BotSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bot/submissions")
@RequiredArgsConstructor
public class BotSubmissionController {

    private final BotSubmissionService botSubmissionService;

    @PostMapping("/{id}/pass")
    public SubmissionSummaryDTO pass(@PathVariable Integer id) {
        return botSubmissionService.pass(id); // BOT_REVIEW -> SUBMITTED
    }

    @PostMapping("/{id}/needs-fix")
    public SubmissionSummaryDTO needsFix(@PathVariable Integer id, @RequestBody(required=false) Map<String,String> b) {
        String reason = b!=null? b.getOrDefault("reason","보정 요청"): "보정 요청";
        return botSubmissionService.needsFix(id, reason); // BOT_REVIEW -> NEEDS_FIX
    }

    @PostMapping("/{id}/reject")
    public SubmissionSummaryDTO reject(@PathVariable Integer id, @RequestBody(required=false) Map<String,String> b) {
        String reason = b!=null? b.getOrDefault("reason","반려(봇)"): "반려(봇)";
        return botSubmissionService.reject(id, reason); // BOT_REVIEW -> REJECTED
    }
}