package com.campusfind.service;

import com.campusfind.dto.LostReportRequest;
import com.campusfind.dto.LostReportResponse;
import com.campusfind.entity.Category;
import com.campusfind.entity.LostReport;
import com.campusfind.entity.ReportStatus;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.exception.BadRequestException;
import com.campusfind.exception.ForbiddenException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.LostReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LostReportService {

    private final LostReportRepository lostReportRepository;
    private final CategoryService categoryService;
    private final AuthService authService;

    public LostReportService(LostReportRepository lostReportRepository,
                             CategoryService categoryService,
                             AuthService authService) {
        this.lostReportRepository = lostReportRepository;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @Transactional
    public LostReportResponse createReport(LostReportRequest request, String reporterEmail) {
        User reporter = authService.getUserEntityByEmail(reporterEmail);
        Category category = categoryService.findEntityById(request.getCategoryId());

        if (!category.getActive()) {
            throw new BadRequestException("Selected category is currently inactive.");
        }

        if (request.getLostDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Lost date cannot be in the future.");
        }

        LostReport report = new LostReport(
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getLocation().trim(),
                request.getLostDate(),
                reporter,
                category
        );

        LostReport saved = lostReportRepository.save(report);
        return LostReportResponse.fromEntity(saved);
    }

    public List<LostReportResponse> getAllOpenReports() {
        return lostReportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.OPEN)
                .stream()
                .map(LostReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LostReportResponse> searchReports(ReportStatus status, Long categoryId, String location, String query) {
        String cleanLocation = (location != null && !location.trim().isEmpty()) ? location.trim() : null;
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        return lostReportRepository.searchReports(status, categoryId, cleanLocation, cleanQuery)
                .stream()
                .map(LostReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public LostReportResponse getReportById(Long id) {
        LostReport report = findEntityById(id);
        return LostReportResponse.fromEntity(report);
    }

    public LostReport findEntityById(Long id) {
        return lostReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lost report not found with ID: " + id));
    }

    public List<LostReportResponse> getMyReports(String userEmail) {
        User user = authService.getUserEntityByEmail(userEmail);
        return lostReportRepository.findByReporterIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(LostReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LostReportResponse updateReport(Long id, LostReportRequest request, String userEmail) {
        LostReport report = findEntityById(id);
        User currentUser = authService.getUserEntityByEmail(userEmail);

        // Check ownership or admin
        verifyReportOwnershipOrAdmin(report, currentUser);

        if (report.getStatus() == ReportStatus.CLOSED) {
            throw new BadRequestException("Closed reports cannot be modified.");
        }

        if (request.getLostDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Lost date cannot be in the future.");
        }

        Category category = categoryService.findEntityById(request.getCategoryId());
        report.setTitle(request.getTitle().trim());
        report.setDescription(request.getDescription().trim());
        report.setLocation(request.getLocation().trim());
        report.setLostDate(request.getLostDate());
        report.setCategory(category);

        LostReport updated = lostReportRepository.save(report);
        return LostReportResponse.fromEntity(updated);
    }

    @Transactional
    public LostReportResponse updateReportStatus(Long id, ReportStatus newStatus, String userEmail) {
        LostReport report = findEntityById(id);
        User currentUser = authService.getUserEntityByEmail(userEmail);

        verifyReportOwnershipOrAdmin(report, currentUser);

        if (report.getStatus() == ReportStatus.CLOSED && newStatus != ReportStatus.CLOSED && currentUser.getRole() != Role.ADMIN) {
            throw new BadRequestException("Closed reports cannot be reopened by non-admin users.");
        }

        report.setStatus(newStatus);
        LostReport updated = lostReportRepository.save(report);
        return LostReportResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteReport(Long id, String userEmail) {
        LostReport report = findEntityById(id);
        User currentUser = authService.getUserEntityByEmail(userEmail);
        verifyReportOwnershipOrAdmin(report, currentUser);
        lostReportRepository.delete(report);
    }

    private void verifyReportOwnershipOrAdmin(LostReport report, User currentUser) {
        boolean isOwner = report.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to modify this lost report.");
        }
    }
}
