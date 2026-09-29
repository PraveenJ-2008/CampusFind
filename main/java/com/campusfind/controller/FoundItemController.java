package com.campusfind.controller;

import com.campusfind.dto.ApiResponse;
import com.campusfind.dto.FoundItemRequest;
import com.campusfind.dto.FoundItemResponse;
import com.campusfind.dto.StatusUpdateRequest;
import com.campusfind.entity.ItemStatus;
import com.campusfind.exception.BadRequestException;
import com.campusfind.service.FoundItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/found")
public class FoundItemController {

    private final FoundItemService foundItemService;

    public FoundItemController(FoundItemService foundItemService) {
        this.foundItemService = foundItemService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<FoundItemResponse>> createFoundItem(
            @Valid @RequestBody FoundItemRequest request,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        FoundItemResponse response = foundItemService.createFoundItem(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Found item registered successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FoundItemResponse>>> getAllFoundItems(
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String query) {
        List<FoundItemResponse> items = foundItemService.getAllFoundItems(status, categoryId, location, query);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoundItemResponse>> getFoundItemById(@PathVariable Long id) {
        FoundItemResponse response = foundItemService.getFoundItemById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FoundItemResponse>>> getMyFoundItems(
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        List<FoundItemResponse> items = foundItemService.getMyFoundItems(userEmail);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FoundItemResponse>> updateFoundItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }

        ItemStatus status;
        try {
            status = ItemStatus.fromString(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus() + ". Allowed: FOUND, CLAIMED, RETURNED");
        }

        FoundItemResponse response = foundItemService.updateFoundItemStatus(id, status, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Found item status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFoundItem(
            @PathVariable Long id,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        foundItemService.deleteFoundItem(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Found item deleted successfully", null));
    }
}
