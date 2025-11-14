package com.gotree.API.dto.document; // Crie este novo pacote

import lombok.Data;
import java.time.LocalDate;

@Data
public class DocumentSummaryDTO {

    private Long id;
    private String documentType; // Ex: "Checklist de Inspeção", "Relatório de Visita"
    private String title;
    private String clientName;
    private LocalDate creationDate; // Um campo de data comum para ordenação
    private boolean signed;
}