package com.gotree.API.dto.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class MyStatsDTO {
    private long totalVisits;
    private long totalAeps;
    private long totalRisks;
    private long totalVisitTimeHours;   // Total de horas
    private long totalVisitTimeMinutes; // Minutos restantes
    private List<CompanyCountDTO> topCompanies;
}