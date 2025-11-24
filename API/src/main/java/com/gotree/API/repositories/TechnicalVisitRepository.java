package com.gotree.API.repositories;

import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Interface de repositório para gerenciamento de Visitas Técnicas.
 * Fornece métodos para busca, contagem e cálculo de duração das visitas.
 */
public interface TechnicalVisitRepository extends JpaRepository<TechnicalVisit, Long> {


    /**
     * Busca todas as visitas de um técnico, ordenadas por data decrescente.
     *
     * @param technician Técnico responsável pelas visitas
     * @return Lista de visitas técnicas
     */
    List<TechnicalVisit> findByTechnicianOrderByVisitDateDesc(User technician);

    /**
     * Busca todas as visitas de um técnico incluindo dados da empresa cliente,
     * ordenadas por data decrescente.
     *
     * @param technician Técnico responsável pelas visitas
     * @return Lista de visitas técnicas com dados da empresa
     */
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

    /**
     * Conta o total de visitas realizadas por um técnico.
     * Utilizado para KPIs do usuário.
     *
     * @param technician Técnico a ser consultado
     * @return Total de visitas do técnico
     */
    long countByTechnician(User technician);

    /**
     * Conta o total de visitas realizadas por um técnico em uma empresa específica.
     * Utilizado para KPIs do administrador.
     *
     * @param technician Técnico a ser consultado
     * @param companyId  ID da empresa
     * @return Total de visitas do técnico na empresa
     */
    long countByTechnicianAndClientCompanyId(User technician, Long companyId);

    /**
     * Calcula o tempo total em segundos de todas as visitas de um usuário.
     * Utiliza query nativa para extração do intervalo de tempo em segundos.
     * @param userId ID do usuário
     * @return Tempo total em segundos
     */
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (t.end_time - t.start_time))), 0) " +
            "FROM tb_technical_visit t WHERE t.technician_id = :userId AND t.end_time IS NOT NULL",
            nativeQuery = true)
    long findTotalVisitDurationInSeconds(@Param("userId") Long userId);

    /**
     * Calcula o tempo total em segundos de todas as visitas no sistema.
     * Utiliza query nativa para extração do intervalo de tempo em segundos.
     *
     * @return Tempo total em segundos de todas as visitas
     */
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (t.end_time - t.start_time))), 0) " +
            "FROM tb_technical_visit t WHERE t.end_time IS NOT NULL",
            nativeQuery = true)
    long findTotalVisitDurationInSeconds();

    boolean existsByClientCompany_Id(Long companyId);

    boolean existsByTechnician_Id(Long technicianId);

    boolean existsBySector_Id(Long sectorId);
}