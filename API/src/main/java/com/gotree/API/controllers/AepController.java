package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.aep.AepDetailDTO;
import com.gotree.API.dto.aep.AepRequestDTO;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.User;
import com.gotree.API.services.AepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; // Importe o PathVariable

import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações relacionadas a Avaliações de Eficácia Profissional (AEPs).
 * Fornece endpoints para criar, atualizar e recuperar informações de AEPs.
 */
@RestController
@RequestMapping("/aep-reports")
public class AepController {
    
    

    private final AepService aepService;

    public AepController(AepService aepService) {
        this.aepService = aepService;
    }

    /**
     * Cria uma nova Avaliação de Eficácia Profissional (AEP).
     *
     * @param dto            - Objeto DTO contendo os dados da AEP a ser criada
     * @param authentication - Objeto de autenticação do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID da AEP criada
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createAepReport(@RequestBody @Valid AepRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User evaluator = userDetails.user();

        // Chama o serviço para SALVAR os dados (sem ID existente)
        AepReport createdAep = aepService.saveAepData(dto, evaluator, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "AEP salva com sucesso!",
                        "reportId", createdAep.getId()
                ));
    }

    /**
     * Atualiza uma Avaliação de Eficácia Profissional (AEP) existente.
     *
     * @param id             - ID da AEP a ser atualizada
     * @param dto            - Objeto DTO contendo os novos dados da AEP
     * @param authentication - Objeto de autenticação do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID da AEP atualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateAepReport(@PathVariable Long id, @RequestBody @Valid AepRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User evaluator = userDetails.user();

        // Chama o serviço para ATUALIZAR os dados (com ID existente)
        AepReport updatedAep = aepService.saveAepData(dto, evaluator, id);

        return ResponseEntity
                .ok()
                .body(Map.of(
                        "message", "AEP atualizada com sucesso!",
                        "reportId", updatedAep.getId()
                ));
    }

    /**
     * Recupera os detalhes de uma Avaliação de Eficácia Profissional (AEP) específica.
     *
     * @param id             - ID da AEP a ser consultada
     * @param authentication - Objeto de autenticação do usuário atual
     * @return ResponseEntity contendo os detalhes da AEP solicitada
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AepDetailDTO> getAepReportDetails(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.user();

        AepDetailDTO aepDetails = aepService.findAepDetails(id, currentUser);

        return ResponseEntity.ok(aepDetails);
    }

}