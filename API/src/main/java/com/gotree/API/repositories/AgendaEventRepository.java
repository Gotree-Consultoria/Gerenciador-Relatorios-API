package com.gotree.API.repositories;

import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Long> {

    /**
     * Busca todos os eventos com JOIN no usuário para performance.
     */
    @Query("SELECT e FROM AgendaEvent e JOIN FETCH e.user ORDER BY e.eventDate ASC")
    List<AgendaEvent> findAllWithUserByOrderByEventDateAsc(); // Mudei o nome para ser claro

    List<AgendaEvent> findAllByOrderByEventDateAsc();

    // Encontra um evento de reagendamento pelo ID do relatório original
    Optional<AgendaEvent> findBySourceVisitId(Long sourceVisitId);

    // Encontra todos os eventos de um usuário que são de um certo tipo
    List<AgendaEvent> findByUserAndEventTypeIn(User user, List<String> types);

    /**
     * (NOVO) Filtra por usuário específico, trazendo os dados do usuário (JOIN FETCH).
     */
    @Query("SELECT e FROM AgendaEvent e JOIN FETCH e.user WHERE e.user.id = :userId ORDER BY e.eventDate ASC")
    List<AgendaEvent> findByUserIdWithUserOrderByEventDateAsc(@Param("userId") Long userId);
}