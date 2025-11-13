package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.visit.CreateTechnicalVisitRequestDTO;
import com.gotree.API.dto.visit.TechnicalVisitResponseDTO;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.mappers.TechnicalVisitMapper;
import com.gotree.API.services.TechnicalVisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller responsável por gerenciar as operações relacionadas às visitas técnicas.
 * Fornece endpoints para:
 * - Criar novas visitas técnicas e gerar relatórios em PDF
 * - Listar visitas técnicas do técnico autenticado
 * <p>
 * Base URL: /technical-visits
 */
@RestController
@RequestMapping("/technical-visits")
public class TechnicalVisitController {
    
    

    private final TechnicalVisitService technicalVisitService;
    private final TechnicalVisitMapper technicalVisitMapper;

    public TechnicalVisitController(TechnicalVisitService technicalVisitService, TechnicalVisitMapper technicalVisitMapper) {
        this.technicalVisitService = technicalVisitService;
        this.technicalVisitMapper = technicalVisitMapper;
    }

    /**
     * Cria uma nova visita técnica e gera o relatório em PDF.
     *
     * @param dto            Dados da visita técnica a ser criada
     * @param authentication Dados do usuário autenticado
     * @return ResponseEntity com mensagem de sucesso e ID da visita criada
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVisit(@RequestBody @Valid CreateTechnicalVisitRequestDTO dto, Authentication authentication) {
        // Obtém os detalhes do utilizador autenticado de forma segura.
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        // Delega toda a lógica de negócio (criação, geração de PDF, salvamento) para o serviço.
        TechnicalVisit createdVisit = technicalVisitService.createAndGeneratePdf(dto, technician);

        // Retorna uma resposta de sucesso para o frontend com uma mensagem e o ID da visita criada.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Relatório de visita técnica criado com sucesso!",
                        "visitId", createdVisit.getId()
                ));
    }

    /**
     * Retorna todas as visitas técnicas realizadas pelo técnico autenticado.
     *
     * @param authentication Dados do usuário autenticado
     * @return Lista de visitas técnicas do técnico
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TechnicalVisitResponseDTO>> findMyVisits(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        List<TechnicalVisit> visits = technicalVisitService.findAllByTechnician(technician);

        List<TechnicalVisitResponseDTO> responseDtos = technicalVisitMapper.toDtoList(visits);

        return ResponseEntity.ok(responseDtos);
    }
}
