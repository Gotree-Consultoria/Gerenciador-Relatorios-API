package com.gotree.API.services;

import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.enums.AgendaEventType;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo gerenciamento de eventos na agenda, incluindo eventos genéricos
 * e visitas técnicas. Oferece funcionalidades para criar, atualizar, excluir e consultar
 * eventos, além de gerenciar reagendamentos de visitas técnicas.
 */
@Service
public class AgendaService {
    
    

    private final AgendaEventRepository agendaEventRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final UserRepository userRepository;

    public AgendaService(AgendaEventRepository agendaEventRepository,
                         TechnicalVisitRepository technicalVisitRepository,
                         UserRepository userRepository) {
        this.agendaEventRepository = agendaEventRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.userRepository = userRepository;
    }

    /**
     * Cria um novo evento de agenda genérico (ex: Reunião, Integração).
     */
    @Transactional
    public AgendaEvent createEvent(CreateEventDTO dto, User user) {
        AgendaEvent event = new AgendaEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setUser(user);

        // Converte a String do DTO para Enum
        try {
            // Garante que o tipo seja válido (EVENTO ou TREINAMENTO)
            AgendaEventType type = AgendaEventType.valueOf(dto.getEventType().toUpperCase());
            if (type == AgendaEventType.VISITA_REAGENDADA) {
                throw new IllegalArgumentException("Não é possível criar uma 'VISITA_REAGENDADA' diretamente.");
            }
            event.setEventType(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de evento inválido: " + dto.getEventType());
        }

        return agendaEventRepository.save(event);
    }

    /**
     * Atualiza um evento existente na agenda.
     *
     * @param eventId     ID do evento a ser atualizado
     * @param dto         Objeto contendo os novos dados do evento
     * @param currentUser Usuário que está realizando a atualização
     * @return O evento atualizado
     * @throws RuntimeException         Se o evento não for encontrado
     * @throws SecurityException        Se o usuário não tiver permissão para modificar o evento
     * @throws IllegalArgumentException Se tentar modificar um evento que não seja genérico
     */
    @Transactional
    public AgendaEvent updateEvent(Long eventId, CreateEventDTO dto, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento com ID " + eventId + " não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a modificar este evento.");
        }

        // Compara usando o Enum
        if (event.getEventType() == AgendaEventType.VISITA_REAGENDADA) {
            throw new IllegalArgumentException("Visitas reagendadas devem ser alteradas pela sua própria rota.");
        }

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());

