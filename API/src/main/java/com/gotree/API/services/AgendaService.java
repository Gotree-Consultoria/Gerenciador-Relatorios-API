package com.gotree.API.services;

import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.enums.AgendaEventType;
import com.gotree.API.enums.Shift;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo gerenciamento de eventos de agenda e visitas técnicas.
 * Fornece funcionalidades para criar, atualizar, excluir e consultar eventos,
 * além de gerenciar o reagendamento de visitas técnicas.
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
     * NOVO MÉTODO: Valida se o relatório pode ser enviado.
     * Bloqueia se houver conflito com visita de outra empresa no mesmo turno.
     *
     * @param visitId ID da visita técnica que está sendo finalizada
     * @param technician Técnico responsável
     * @param date Data do relatório/visita
     * @param shiftStr Turno selecionado
     */
    public void validateReportSubmission(Long visitId, User technician, LocalDate date, String shiftStr) {
        try {
            Shift shift = Shift.valueOf(shiftStr.toUpperCase());

            // Busca todos os eventos agendados para este técnico, nesta data e turno
            List<AgendaEvent> conflictingEvents = agendaEventRepository.findByUserAndEventDateAndShift(technician, date, shift);

            for (AgendaEvent event : conflictingEvents) {
                // Se o evento encontrado for referente à MESMA visita que estamos preenchendo, ignora (não é conflito)
                if (event.getTechnicalVisit() != null && event.getTechnicalVisit().getId().equals(visitId)) {
                    continue;
                }

                // Se chegou aqui, encontrou um evento DIFERENTE no mesmo horário (outra empresa)
                String conflictingClient = (event.getClientName() != null) ? event.getClientName() : "Outro Cliente";
                if (event.getTechnicalVisit() != null && event.getTechnicalVisit().getClientCompany() != null) {
                    conflictingClient = event.getTechnicalVisit().getClientCompany().getName();
                }

                // Lança o bloqueio para o frontend capturar e avisar o usuário
                throw new IllegalStateException("BLOQUEIO DE AGENDA: Você já possui uma visita marcada na empresa '"
                        + conflictingClient + "' neste mesmo turno (" + shift + "). "
                        + "Retorne, altere o turno ou a data da visita atual e tente novamente.");
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Turno inválido fornecido para validação.");
        }
    }

    /**
     * Verifica a disponibilidade de um técnico em uma data e turno específicos.
     *
     * @param technician Técnico a ser verificado
     * @param date       Data para verificação
     * @param shiftStr   Turno desejado ("MANHA" ou "TARDE")
     * @return Mensagem de erro se houver conflito, null caso contrário
     */
    public String checkAvailability(User technician, LocalDate date, String shiftStr) {
        try {
            Shift shift = Shift.valueOf(shiftStr.toUpperCase());
            long eventsInDay = agendaEventRepository.countByUserAndEventDate(technician, date);
            if (eventsInDay >= 2) {
                return "Você já possui visitas agendadas para os turnos manhã e tarde nesta data. Escolha outra data.";
            }
            long eventsInShift = agendaEventRepository.countByUserAndEventDateAndShift(technician, date, shift);
            if (eventsInShift > 0) {
                return "Você já possui uma visita agendada neste turno (" + shift + "). Escolha outro turno.";
            }
            return null;
        } catch (IllegalArgumentException e) {
            return "Turno inválido.";
        }
    }

    /**
     * Cria um novo evento na agenda.
     */
    @Transactional
    public AgendaEvent createEvent(CreateEventDTO dto, User user) {
        String conflict = checkAvailability(user, dto.getEventDate(), dto.getShift());
        if (conflict != null) {
            throw new IllegalStateException(conflict);
        }

        AgendaEvent event = new AgendaEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setUser(user);

        // Salva os campos manuais
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        try {
            event.setShift(Shift.valueOf(dto.getShift().toUpperCase()));
            AgendaEventType type = AgendaEventType.valueOf(dto.getEventType().toUpperCase());
            if (type == AgendaEventType.VISITA_REAGENDADA) {
                throw new IllegalArgumentException("Use a rota de reagendamento para visitas.");
            }
            event.setEventType(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dados inválidos: " + e.getMessage());
        }

        return agendaEventRepository.save(event);
    }

    /**
     * Atualiza um evento existente na agenda.
     */
    @Transactional
    public AgendaEvent updateEvent(Long eventId, CreateEventDTO dto, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão.");
        }

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        // Atualiza manuais também
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        return agendaEventRepository.save(event);
    }

    /**
     * Reagenda uma visita técnica.
     */
    @Transactional
    public AgendaEvent rescheduleVisit(Long visitId, RescheduleVisitDTO dto, User currentUser) {
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visita não encontrada"));

        AgendaEvent event = agendaEventRepository.findByTechnicalVisit_Id(visitId)
                .orElse(new AgendaEvent());

        event.setUser(currentUser);
        event.setEventType(AgendaEventType.VISITA_REAGENDADA);
        event.setTitle("Reagendamento: " + visit.getTitle());
        event.setOriginalVisitDate(visit.getNextVisitDate());
        event.setTechnicalVisit(visit); // Vínculo forte
        event.setEventDate(dto.getNewDate());

        if (event.getShift() == null) event.setShift(Shift.MANHA);

        return agendaEventRepository.save(event);
    }

    /**
     * Remove um evento da agenda.
     */
    @Transactional
    public void deleteEvent(Long eventId, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão.");
        }
        agendaEventRepository.delete(event);
    }

    /**
     * Busca todos os eventos de agenda para um usuário específico.
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForUser(User user) {
        List<AgendaEvent> persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
        List<TechnicalVisit> scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(user);
        return aggregateAndSortEvents(persistentEvents, scheduledVisits);
    }

    /**
     * Busca todos os eventos de agenda com filtro opcional por usuário (visão administrativa).
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForAdmin(Long userId) {
        List<AgendaEvent> persistentEvents;
        List<TechnicalVisit> scheduledVisits;

        if (userId != null) {
            User filterUser = userRepository.findById(userId).orElseThrow();
            persistentEvents = agendaEventRepository.findByUserIdWithUserOrderByEventDateAsc(userId);
            scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(filterUser);
        } else {
            persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
            scheduledVisits = technicalVisitRepository.findAllScheduledWithCompany();
        }

        return aggregateAndSortEvents(persistentEvents, scheduledVisits);
    }

    // AgendaService.java

    @Transactional(readOnly = true)
    public List<MonthlyAvailabilityDTO> getMonthAvailability(User technician, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 1. Busca visitas REALIZADAS neste mês (visit_date)
        List<TechnicalVisit> visitsRealized = technicalVisitRepository.findByTechnicianAndDateRange(technician, start, end);

        // 2. NOVO: Busca visitas AGENDADAS para este mês (next_visit_date)
        List<TechnicalVisit> visitsScheduled = technicalVisitRepository.findByTechnicianAndNextVisitDateBetween(technician, start, end);

        // 3. Busca eventos manuais (reuniões, folgas)
        List<AgendaEvent> manualEvents = agendaEventRepository.findByUserAndEventDateBetween(technician, start, end);

        List<MonthlyAvailabilityDTO> availability = new ArrayList<>();

        // Itera dia a dia do mês
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate currentDate = date;

            boolean morningBusy = false;
            boolean afternoonBusy = false;

            // --- A. Checa Visitas REALIZADAS (baseado na hora de início) ---
            for (TechnicalVisit v : visitsRealized) {
                if (v.getVisitDate().equals(currentDate)) {
                    if (v.getStartTime().getHour() < 12) morningBusy = true;
                    else afternoonBusy = true;
                }
            }

            // --- B. Checa Visitas AGENDADAS (baseado no Turno Gravado) ---
            for (TechnicalVisit v : visitsScheduled) {
                if (v.getNextVisitDate().equals(currentDate)) {
                    if (v.getNextVisitShift() == Shift.MANHA) morningBusy = true;
                    else if (v.getNextVisitShift() == Shift.TARDE) afternoonBusy = true;
                }
            }

            // --- C. Checa Eventos Manuais ---
            for (AgendaEvent e : manualEvents) {
                if (e.getEventDate().equals(currentDate)) {
                    // Evita duplicidade se o evento já for vinculado a uma visita técnica
                    if (e.getTechnicalVisit() != null) continue;

                    if (e.getShift() == Shift.MANHA) morningBusy = true;
                    else if (e.getShift() == Shift.TARDE) afternoonBusy = true;
                }
            }

            // Se encontrou alguma ocupação, adiciona na lista
            if (morningBusy || afternoonBusy) {
                MonthlyAvailabilityDTO dto = new MonthlyAvailabilityDTO();
                dto.setDate(date);
                dto.setMorningBusy(morningBusy);
                dto.setAfternoonBusy(afternoonBusy);
                dto.setFullDayBusy(morningBusy && afternoonBusy);
                availability.add(dto);
            }
        }

        return availability;
    }

    private List<AgendaResponseDTO> aggregateAndSortEvents(List<AgendaEvent> persistentEvents, List<TechnicalVisit> scheduledVisits) {
        List<AgendaResponseDTO> allEvents = new ArrayList<>();

        Set<String> addedVirtualVisits = new java.util.HashSet<>();

        for (TechnicalVisit visit : scheduledVisits) {
            // Verifica se essa visita já foi convertida em um evento real (reagendado)
            boolean isRescheduled = persistentEvents.stream()
                    .anyMatch(e -> e.getTechnicalVisit() != null && e.getTechnicalVisit().getId().equals(visit.getId()));

            if (!isRescheduled) {
                // Cria uma chave única: DATA + TURNO + ID_EMPRESA
                String shiftKey = (visit.getNextVisitShift() != null) ? visit.getNextVisitShift().name() : "N/A";
                String clientKey = (visit.getClientCompany() != null) ? visit.getClientCompany().getId().toString() : "0";
                String uniqueKey = visit.getNextVisitDate().toString() + "_" + shiftKey + "_" + clientKey;

                // Só adiciona na lista se essa chave ainda não foi processada
                if (!addedVirtualVisits.contains(uniqueKey)) {
                    allEvents.add(mapVisitToDto(visit));
                    addedVirtualVisits.add(uniqueKey); // Marca como adicionado
                }
            }
        }

        allEvents.sort(Comparator.comparing(AgendaResponseDTO::getDate));
        return allEvents;
    }

    /**
     * Converte um evento de agenda para seu DTO correspondente.
     */
    public AgendaResponseDTO mapToDto(AgendaEvent event) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        dto.setReferenceId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDate(event.getEventDate());
        dto.setType(event.getEventType().name());
        dto.setDescription(event.getDescription());
        dto.setShift(event.getShift().name());
        if (event.getUser() != null) dto.setResponsibleName(event.getUser().getName());

        // LÓGICA HÍBRIDA
        if (event.getTechnicalVisit() != null) {
            // Automático (Vem da Visita)
            TechnicalVisit v = event.getTechnicalVisit();
            dto.setSourceVisitId(v.getId());
            if (v.getClientCompany() != null) {
                dto.setClientName(v.getClientCompany().getName());
            }
            if (v.getUnit() != null) dto.setUnitName(v.getUnit().getName());
            if (v.getSector() != null) dto.setSectorName(v.getSector().getName());
        } else {
            // Manual
            dto.setClientName(event.getClientName());
            if (event.getManualObservation() != null) {
                dto.setDescription(dto.getDescription() + " | " + event.getManualObservation());
            }
        }
        return dto;
    }

    private AgendaResponseDTO mapVisitToDto(TechnicalVisit visit) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        String companyName = (visit.getClientCompany() != null) ? visit.getClientCompany().getName() : "Empresa N/A";
        String unitName = (visit.getUnit() != null) ? visit.getUnit().getName() : null;
        String sectorName = (visit.getSector() != null) ? visit.getSector().getName() : null;

        // Constrói título
        StringBuilder titleBuilder = new StringBuilder("Próxima Visita: " + companyName);
        if (unitName != null) titleBuilder.append(" (").append(unitName).append(")");

        dto.setTitle(titleBuilder.toString());
        dto.setDate(visit.getNextVisitDate());
        dto.setType("VISITA"); // Tipo virtual
        dto.setReferenceId(visit.getId());

        if (visit.getNextVisitShift() != null) {
            dto.setShift(visit.getNextVisitShift().name()); // Converte Enum MANHA/TARDE para String
        }

        // Preenche dados para consistência
        dto.setClientName(companyName);
        dto.setUnitName(unitName);
        dto.setSectorName(sectorName);

        if (visit.getTechnician() != null) {
            dto.setResponsibleName(visit.getTechnician().getName());
        }
        return dto;
    }
}