package com.campusfind.service;

import com.campusfind.dto.MatchSuggestionDto;
import com.campusfind.entity.FoundItem;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.LostReport;
import com.campusfind.entity.ReportStatus;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.exception.BadRequestException;
import com.campusfind.exception.ForbiddenException;
import com.campusfind.repository.FoundItemRepository;
import com.campusfind.repository.LostReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private final LostReportRepository lostReportRepository;
    private final FoundItemRepository foundItemRepository;
    private final AuthService authService;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "because", "as", "what", "which", "this", "that", "these",
            "those", "then", "so", "than", "such", "both", "through", "about", "for", "is", "of", "while", "during",
            "to", "from", "in", "out", "on", "off", "over", "under", "again", "further", "here", "there", "when",
            "where", "why", "how", "all", "any", "each", "few", "more", "most", "other", "some", "no", "nor",
            "not", "only", "own", "same", "too", "very", "can", "will", "just", "should", "now", "i", "me", "my",
            "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "he", "him", "his", "she", "her",
            "hers", "it", "its", "they", "them", "their", "theirs", "was", "were", "be", "been", "being", "have",
            "has", "had", "having", "do", "does", "did", "doing", "lost", "found", "item", "please", "help",
            "contact", "someone", "near", "campus", "room", "hall", "block", "floor"
    );

    public MatchingService(LostReportRepository lostReportRepository,
                           FoundItemRepository foundItemRepository,
                           AuthService authService) {
        this.lostReportRepository = lostReportRepository;
        this.foundItemRepository = foundItemRepository;
        this.authService = authService;
    }

    public List<MatchSuggestionDto> findMatchesForLostReport(Long lostReportId) {
        LostReport lostReport = lostReportRepository.findById(lostReportId)
                .orElseThrow(() -> new BadRequestException("Lost report not found with ID: " + lostReportId));

        if (lostReport.getStatus() == ReportStatus.CLOSED) {
            throw new BadRequestException("Closed lost reports are excluded from suggestions.");
        }

        // Active un-claimed found items (FOUND or AVAILABLE)
        List<FoundItem> availableItems = foundItemRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(ItemStatus.FOUND, ItemStatus.AVAILABLE)
        );

        Set<String> lostKeywords = extractKeywords(lostReport.getTitle() + " " + lostReport.getDescription());
        Set<String> lostLocationTokens = extractKeywords(lostReport.getLocation());

        List<MatchSuggestionDto> matches = new ArrayList<>();

        for (FoundItem found : availableItems) {
            boolean categoryMatched = lostReport.getCategory().getId().equals(found.getCategory().getId());

            Set<String> foundKeywords = extractKeywords(found.getTitle() + " " + found.getDescription());
            Set<String> foundLocationTokens = extractKeywords(found.getLocation());

            // Keyword intersection
            Set<String> overlappingKeywords = new HashSet<>(lostKeywords);
            overlappingKeywords.retainAll(foundKeywords);

            // Location intersection
            Set<String> overlappingLocation = new HashSet<>(lostLocationTokens);
            overlappingLocation.retainAll(foundLocationTokens);

            // Match Score Calculation
            int score = 0;
            if (categoryMatched) {
                score += 40; // 40 points for same category
            }

            int keywordPoints = Math.min(overlappingKeywords.size() * 15, 45); // up to 45 points for keyword overlap
            score += keywordPoints;

            int locationPoints = Math.min(overlappingLocation.size() * 15, 15); // up to 15 points for location overlap
            score += locationPoints;

            score = Math.min(score, 100);

            // Add to suggestions if there is category match and keyword match, or score >= 40
            if (score >= 40 || (categoryMatched && !overlappingKeywords.isEmpty())) {
                List<String> combinedMatchedWords = new ArrayList<>(overlappingKeywords);
                for (String loc : overlappingLocation) {
                    if (!combinedMatchedWords.contains(loc)) {
                        combinedMatchedWords.add(loc + " (location)");
                    }
                }

                MatchSuggestionDto dto = new MatchSuggestionDto(
                        lostReport.getId(),
                        lostReport.getTitle(),
                        found.getId(),
                        found.getTitle(),
                        found.getDescription(),
                        found.getLocation(),
                        found.getFoundDate(),
                        score,
                        combinedMatchedWords,
                        found.getCategory().getName(),
                        categoryMatched
                );
                matches.add(dto);
            }
        }

        // Sort descending by score
        matches.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return matches;
    }

    @Transactional
    public void confirmMatch(Long lostReportId, Long foundItemId, String userEmail) {
        LostReport lostReport = lostReportRepository.findById(lostReportId)
                .orElseThrow(() -> new BadRequestException("Lost report not found with ID: " + lostReportId));

        FoundItem foundItem = foundItemRepository.findById(foundItemId)
                .orElseThrow(() -> new BadRequestException("Found item not found with ID: " + foundItemId));

        User currentUser = authService.getUserEntityByEmail(userEmail);

        boolean isLostOwner = lostReport.getReporter().getId().equals(currentUser.getId());
        boolean isFoundOwner = foundItem.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isLostOwner && !isFoundOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to confirm this match.");
        }

        if (lostReport.getStatus() == ReportStatus.CLOSED) {
            throw new BadRequestException("Lost report is already closed.");
        }

        if (foundItem.getStatus() == ItemStatus.RETURNED) {
            throw new BadRequestException("Found item has already been returned.");
        }

        // Mark lost report as MATCHED
        lostReport.setStatus(ReportStatus.MATCHED);
        lostReportRepository.save(lostReport);

        // Mark found item as CLAIMED
        foundItem.setStatus(ItemStatus.CLAIMED);
        foundItemRepository.save(foundItem);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        // Normalize: lowercase, remove non-alphanumeric, split by whitespace
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = normalized.split("\\s+");

        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }
}
