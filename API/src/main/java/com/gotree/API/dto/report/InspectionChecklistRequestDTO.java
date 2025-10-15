package com.gotree.API.dto.report;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class InspectionChecklistRequestDTO {
    // Campos do cabeçalho do formulário
    private Long empresaId;
    private Long unidadeId;
    private Long setorId;
    private LocalDate dataInspecao;
    private String responsavel;
    private String responsavelSigla;
    private String responsavelRegistro;
    private String localInspecao;

    // Campos de texto grandes
    private String anotacoes;
    private String observacoes;

    // Lista com os itens do checklist
    private List<ChecklistItemDTO> items;
}