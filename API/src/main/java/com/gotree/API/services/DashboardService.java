package com.gotree.API.services;

import com.gotree.API.dto.dashboard.AdminStatsDTO;
import com.gotree.API.dto.dashboard.CompanyCountDTO;
import com.gotree.API.dto.dashboard.MyStatsDTO;
import com.gotree.API.dto.dashboard.UserDocumentStatsDTO;
import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.CompanyRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final AepReportRepository aepReportRepository;
    // (CORREÇÃO 1: Nome da variável em camelCase)
    private final OccupationalRiskReportRepository occupationalRiskReportRepository;
    private final DocumentAggregationService documentAggregationService;
    private final UserService userService;

    public DashboardService(UserRepository userRepository, CompanyRepository companyRepository,
                            TechnicalVisitRepository technicalVisitRepository, AepReportRepository aepReportRepository,
                            DocumentAggregationService documentAggregationService, UserService userService,
                            OccupationalRiskReportRepository occupationalRiskReportRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.aepReportRepository = aepReportRepository;
        this.documentAggregationService = documentAggregationService;
        this.userService = userService;
        this.occupationalRiskReportRepository = occupationalRiskReportRepository;
    }

    /**
     * Retorna os KPIs do usuário logado.
     */
    @Transactional(readOnly = true)
    public MyStatsDTO getMyStats(User user) {
        long totalVisits = technicalVisitRepository.countByTechnician(user);
        long totalAeps = aepReportRepository.countByEvaluator(user);

        // (CORREÇÃO 2: Adicionado contagem de Riscos para o usuário)
        long totalRisks = occupationalRiskReportRepository.countByTechnician(user);

        // Calcula o tempo (Lógica de horas/minutos)
        long totalSeconds = technicalVisitRepository.findTotalVisitDurationInSeconds(user.getId());
        long totalMinutes = totalSeconds / 60;
        long hours = totalMinutes / 60;
        long remainingMinutes = totalMinutes % 60;

        // Calcula o Top 5 de Empresas
        List<DocumentSummaryDTO> myDocuments = documentAggregationService.findAllDocumentsListForUser(user);

        Map<String, Long> companyCounts = myDocuments.stream()
                .filter(doc -> doc.getClientName() != null && !doc.getClientName().equals("N/A"))
                .collect(Collectors.groupingBy(DocumentSummaryDTO::getClientName, Collectors.counting()));

        List<CompanyCountDTO> topCompanies = companyCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new CompanyCountDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        MyStatsDTO stats = new MyStatsDTO();
        stats.setTotalVisits(totalVisits);
        stats.setTotalAeps(totalAeps);
        stats.setTotalRisks(totalRisks);

        stats.setTotalVisitTimeHours(hours);
        stats.setTotalVisitTimeMinutes(remainingMinutes);
        stats.setTopCompanies(topCompanies);

        return stats;
    }

    /**
     * Retorna os KPIs globais para o Admin.
     */
    @Transactional(readOnly = true)
    public AdminStatsDTO getAdminStats() {
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();

        long totalVisits = technicalVisitRepository.count();
        long totalAeps = aepReportRepository.count();
        long totalRisks = occupationalRiskReportRepository.count();

        long totalDocuments = totalVisits + totalAeps + totalRisks;

        long totalSeconds = technicalVisitRepository.findTotalVisitDurationInSeconds();
        long totalMinutes = totalSeconds / 60;
        long hours = totalMinutes / 60;
        long remainingMinutes = totalMinutes % 60;

        AdminStatsDTO stats = new AdminStatsDTO();
        stats.setTotalUsers(totalUsers);
        stats.setTotalCompanies(totalCompanies);
        stats.setTotalDocuments(totalDocuments);
        stats.setTotalVisitTimeHours(hours);
        stats.setTotalVisitTimeMinutes(remainingMinutes);

        return stats;
    }

    /**
     * Retorna a contagem de documentos por usuário, com filtros.
     */
    @Transactional(readOnly = true)
    public List<UserDocumentStatsDTO> getAdminDocumentStats(Long userId, String type, Long companyId) {

        List<User> usersToProcess = (userId != null)
                ? List.of(userService.findById(userId))
                : userService.findAll();

        boolean checkVisits = (type == null || "VISIT".equalsIgnoreCase(type));
        boolean checkAeps = (type == null || "AEP".equalsIgnoreCase(type));
        boolean checkRisks = (type == null || "RISK".equalsIgnoreCase(type));

        return usersToProcess.stream().map(user -> {
            UserDocumentStatsDTO stats = new UserDocumentStatsDTO(user.getId(), user.getName());

            long visitCount = 0;
            long aepCount = 0;
            long riskCount = 0;

            if (checkVisits) {
                visitCount = (companyId != null)
                        ? technicalVisitRepository.countByTechnicianAndClientCompanyId(user, companyId)
                        : technicalVisitRepository.countByTechnician(user);
            }
            if (checkAeps) {
                aepCount = (companyId != null)
                        ? aepReportRepository.countByEvaluatorAndCompanyId(user, companyId)
                        : aepReportRepository.countByEvaluator(user);
            }

            if (checkRisks) {
                // (CORREÇÃO 4: Lógica do Ternário INVERTIDA estava errada, corrigido aqui)
                riskCount = (companyId != null)
                        ? occupationalRiskReportRepository.countByTechnicianAndCompanyId(user, companyId) // Com Filtro
                        : occupationalRiskReportRepository.countByTechnician(user); // Sem Filtro
            }

            stats.setTotalVisits(visitCount);
            stats.setTotalAeps(aepCount);
            stats.setTotalRisks(riskCount); // <-- Certifique-se que UserDocumentStatsDTO tem este campo
            stats.setTotalDocuments(visitCount + aepCount + riskCount);
            return stats;
        }).collect(Collectors.toList());
    }
}