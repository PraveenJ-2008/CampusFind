package com.campusfind.controller;

import com.campusfind.dto.ApiResponse;
import com.campusfind.dto.MatchSuggestionDto;
import com.campusfind.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/{lostId}")
    public ResponseEntity<ApiResponse<List<MatchSuggestionDto>>> getMatchesForReport(
            @PathVariable Long lostId) {
        List<MatchSuggestionDto> matches = matchingService.findMatchesForLostReport(lostId);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @PostMapping("/{lostId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmMatch(
            @PathVariable Long lostId,
            @RequestParam Long foundId,
            @AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        matchingService.confirmMatch(lostId, foundId, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Match confirmed successfully. Item marked as CLAIMED and report marked as MATCHED.", null));
    }
}