        return agendaEventRepository.save(event);
    }


    /**
     * Reagenda uma visita técnica existente.
     *
     * @param visitId     ID da visita técnica a ser reagendada
     * @param dto         Objeto contendo os dados do reagendamento
     * @param currentUser Usuário que está realizando o reagendamento
     * @return O evento de reagendamento criado
     * @throws RuntimeException  Se a visita técnica não for encontrada
     * @throws SecurityException Se o usuário não tiver permissão para reagendar a visita
     */
    @Transactional
    public AgendaEvent rescheduleVisit(Long visitId, RescheduleVisitDTO dto, User currentUser) {

        // 1. Busca o Relatório de Visita original (que é imutável)
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + visitId + " não encontrado."));

        // 2. Verifica permissão
        if (!visit.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a reagendar esta visita.");
        }

        // 3. Verifica se já existe um "evento de reagendamento" para esta visita
        AgendaEvent event = agendaEventRepository.findBySourceVisitId(visitId)
                .orElse(new AgendaEvent()); // Se não existe, cria um novo

        // 4. Preenche os dados conforme sua regra de negócio
        event.setUser(currentUser);

        // 5. Usa o Enum
        event.setEventType(AgendaEventType.VISITA_REAGENDADA);

        event.setTitle("Reagendamento: " + visit.getTitle());
        event.setDescription("Visita reagendada. Motivo: " + (dto.getReason() != null ? dto.getReason() : "Não especificado."));
        event.setOriginalVisitDate(visit.getNextVisitDate());
        event.setEventDate(dto.getNewDate());
        event.setSourceVisitId(visit.getId());

        return agendaEventRepository.save(event);
    }

    /**
     * Remove um evento da agenda.
     *
     * @param eventId     ID do evento a ser removido
     * @param currentUser Usuário que está realizando a remoção
     * @throws RuntimeException  Se o evento não for encontrado
     * @throws SecurityException Se o usuário não tiver permissão para deletar o evento
     */
    @Transactional
    public void deleteEvent(Long eventId, User currentUser) {
        // Agora este metodo pode deletar *qualquer* tipo de evento (genérico ou reagendado)
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento com ID " + eventId + " não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a deletar este evento.");
        }

        // Se deletar um evento "VISITA_REAGENDADA", o relatório original (Próxima Visita)
        // voltará a aparecer automaticamente na agenda no próximo GET.
        agendaEventRepository.delete(event);
    }

    /**
     * Busca todos os compromissos de um usuário específico.
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForUser(User user) {
        // 1. Busca os eventos reais do usuário
        List<AgendaEvent> persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
        // 2. Busca as visitas virtuais do usuário
        List<TechnicalVisit> scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(user);
        // 3. Delega ao helper para processar e ordenar
        return aggregateAndSortEvents(persistentEvents, scheduledVisits);
    }

    /**
     * Busca todos os eventos de todos os usuários do sistema.
     * Este metodo é destinado apenas para usuários com perfil administrativo.
     *
     * @return Lista de eventos ordenada por data
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForAdmin(Long userId) {
        // Usa o método otimizado que traz o usuário junto
        List<AgendaEvent> persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
        // 2. Busca TODAS as visitas virtuais
        List<TechnicalVisit> scheduledVisits = technicalVisitRepository.findAllScheduledWithCompany();

        if (userId != null) {
            // --- LÓGICA COM FILTRO ---
            // 1. Busca o usuário (necessário para o repositório de visitas)
            User filterUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário com ID " + userId + " não encontrado."));

            // 2. Busca Eventos do Usuário (usando o novo método otimizado)
            persistentEvents = agendaEventRepository.findByUserIdWithUserOrderByEventDateAsc(userId);

            // 3. Busca Visitas do Usuário (reutiliza o método existente)
            scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(filterUser);

        } else {
            // --- LÓGICA SEM FILTRO (TODOS) ---
            persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
            scheduledVisits = technicalVisitRepository.findAllScheduledWithCompany();
        }

        // 3. Delega ao helper para processar e ordenar
        return aggregateAndSortEvents(persistentEvents, scheduledVisits);
    }

    // ==========================================
    // --- MÉTODOS AUXILIARES (HELPERS) ---
    // ==========================================

    /**
     * Converte um AgendaEvent (do banco) em um DTO de resposta.
     */
    private List<AgendaResponseDTO> aggregateAndSortEvents(List<AgendaEvent> persistentEvents, List<TechnicalVisit> scheduledVisits) {
        List<AgendaResponseDTO> allEvents = new ArrayList<>();

        // 1. Extrai os IDs das visitas que JÁ FORAM REAGENDADAS
        Set<Long> rescheduledVisitIds = persistentEvents.stream()
                // 6. Compara usando o Enum
                .filter(e -> e.getEventType() == AgendaEventType.VISITA_REAGENDADA && e.getSourceVisitId() != null)
                .map(AgendaEvent::getSourceVisitId)
                .collect(Collectors.toSet());

        // 2. Mapeia os eventos reais (do banco)
        for (AgendaEvent event : persistentEvents) {
            allEvents.add(mapToDto(event));
        }

        // 3. Mapeia as visitas "virtuais", IGNORANDO as que já foram reagendadas
        for (TechnicalVisit visit : scheduledVisits) {
            if (!rescheduledVisitIds.contains(visit.getId())) {
                allEvents.add(mapVisitToDto(visit));
            }
        }

        // 4. Ordena a lista final
        allEvents.sort(Comparator.comparing(AgendaResponseDTO::getDate));
        return allEvents;
    }

    /**
     * Converte um AgendaEvent (do banco) em um DTO de resposta.
     */
    public AgendaResponseDTO mapToDto(AgendaEvent event) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        dto.setTitle(event.getTitle());
        dto.setDate(event.getEventDate());
        dto.setDescription(event.getDescription());
        dto.setType(event.getEventType().name());
        dto.setReferenceId(event.getId());
        dto.setOriginalVisitDate(event.getOriginalVisitDate());
        dto.setSourceVisitId(event.getSourceVisitId());

        if (event.getUser() != null) {
            dto.setResponsibleName(event.getUser().getName());
        }

        return dto;
    }

    /**
     * Converte uma entidade TechnicalVisit em um DTO de resposta.
     * O metodo constrói um título descritivo incluindo informações da empresa,
     * unidade e setor quando disponíveis.
     *
     * @param visit Visita técnica a ser convertida
     * @return DTO contendo os dados da visita
     */
    private AgendaResponseDTO mapVisitToDto(TechnicalVisit visit) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        String companyName = (visit.getClientCompany() != null) ? visit.getClientCompany().getName() : "Empresa N/A";
        String unitName = (visit.getUnit() != null) ? visit.getUnit().getName() : null;
        String sectorName = (visit.getSector() != null) ? visit.getSector().getName() : null;
        StringBuilder titleBuilder = new StringBuilder("Próxima Visita: " + companyName);
        if (unitName != null && !unitName.isBlank()) {
            titleBuilder.append(" (Unidade: ").append(unitName);
            if (sectorName != null && !sectorName.isBlank()) {
                titleBuilder.append(" - Setor: ").append(sectorName);
            }
            titleBuilder.append(")");
        } else if (sectorName != null && !sectorName.isBlank()) {
            titleBuilder.append(" (Setor: ").append(sectorName).append(")");
        }
        dto.setTitle(titleBuilder.toString());
        dto.setDate(visit.getNextVisitDate());
        dto.setType("VISITA");
        dto.setReferenceId(visit.getId());
        dto.setUnitName(unitName);
        dto.setSectorName(sectorName);
        dto.setOriginalVisitDate(null);
        dto.setSourceVisitId(null);

        if (visit.getTechnician() != null) {
            dto.setResponsibleName(visit.getTechnician().getName());
        }

        return dto;
    }
}