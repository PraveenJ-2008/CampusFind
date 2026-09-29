package com.campusfind.service;

import com.campusfind.dto.FoundItemRequest;
import com.campusfind.dto.FoundItemResponse;
import com.campusfind.entity.Category;
import com.campusfind.entity.FoundItem;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.exception.BadRequestException;
import com.campusfind.exception.ForbiddenException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.FoundItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoundItemService {

    private final FoundItemRepository foundItemRepository;
    private final CategoryService categoryService;
    private final AuthService authService;

    public FoundItemService(FoundItemRepository foundItemRepository,
                            CategoryService categoryService,
                            AuthService authService) {
        this.foundItemRepository = foundItemRepository;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @Transactional
    public FoundItemResponse createFoundItem(FoundItemRequest request, String reporterEmail) {
        User reporter = authService.getUserEntityByEmail(reporterEmail);

        if (reporter.getRole() != Role.STAFF && reporter.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only staff members or administrators can register found items.");
        }

        Category category = categoryService.findEntityById(request.getCategoryId());

        if (!category.getActive()) {
            throw new BadRequestException("Selected category is currently inactive.");
        }

        if (request.getFoundDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Found date cannot be in the future.");
        }

        FoundItem item = new FoundItem(
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getLocation().trim(),
                request.getFoundDate(),
                reporter,
                category
        );

        FoundItem saved = foundItemRepository.save(item);
        return FoundItemResponse.fromEntity(saved);
    }

    public List<FoundItemResponse> getAllFoundItems(ItemStatus status, Long categoryId, String location, String query) {
        String cleanLocation = (location != null && !location.trim().isEmpty()) ? location.trim() : null;
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        return foundItemRepository.searchItems(status, categoryId, cleanLocation, cleanQuery)
                .stream()
                .map(FoundItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FoundItemResponse> getMyFoundItems(String userEmail) {
        User user = authService.getUserEntityByEmail(userEmail);
        return foundItemRepository.findByReporterIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(FoundItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public FoundItemResponse getFoundItemById(Long id) {
        FoundItem item = findEntityById(id);
        return FoundItemResponse.fromEntity(item);
    }

    public FoundItem findEntityById(Long id) {
        return foundItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Found item not found with ID: " + id));
    }

    @Transactional
    public FoundItemResponse updateFoundItemStatus(Long id, ItemStatus newStatus, String userEmail) {
        FoundItem item = findEntityById(id);
        User currentUser = authService.getUserEntityByEmail(userEmail);

        // Authorization check: Only reporting staff member or admin
        boolean isOwner = item.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the reporting staff member or an administrator can update this found item's status.");
        }

        ItemStatus currentStatus = item.getStatus();

        if (currentStatus == newStatus) {
            return FoundItemResponse.fromEntity(item);
        }

        // Enforce state machine transitions
        if ((currentStatus == ItemStatus.FOUND || currentStatus == ItemStatus.AVAILABLE) && newStatus == ItemStatus.RETURNED) {
            throw new BadRequestException("Cannot transition found item directly from " + currentStatus + " to RETURNED. It must first be CLAIMED.");
        }

        if (currentStatus == ItemStatus.RETURNED && !isAdmin) {
            throw new BadRequestException("Item has already been marked as RETURNED. Only an administrator can re-open it.");
        }

        item.setStatus(newStatus);
        FoundItem updated = foundItemRepository.save(item);
        return FoundItemResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteFoundItem(Long id, String userEmail) {
        FoundItem item = findEntityById(id);
        User currentUser = authService.getUserEntityByEmail(userEmail);
        boolean isOwner = item.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the reporting staff member or an administrator can delete this found item.");
        }
        foundItemRepository.delete(item);
    }
}
