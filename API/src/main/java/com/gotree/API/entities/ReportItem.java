package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "tb_report_item")
@Data
@EqualsAndHashCode(of = "id")
public class ReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1024) // Aumenta o tamanho da coluna para descrições longas
    private String description;

    private boolean isChecked;

    @Column(name = "is_na")
    private boolean isNa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id") // O item agora se junta com a seção
    private ReportSection section;

}