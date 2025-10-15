package com.gotree.API.entities;

import com.gotree.API.entities.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tb_inspection_report")
@Data
@EqualsAndHashCode(of = "id")
public class InspectionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Campos para Busca e Organização ---
    @Enumerated(EnumType.STRING)
    private DocumentType type;
    private String title;
    private String pdfPath;
    private boolean digitallySigned = false; // Flag para assinatura digital avançada

    // --- Relacionamentos de Contexto ---
    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    private Sector sector;

    // --- Dados do Formulário de Inspeção ---
    private LocalDate inspectionDate;
    private String local;
    @Lob
    private String notes;
    @Lob
    private String observations;
    private String responsavelSigla;
    private String responsavelRegistro;

    // --- Dados da Assinatura do Técnico ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_user_id")
    private User technician; // O usuário logado que realizou a inspeção

    @Column(columnDefinition = "TEXT")
    private String technicianSignatureImageBase64; // A imagem da assinatura
    private LocalDateTime technicianSignedAt;
    @Column(unique = true)
    private String technicianSignatureToken;

    // --- Dados da Assinatura do Cliente ---
    private String clientSignerName; // Nome digitado pelo cliente

    @Column(columnDefinition = "TEXT")
    private String clientSignatureImageBase64; // A imagem da assinatura
    private LocalDateTime clientSignedAt;
    private Double clientSignatureLatitude;
    private Double clientSignatureLongitude;

    // --- Relação com os Itens do Checklist ---
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportSection> sections = new ArrayList<>();
}