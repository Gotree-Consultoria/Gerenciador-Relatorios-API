package com.gotree.API.dto.aep;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AepDetailDTO {
    private Long id;
    private Long companyId;
    private String companyName;
    private String companyCnpj;
    private LocalDate evaluationDate;
    private String evaluatedFunction;
    private Long physiotherapistId;
    private List<String> selectedRisks; // A lista de riscos marcados

    // Dados do Avaliador (para referência)
    private Long evaluatorId;
    private String evaluatorName;
}