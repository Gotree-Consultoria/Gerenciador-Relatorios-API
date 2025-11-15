package com.gotree.API.services;

import com.gotree.API.entities.Unit;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitService {

    private final UnitRepository unitRepository;
    private final OccupationalRiskReportRepository riskReportRepository;
    private final AepReportRepository aepReportRepository;

    public UnitService(UnitRepository unitRepository,
                       OccupationalRiskReportRepository riskReportRepository,
                       AepReportRepository aepReportRepository) {
        this.unitRepository = unitRepository;
        this.riskReportRepository = riskReportRepository;
        this.aepReportRepository = aepReportRepository;
    }

    @Transactional
    public void deleteUnit(Long unitId) {
        // 1. Verifica se a unidade existe
        if (!unitRepository.existsById(unitId)) {
            throw new RuntimeException("Unidade com ID " + unitId + " não encontrada.");
        }

        // 2. APLICA A SUA REGRA DE NEGÓCIO
        if (riskReportRepository.existsByUnit_Id(unitId)) {
            throw new IllegalStateException("Esta unidade não pode ser excluída, pois está sendo usada em Checklists de Risco.");
        }

        if (aepReportRepository.existsByUnit_Id(unitId)) {
            throw new IllegalStateException("Esta unidade não pode ser excluída, pois está sendo usada em relatórios AEP.");
        }

        // 3. Se passou em todas as verificações, exclui
        unitRepository.deleteById(unitId);
    }
}