package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    /**
     * Verifica a disponibilidade de agenda para uma data e turno específicos.
     *
     * @param auth  Dados de autenticação do usuário
     * @param date  Data para verificação
     * @param shift Turno (MANHA ou TARDE)
     * @return 200 OK se disponível, 409 Conflict se indisponível
     */
    @GetMapping("/check-availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAvailability(
            Authentication auth,
            @RequestParam LocalDate date,
            @RequestParam String shift
    ) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        String warning = agendaService.checkAvailability(user, date, shift);

        if (warning != null) {
            // 409 Conflict: Informa o frontend que a agenda está cheia
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", warning));
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Cria um novo evento na agenda do usuário.
     *
     * @param dto            Dados do evento a ser criado
     * @param authentication Dados do usuário autenticado
     * @return Dados do evento criado
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
     * @param authentication Dados do usuário autenticado
     * @return Lista de eventos do usuário
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
     * @param id             ID do evento a ser atualizado
     * @param dto            Novos dados do evento
     * @param authentication Dados do usuário autenticado
     * @return Dados do evento atualizado
     */
    @PutMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent updatedEvent = agendaService.updateEvent(id, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(updatedEvent));
    }

    /**
     * Reagenda uma visita técnica para nova data/horário.
     *
     * @param visitId        ID da visita a ser reagendada
     * @param dto            Dados do novo agendamento
     * @param authentication Dados do usuário autenticado
     * @return Dados da visita reagendada
     */
    @PutMapping("/visitas/{visitId}/reagendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> rescheduleVisit(
            @PathVariable Long visitId,
            @RequestBody @Valid RescheduleVisitDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent rescheduledEvent = agendaService.rescheduleVisit(visitId, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(rescheduledEvent));
    }

    /**
     * Remove um evento da agenda.
     *
     * @param id             ID do evento a ser removido
     * @param authentication Dados do usuário autenticado
     * @return 204 No Content
     */
    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        agendaService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna todos os eventos para administradores.
     * Permite filtrar por ID do usuário.
     *
     * @param userId ID do usuário para filtrar (opcional)
     * @return Lista de todos os eventos
     */
    @GetMapping("/eventos/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEventsForAdmin(
            @RequestParam(required = false) Long userId
    ) {
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForAdmin(userId);
        return ResponseEntity.ok(allEvents);
    }

    /**
     * Endpoint específico para validar se o relatório pode ser enviado.
     * Deve ser chamado pelo Frontend ANTES de abrir a tela de assinatura ou enviar os dados.
     */
    @GetMapping("/validate-report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validateReportSubmission(
            Authentication auth,
            @RequestParam Long visitId,
            @RequestParam LocalDate date,
            @RequestParam String shift
    ) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        try {
            // Chama a validação criada no Service
            agendaService.validateReportSubmission(visitId, user, date, shift);

            // Se passar sem erro, retorna 200 OK
            return ResponseEntity.ok().build();

        } catch (IllegalStateException e) {
            // Se houver conflito (bloqueio), retorna 409 Conflict com a mensagem do Service
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Erro de dados (ex: turno inválido)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    /**
     * Retorna a disponibilidade de agenda do usuário para um determinado mês.
     * Este endpoint é utilizado para visualizar os dias disponíveis e ocupados em um calendário mensal.
     *
     * @param auth  Dados de autenticação do usuário atual
     * @param year  Ano para consulta da disponibilidade
     * @param month Mês para consulta da disponibilidade (1-12)
     * @return Lista de disponibilidade diária contendo informações sobre os horários livres e ocupados
     */
    @GetMapping("/availability")
    public ResponseEntity<List<MonthlyAvailabilityDTO>> getAvailability(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month) {

        User user = ((CustomUserDetails) auth.getPrincipal()).user();
        return ResponseEntity.ok(agendaService.getMonthAvailability(user, year, month));
    }
}