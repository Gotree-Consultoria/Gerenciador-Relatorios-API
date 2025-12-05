package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.risk.SaveRiskReportRequestDTO;
import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.services.RiskChecklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller responsável por gerenciar as operações relacionadas aos checklists de risco ocupacional.
 * Fornece endpoints para criação, atualização e gerenciamento de relatórios de risco.
 *
 * Este controller expõe as seguintes operações:
 * - POST /risk-checklist: Cria um novo relatório de risco
 * - PUT /risk-checklist/{id}: Atualiza um relatório existente
 */
@RestController
@RequestMapping("/risk-checklist")
public class RiskChecklistController {

    private final RiskChecklistService service;

    /**
     * Construtor que inicializa o controller com o serviço necessário.
     *
     * @param service Serviço que contém a lógica de negócios para checklist de riscos
     */
    public RiskChecklistController(RiskChecklistService service) {
        this.service = service;
    }

    /**
     * Busca os dados detalhados de um relatório de risco para edição.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SaveRiskReportRequestDTO> getReportDetailsForEdit(@PathVariable Long id, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();

        // Você precisa criar este método no seu 'RiskChecklistService'
        SaveRiskReportRequestDTO reportDto = service.findReportForEdit(id, user);

        return ResponseEntity.ok(reportDto);
    }

    /**
     * Cria um novo relatório de risco ocupacional e gera o PDF correspondente.
     *
     * @param dto            DTO contendo os dados necessários para criar o relatório
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID do relatório criado
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> create(@RequestBody SaveRiskReportRequestDTO dto, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();
        OccupationalRiskReport report = service.createAndGeneratePdf(dto, user);

        return ResponseEntity.ok(Map.of("message", "Criado com sucesso", "id", report.getId()));
    }

    /**
     * Atualiza um relatório de risco ocupacional existente e regenera o PDF.
     *
     * @param id             ID do relatório a ser atualizado
     * @param dto            DTO contendo os novos dados do relatório
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID do relatório atualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SaveRiskReportRequestDTO dto, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();
        OccupationalRiskReport report = service.updateReport(id, dto, user);

        return ResponseEntity.ok(Map.of("message", "Atualizado com sucesso", "id", report.getId()));
        
    }
}