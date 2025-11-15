package com.gotree.API.repositories;

import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório responsável por gerenciar operações de banco de dados para entidades AepReport.
 * Estende JpaRepository para herdar operações CRUD básicas.
 */
public interface AepReportRepository extends JpaRepository<AepReport, Long> {

    /**
     * Busca todos os relatórios AEP associados a um avaliador específico.
     *
     * @param evaluator O usuário avaliador para filtrar os relatórios
     * @return Lista de relatórios AEP do avaliador
     */
    List<AepReport> findAllByEvaluator(User evaluator);

    /**
     * Conta o número total de relatórios AEP feitos por um avaliador.
     * Utilizado para cálculos de KPIs do usuário.
     *
     * @param evaluator O usuário avaliador para contagem
     * @return Total de relatórios do avaliador
     */
    long countByEvaluator(User evaluator);

    /**
     * Conta o número de relatórios AEP feitos por um avaliador para uma empresa específica.
     * Utilizado para cálculos de KPIs administrativos.
     *
     * @param evaluator O usuário avaliador para contagem
     * @param companyId ID da empresa para filtrar
     * @return Total de relatórios do avaliador para a empresa
     */
    long countByEvaluatorAndCompanyId(User evaluator, Long companyId);

    boolean existsByUnit_Id(Long unitId);
    boolean existsBySector_Id(Long sectorId);

    boolean existsByCompany_Id(Long companyId);

    boolean existsByEvaluator_Id(Long evaluatorId);
}
