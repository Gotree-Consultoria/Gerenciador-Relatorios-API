package com.gotree.API.repositories;

import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OccupationalRiskReportRepository extends JpaRepository<OccupationalRiskReport, Long> {

    // Busca relatórios pelo técnico (para listar no dashboard)
    List<OccupationalRiskReport> findByTechnicianOrderByInspectionDateDesc(User technician);

    // (NOVO) Para KPIs do Usuário (Total de relatórios feitos pelo técnico)
    long countByTechnician(User technician);

    // (NOVO) Para KPIs do Admin (Contagem filtrada por técnico e empresa)
    long countByTechnicianAndCompanyId(User technician, Long companyId);

    boolean existsByUnit_Id(Long unitId);
    boolean existsBySector_Id(Long sectorId);

    boolean existsByCompany_Id(Long companyId);

    boolean existsByTechnician_Id(Long technicianId);
}