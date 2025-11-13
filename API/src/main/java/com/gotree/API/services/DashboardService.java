package com.gotree.API.services;

import com.gotree.API.dto.dashboard.AdminStatsDTO;
import com.gotree.API.dto.dashboard.CompanyCountDTO;
import com.gotree.API.dto.dashboard.MyStatsDTO;
import com.gotree.API.dto.dashboard.UserDocumentStatsDTO;
import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.CompanyRepository;
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
    private final DocumentAggregationService documentAggregationService;
    private final UserService userService; // Para buscar usuários por ID

    public DashboardService(UserRepository userRepository, CompanyRepository companyRepository,
                            TechnicalVisitRepository technicalVisitRepository, AepReportRepository aepReportRepository,
                            DocumentAggregationService documentAggregationService, UserService userService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.aepReportRepository = aepReportRepository;
        this.documentAggregationService = documentAggregationService;
        this.userService = userService;
    }

    /**
     * Retorna os KPIs do usuário logado.
     */
    @Transactional(readOnly = true)
    public MyStatsDTO getMyStats(User user) {
        long totalVisits = technicalVisitRepository.countByTechnician(user);
        long totalAeps = aepReportRepository.countByEvaluator(user);

        // 1. Calcula os segundos totais
        long totalSeconds = technicalVisitRepository.findTotalVisitDurationInSeconds(user.getId());
        // 2. Converte para o total de minutos
        long totalMinutes = totalSeconds / 60;
        // 3. Calcula as horas
        long hours = totalMinutes / 60;
        // 4. Calcula os minutos restantes
        long remainingMinutes = totalMinutes % 60;
        // --- FIM DA MODIFICAÇÃO ---

        // Calcula o Top 5 de Empresas (processando em Java)
        List<DocumentSummaryDTO> myDocuments = documentAggregationService.findAllDocumentsForUser(user);

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
        long totalDocuments = totalVisits + totalAeps;

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

        // 1. Define a lista de usuários a processar (ou todos, ou um específico)
        List<User> usersToProcess = (userId != null)
                ? List.of(userService.findById(userId))
                : userService.findAll();

        boolean checkVisits = (type == null || "VISIT".equalsIgnoreCase(type));
        boolean checkAeps = (type == null || "AEP".equalsIgnoreCase(type));

        return usersToProcess.stream().map(user -> {
            UserDocumentStatsDTO stats = new UserDocumentStatsDTO(user.getId(), user.getName());

            long visitCount = 0;
            long aepCount = 0;

            // Lógica de contagem baseada nos filtros
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

            stats.setTotalVisits(visitCount);
            stats.setTotalAeps(aepCount);
            stats.setTotalDocuments(visitCount + aepCount);
            return stats;
        }).collect(Collectors.toList());
    }
}