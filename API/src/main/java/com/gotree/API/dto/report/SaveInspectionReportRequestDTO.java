package com.gotree.API.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaveInspectionReportRequestDTO {
    // IDs para os relacionamentos
    private Long companyId;
    private Long unitId;
    private Long sectorId;

    // Título do Documento
    private String title;

    // Dados do cabeçalho do formulário
    @NotNull(message = "A data da inspeção é obrigatória.")
    private LocalDate inspectionDate;

    private String local;

    // Campos de texto grandes
    private String notes;
    private String observations;

    private String responsavelSigla;
    private String responsavelRegistro;

    // Objeto com os dados da assinatura do cliente
    private ClientSignatureDTO clientSignature;

    // Objeto com os dados da assinatura do técnico
    private TechnicianSignatureDTO technicianSignature;

    // Lista com os itens do checklist
    private List<SectionDTO> sections;

    private boolean useDigitalSignature;

}