package com.gotree.API.services;

import com.gotree.API.entities.Sector;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final OccupationalRiskReportRepository riskReportRepository;
    private final AepReportRepository aepReportRepository;

    public SectorService(SectorRepository sectorRepository,
                         OccupationalRiskReportRepository riskReportRepository,
                         AepReportRepository aepReportRepository) {
        this.sectorRepository = sectorRepository;
        this.riskReportRepository = riskReportRepository;
        this.aepReportRepository = aepReportRepository;
    }

    @Transactional
    public void deleteSector(Long sectorId) {
        if (!sectorRepository.existsById(sectorId)) {
            throw new RuntimeException("Setor com ID " + sectorId + " não encontrado.");
        }

        if (riskReportRepository.existsBySector_Id(sectorId)) {
            throw new IllegalStateException("Este setor não pode ser excluído, pois está sendo usado em Checklists de Risco.");
        }

        if (aepReportRepository.existsBySector_Id(sectorId)) {
            throw new IllegalStateException("Este setor não pode ser excluído, pois está sendo usada em relatórios AEP.");
        }

        sectorRepository.deleteById(sectorId);
    }
}