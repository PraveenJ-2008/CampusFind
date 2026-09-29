package com.campusfind.dto;

import java.util.List;

public class AdminDashboardDto {

    private long totalLostReports;
    private long totalFoundItems;
    private long pendingLostReports;
    private long matchedReports;
    private long closedReports;
    private long availableFoundItems;
    private long claimedFoundItems;
    private long returnedFoundItems;
    private long totalUsers;
    private List<LostReportResponse> recentLostReports;
    private List<FoundItemResponse> recentFoundItems;

    public AdminDashboardDto() {
    }

    // Getters and Setters

    public long getTotalLostReports() {
        return totalLostReports;
    }

    public void setTotalLostReports(long totalLostReports) {
        this.totalLostReports = totalLostReports;
    }

    public long getTotalFoundItems() {
        return totalFoundItems;
    }

    public void setTotalFoundItems(long totalFoundItems) {
        this.totalFoundItems = totalFoundItems;
    }

    public long getPendingLostReports() {
        return pendingLostReports;
    }

    public void setPendingLostReports(long pendingLostReports) {
        this.pendingLostReports = pendingLostReports;
    }

    public long getMatchedReports() {
        return matchedReports;
    }

    public void setMatchedReports(long matchedReports) {
        this.matchedReports = matchedReports;
    }

    public long getClosedReports() {
        return closedReports;
    }

    public void setClosedReports(long closedReports) {
        this.closedReports = closedReports;
    }

    public long getAvailableFoundItems() {
        return availableFoundItems;
    }

    public void setAvailableFoundItems(long availableFoundItems) {
        this.availableFoundItems = availableFoundItems;
    }

    public long getClaimedFoundItems() {
        return claimedFoundItems;
    }

    public void setClaimedFoundItems(long claimedFoundItems) {
        this.claimedFoundItems = claimedFoundItems;
    }

    public long getReturnedFoundItems() {
        return returnedFoundItems;
    }

    public void setReturnedFoundItems(long returnedFoundItems) {
        this.returnedFoundItems = returnedFoundItems;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public List<LostReportResponse> getRecentLostReports() {
        return recentLostReports;
    }

    public void setRecentLostReports(List<LostReportResponse> recentLostReports) {
        this.recentLostReports = recentLostReports;
    }

    public List<FoundItemResponse> getRecentFoundItems() {
        return recentFoundItems;
    }

    public void setRecentFoundItems(List<FoundItemResponse> recentFoundItems) {
        this.recentFoundItems = recentFoundItems;
    }
}
