package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidade que representa uma Avaliação Ergonômica Preliminar (AEP) salva no banco de dados.
 */
@Entity
@Table(name = "tb_aep_report")
@Data
public class AepReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Relacionamentos de Contexto (Vindos do DTO) ---
    // Ligação com a empresa cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // --- Dados do Avaliador ---
    // Ligação com o técnico que está logado e preenchendo o relatório
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_user_id")
    private User evaluator;

    // --- Dados da Avaliação (Vindos do DTO) ---
    @Column(name = "evaluation_date")
    private LocalDate evaluationDate;

    @Column(name = "evaluated_function")
    private String evaluatedFunction;

    // --- Armazenamento do PDF Gerado ---
    // Armazena o caminho do arquivo PDF após ser gerado
    @Column(name = "pdf_path")
    private String pdfPath;

    // --- Lista de Riscos Selecionados (Vindos do DTO) ---
    /**
     * Armazena a lista de riscos que o usuário selecionou no formulário.
     * O JPA criará uma tabela separada (tb_aep_selected_risks)
     * para ligar o ID do relatório aos textos dos riscos selecionados.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tb_aep_selected_risks", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "risk_description", length = 1024) // Coluna para o texto do risco
    private List<String> selectedRisks;

    // --- Informações da Fisioterapeuta ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physiotherapist_id")
   private Physiotherapist physiotherapist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;
}