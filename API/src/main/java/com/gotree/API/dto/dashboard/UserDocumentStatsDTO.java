package com.gotree.API.dto.dashboard;

import lombok.Data;

@Data
public class UserDocumentStatsDTO {
    private Long userId;
    private String userName;
    private long totalVisits;
    private long totalAeps;
    private long totalRisks;
    private long totalDocuments;

    public UserDocumentStatsDTO(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.totalVisits = 0;
        this.totalAeps = 0;
        this.totalRisks = 0;
        this.totalDocuments = 0;
    }
}