package com.campusfind.controller;

import com.campusfind.dto.AdminDashboardDto;
import com.campusfind.dto.ApiResponse;
import com.campusfind.dto.LostReportResponse;
import com.campusfind.dto.UserDto;
import com.campusfind.entity.ReportStatus;
import com.campusfind.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {
        AdminDashboardDto metrics = adminService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<LostReportResponse>>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String query) {
        List<LostReportResponse> reports = adminService.getAllReportsWithFilter(status, categoryId, location, query);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }
}
