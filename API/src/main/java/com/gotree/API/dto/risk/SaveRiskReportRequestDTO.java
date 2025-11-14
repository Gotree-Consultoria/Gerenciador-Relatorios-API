package com.gotree.API.dto.risk;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaveRiskReportRequestDTO {

    private String title;
    private Long companyId;
    private Long unitId;
    private Long sectorId;
    private LocalDate inspectionDate;
    private String technicianSignatureImageBase64; // Base64 da assinatura

    // Lista de funções avaliadas
    private List<EvaluatedFunctionRequestDTO> functions;
}