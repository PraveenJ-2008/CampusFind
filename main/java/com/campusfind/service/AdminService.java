package com.campusfind.service;

import com.campusfind.dto.AdminDashboardDto;
import com.campusfind.dto.FoundItemResponse;
import com.campusfind.dto.LostReportResponse;
import com.campusfind.dto.UserDto;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.ReportStatus;
import com.campusfind.repository.FoundItemRepository;
import com.campusfind.repository.LostReportRepository;
import com.campusfind.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final LostReportRepository lostReportRepository;
    private final FoundItemRepository foundItemRepository;
    private final UserRepository userRepository;

    public AdminService(LostReportRepository lostReportRepository,
                        FoundItemRepository foundItemRepository,
                        UserRepository userRepository) {
        this.lostReportRepository = lostReportRepository;
        this.foundItemRepository = foundItemRepository;
        this.userRepository = userRepository;
    }

    public AdminDashboardDto getDashboardMetrics() {
        AdminDashboardDto dto = new AdminDashboardDto();

        dto.setTotalLostReports(lostReportRepository.count());
        dto.setTotalFoundItems(foundItemRepository.count());
        dto.setPendingLostReports(lostReportRepository.countByStatus(ReportStatus.OPEN));
        dto.setMatchedReports(lostReportRepository.countByStatus(ReportStatus.MATCHED));
        dto.setClosedReports(lostReportRepository.countByStatus(ReportStatus.CLOSED));
        dto.setAvailableFoundItems(foundItemRepository.countByStatus(ItemStatus.FOUND) + foundItemRepository.countByStatus(ItemStatus.AVAILABLE));
        dto.setClaimedFoundItems(foundItemRepository.countByStatus(ItemStatus.CLAIMED));
        dto.setReturnedFoundItems(foundItemRepository.countByStatus(ItemStatus.RETURNED));
        dto.setTotalUsers(userRepository.count());

        List<LostReportResponse> recentLost = lostReportRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(LostReportResponse::fromEntity)
                .collect(Collectors.toList());
        dto.setRecentLostReports(recentLost);

        List<FoundItemResponse> recentFound = foundItemRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(FoundItemResponse::fromEntity)
                .collect(Collectors.toList());
        dto.setRecentFoundItems(recentFound);

        return dto;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LostReportResponse> getAllReportsWithFilter(ReportStatus status, Long categoryId, String location, String query) {
        return lostReportRepository.searchReports(status, categoryId, location, query)
                .stream()
                .map(LostReportResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
