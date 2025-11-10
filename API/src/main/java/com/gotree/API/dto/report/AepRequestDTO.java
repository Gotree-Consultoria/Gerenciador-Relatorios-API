package com.gotree.API.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AepRequestDTO {
    private Long companyId;

    @NotNull(message = "A data da Avaliação é obrigatória.")
    private LocalDate evaluationDate; // Data da Avaliação
    private String evaluatedFunction; // Função Avaliada
    private List<String> selectedRiskIds; // Apenas os IDs dos riscos selecionados

    // --- Campos para a Fisioterapeuta
    private Long physiotherapistId;
}