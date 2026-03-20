package com.github.accessreport.controller;

import com.github.accessreport.dto.AccessReportResponse;
import com.github.accessreport.service.AccessReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccessReportController {

    private final AccessReportService accessReportService;

    @GetMapping("/report")
    public ResponseEntity<AccessReportResponse> getAccessReport(@RequestParam String org) {

        if (org == null || org.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Received report request for org: {}", org);
        AccessReportResponse report = accessReportService.generateReport(org.trim());
        return ResponseEntity.ok(report);
    }
}
