package com.gotree.API.repositories;

import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AepReportRepository extends JpaRepository<AepReport, Long> {

    List<AepReport> findAllByEvaluator(User evaluator);

    // (NOVO) Para KPIs do Usuário
    long countByEvaluator(User evaluator);

    // (NOVO) Para KPIs do Admin
    long countByEvaluatorAndCompanyId(User evaluator, Long companyId);
}
