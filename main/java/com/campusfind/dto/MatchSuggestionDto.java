package com.campusfind.dto;

import java.time.LocalDate;
import java.util.List;

public class MatchSuggestionDto {

    private Long lostId;
    private String lostItemTitle;
    private Long foundItemId;
    private String foundItemTitle;
    private String foundItemDescription;
    private String foundItemLocation;
    private LocalDate foundDate;
    private int score;
    private List<String> matchingKeywords;
    private String categoryName;
    private boolean categoryMatched;

    public MatchSuggestionDto() {
    }

    public MatchSuggestionDto(Long lostId, String lostItemTitle, Long foundItemId, String foundItemTitle,
                              String foundItemDescription, String foundItemLocation, LocalDate foundDate,
                              int score, List<String> matchingKeywords, String categoryName, boolean categoryMatched) {
        this.lostId = lostId;
        this.lostItemTitle = lostItemTitle;
        this.foundItemId = foundItemId;
        this.foundItemTitle = foundItemTitle;
        this.foundItemDescription = foundItemDescription;
        this.foundItemLocation = foundItemLocation;
        this.foundDate = foundDate;
        this.score = score;
        this.matchingKeywords = matchingKeywords;
        this.categoryName = categoryName;
        this.categoryMatched = categoryMatched;
    }

    // Getters and Setters

    public Long getLostId() {
        return lostId;
    }

    public void setLostId(Long lostId) {
        this.lostId = lostId;
    }

    public String getLostItemTitle() {
        return lostItemTitle;
    }

    public void setLostItemTitle(String lostItemTitle) {
        this.lostItemTitle = lostItemTitle;
    }

    public Long getFoundItemId() {
        return foundItemId;
    }

    public void setFoundItemId(Long foundItemId) {
        this.foundItemId = foundItemId;
    }

    public String getFoundItemTitle() {
        return foundItemTitle;
    }

    public void setFoundItemTitle(String foundItemTitle) {
        this.foundItemTitle = foundItemTitle;
    }

    public String getFoundItemDescription() {
        return foundItemDescription;
    }

    public void setFoundItemDescription(String foundItemDescription) {
        this.foundItemDescription = foundItemDescription;
    }

    public String getFoundItemLocation() {
        return foundItemLocation;
    }

    public void setFoundItemLocation(String foundItemLocation) {
        this.foundItemLocation = foundItemLocation;
    }

    public LocalDate getFoundDate() {
        return foundDate;
    }

    public void setFoundDate(LocalDate foundDate) {
        this.foundDate = foundDate;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getMatchingKeywords() {
        return matchingKeywords;
    }

    public void setMatchingKeywords(List<String> matchingKeywords) {
        this.matchingKeywords = matchingKeywords;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isCategoryMatched() {
        return categoryMatched;
    }

    public void setCategoryMatched(boolean categoryMatched) {
        this.categoryMatched = categoryMatched;
    }
}
