// Sugestão de novo nome: AepRequestDTO.java
package com.gotree.API.dto.report;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AepRequestDTO { // Nome atualizado
    private Long companyId;
    private Long unitId;
    private Long sectorId;
    private String evaluator; // Nome do Avaliador
    private LocalDate evaluationDate; // Data da Avaliação
    private String evaluatedFunction; // Função Avaliada
    private List<String> selectedRiskIds; // Apenas os IDs dos riscos selecionados
}