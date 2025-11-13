package com.gotree.API.dto.dashboard;

import lombok.Data;

@Data
public class AdminStatsDTO {
    private long totalUsers;
    private long totalCompanies;
    private long totalDocuments;
    private long totalVisitTimeHours;   // Total de horas
    private long totalVisitTimeMinutes; // Minutos restantes
}