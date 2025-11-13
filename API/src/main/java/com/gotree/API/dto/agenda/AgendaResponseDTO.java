package com.gotree.API.dto.agenda;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AgendaResponseDTO {
    private String title;
    private LocalDate date; // Esta será a "Nova Data" (eventDate)
    private String type; // "EVENTO" ou "VISITA"
    private String description; // "Essa visita foi reagendado..."
    private Long referenceId; // O ID do AgendaEvent ou do TechnicalVisit
    private String unitName;
    private String sectorName;

    private LocalDate originalVisitDate; // A "Data original"
    private Long sourceVisitId; // O ID do relatório de visita original

    private String responsibleName;
}