package com.gotree.API.repositories;

import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TechnicalVisitRepository extends JpaRepository<TechnicalVisit, Long> {

    List<TechnicalVisit> findByTechnicianOrderByVisitDateDesc(User technician);

    @Query("SELECT v FROM TechnicalVisit v LEFT JOIN FETCH v.clientCompany WHERE v.technician = :technician ORDER BY v.visitDate DESC")
    List<TechnicalVisit> findAllWithCompanyByTechnician(@Param("technician") User technician);

    /**
     * Encontra todas as visitas pelo usuário autenticado que têm uma "próxima visita" agendada,
     * já trazendo a Empresa (clientCompany), a Unidade (unit) e o Setor (sector).
     */
    @Query("SELECT v FROM TechnicalVisit v " +
            "LEFT JOIN FETCH v.clientCompany " +
            "LEFT JOIN FETCH v.unit " +
            "LEFT JOIN FETCH v.sector " +
            "WHERE v.technician = :technician AND v.nextVisitDate IS NOT NULL " +
            "ORDER BY v.nextVisitDate ASC")
    List<TechnicalVisit> findAllScheduledWithCompanyByTechnician(@Param("technician") User technician);

    /**
     * Encontra TODAS as visitas de TODOS os usuários que têm
     * uma "próxima visita" agendada.
     */
    @Query("SELECT v FROM TechnicalVisit v " +
            "LEFT JOIN FETCH v.clientCompany " +
            "LEFT JOIN FETCH v.unit " +
            "LEFT JOIN FETCH v.sector " +
            "LEFT JOIN FETCH v.technician " +
            "WHERE v.nextVisitDate IS NOT NULL " +
            "ORDER BY v.nextVisitDate ASC")
    List<TechnicalVisit> findAllScheduledWithCompany();

    // (NOVO) Para KPIs do Usuário
    long countByTechnician(User technician);

    // (NOVO) Para KPIs do Admin
    long countByTechnicianAndClientCompanyId(User technician, Long companyId);

    // (NOVO) Calcula o tempo total em segundos para um usuário
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (t.end_time - t.start_time))), 0) " +
            "FROM tb_technical_visit t WHERE t.technician_id = :userId AND t.end_time IS NOT NULL",
            nativeQuery = true) // <-- Adicionado
    long findTotalVisitDurationInSeconds(@Param("userId") Long userId); // <-- Mudado de User para Long

    // (NOVO) Calcula o tempo total em segundos para todos
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (t.end_time - t.start_time))), 0) " +
            "FROM tb_technical_visit t WHERE t.end_time IS NOT NULL",
            nativeQuery = true) // <-- Adicionado
    long findTotalVisitDurationInSeconds();
}