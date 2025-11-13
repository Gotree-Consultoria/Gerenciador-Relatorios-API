package com.gotree.API.entities;

import com.gotree.API.enums.AgendaEventType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "tb_agenda_event")
@Data
public class AgendaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Lob
    private String description; // Aqui podemos salvar: "Essa visita foi reagendado"

    // Este campo guardará o "Novo agendamento"
    private LocalDate eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Guarda o ID do TechnicalVisit de origem.
    @Column(name = "source_visit_id", unique = true, nullable = true)
    private Long sourceVisitId;

    // Guarda a "Data original" do TechnicalVisit
    @Column(name = "original_visit_date")
    private LocalDate originalVisitDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private AgendaEventType eventType;
}