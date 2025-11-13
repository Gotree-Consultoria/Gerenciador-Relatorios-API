package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import com.gotree.API.services.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST responsável por gerenciar operações relacionadas à agenda de eventos.
 * Fornece endpoints para criar, atualizar, excluir e consultar eventos da agenda.
 */
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    
    

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    /**
     * Cria um novo evento na agenda.
     *
     * @param dto            objeto contendo os dados do evento a ser criado
     * @param authentication objeto de autenticação do usuário atual
     * @return ResponseEntity contendo o evento criado
     */
    @PostMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> createEvent(
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication
    ) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent newEvent = agendaService.createEvent(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendaService.mapToDto(newEvent));
    }

    /**
     * Retorna todos os eventos da agenda do usuário autenticado.
     *
     * @param authentication objeto de autenticação do usuário atual
     * @return ResponseEntity contendo a lista de eventos
     */
    @GetMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEvents(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForUser(currentUser);
        return ResponseEntity.ok(allEvents);
    }

    /**
     * Atualiza um evento existente na agenda.
     *
     * @param id             identificador do evento a ser atualizado
     * @param dto            objeto contendo os novos dados do evento
     * @param authentication objeto de autenticação do usuário atual
     * @return ResponseEntity contendo o evento atualizado
     */
    @PutMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> updateEvent(
            @PathVariable Long id, // ID do AgendaEvent
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent updatedEvent = agendaService.updateEvent(id, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(updatedEvent));
    }

    /**
     * Reagenda uma visita técnica, convertendo-a em um evento de agenda.
     *
     * @param visitId        identificador da visita técnica a ser reagendada
     * @param dto            objeto contendo os dados do reagendamento
     * @param authentication objeto de autenticação do usuário atual
     * @return ResponseEntity contendo o evento reagendado
     */
    @PutMapping("/visitas/{visitId}/reagendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> rescheduleVisit(
            @PathVariable Long visitId, // ID do TechnicalVisit original
            @RequestBody @Valid RescheduleVisitDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent rescheduledEvent = agendaService.rescheduleVisit(visitId, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(rescheduledEvent));
    }

    /**
     * Remove um evento da agenda.
     *
     * @param id             identificador do evento a ser removido
     * @param authentication objeto de autenticação do usuário atual
     * @return ResponseEntity sem conteúdo
     */
    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id, // ID do AgendaEvent
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        agendaService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna todos os eventos da agenda do sistema (acesso administrativo).
     *
     * @return ResponseEntity contendo a lista completa de eventos
     *
     * Ver compromissos (Todos ou Filtrado).
     */
    @GetMapping("/eventos/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEventsForAdmin(
            @RequestParam(required = false) Long userId // <-- Parâmetro opcional
    ) {
        // Passa o userId (pode ser nulo) para o serviço
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForAdmin(userId);
        return ResponseEntity.ok(allEvents);
    }


}