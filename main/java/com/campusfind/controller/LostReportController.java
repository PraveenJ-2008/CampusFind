package com.campusfind.controller;

import com.campusfind.dto.ApiResponse;
import com.campusfind.dto.LostReportRequest;
import com.campusfind.dto.LostReportResponse;
import com.campusfind.dto.StatusUpdateRequest;
import com.campusfind.entity.ReportStatus;
import com.campusfind.exception.BadRequestException;
import com.campusfind.service.LostReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lost")
public class LostReportController {

    private final LostReportService lostReportService;

    public LostReportController(LostReportService lostReportService) {
        this.lostReportService = lostReportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LostReportResponse>> createReport(
            @Valid @RequestBody LostReportRequest request,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        LostReportResponse response = lostReportService.createReport(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lost item reported successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LostReportResponse>>> searchReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String query) {
        List<LostReportResponse> results = lostReportService.searchReports(status, categoryId, location, query);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LostReportResponse>> getReportById(@PathVariable Long id) {
        LostReportResponse response = lostReportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<LostReportResponse>>> getMyReports(
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        List<LostReportResponse> reports = lostReportService.getMyReports(userEmail);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LostReportResponse>> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody LostReportRequest request,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        LostReportResponse response = lostReportService.updateReport(id, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Lost report updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LostReportResponse>> updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }

        ReportStatus status;
        try {
            status = ReportStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus() + ". Allowed: OPEN, MATCHED, CLOSED");
        }

        LostReportResponse response = lostReportService.updateReportStatus(id, status, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Lost report status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        lostReportService.deleteReport(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Lost report deleted successfully", null));
    }
}
