package com.gotree.API.entities;

import com.gotree.API.entities.enums.NrsCheckStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_nrs_section") // <-- Nova tabela
@Data
public class NrsSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // Ex: "Higiene e Limpeza (NR 24...)"

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status")
    private NrsCheckStatus summaryStatus; // Armazena C, NC ou NA para a seção inteira

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id") // <-- Link para o InspectionReport principal
    private InspectionReport report;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NrsItem> items = new ArrayList<>(); // <-- Link para os novos NrsItem
}