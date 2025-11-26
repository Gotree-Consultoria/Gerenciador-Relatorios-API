package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Representa um achado/constatação registrada durante uma visita técnica.
 * 
 * Esta entidade persiste informações de evidência (caminhos de fotos),
 * contexto e análise (descrição, consequências e orientação legal),
 * além de dados operacionais (responsável, penalidades, prioridade,
 * prazo e recorrência). Cada achado pertence a uma {@link TechnicalVisit}.
 *
 * Mapeamento:
 * - Tabela: tb_visit_finding
 * - Relacionamento: ManyToOne (LAZY) com TechnicalVisit
 * - Campos de texto extensos: description, consequences e legalGuidance (@Lob)
 * - Prioridade: enum armazenado como STRING (BAIXA, MEDIA, ALTA)
 *
 * Uso típico:
 * - Listar e detalhar constatações de uma visita técnica
 * - Apoiar a geração de relatórios, planos de ação e acompanhamento de prazos
 */
@Entity
@Table(name = "tb_visit_finding")
@Data
public class VisitFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TechnicalVisit technicalVisit;

    private String photoPath1; // Caminho para a imagem salva no servidor
    private String photoPath2; // Pode ser nulo

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String consequences;

    @Column(columnDefinition = "TEXT")
    private String legalGuidance;

    @Column(columnDefinition = "TEXT")
    private String penalties;

    private String responsible;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDate deadline;
    private boolean recurrence;

    public enum Priority {
        BAIXA, MEDIA, ALTA
    }
}