package com.gotree.API.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaveNrsReportRequestDTO {
    // Campos do 'SaveInspectionReportRequestDTO'
    private Long companyId;
    private Long unitId;
    private Long sectorId;
    private String title; // Ex: "1. Cozinha / Padaria" do PDF

    @NotNull(message = "A data da inspeção é obrigatória.")
    private LocalDate inspectionDate;
    private String local;
    private String notes;
    private String observations;
    private String responsavelSigla;
    private String responsavelRegistro;
    private ClientSignatureDTO clientSignature;
    private TechnicianSignatureDTO technicianSignature;
    private boolean useDigitalSignature;

    // --- A MUDANÇA ---
    // Aponta para a nova lista de DTOs
    private List<NrsSectionDTO> nrsSections;
}