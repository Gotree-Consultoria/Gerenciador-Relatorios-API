package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.InspectionReport;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.InspectionReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentAggregationService {

    private final InspectionReportRepository inspectionReportRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final InspectionReportService inspectionReportService;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;
    private final AepReportRepository aepReportRepository;

    @Value("${file.storage.path}") // Injete o caminho base aqui também
    private String fileStoragePath;

    public DocumentAggregationService(InspectionReportRepository inspectionReportRepository,
                                      TechnicalVisitRepository technicalVisitRepository,
                                      InspectionReportService inspectionReportService,
                                      TechnicalVisitService technicalVisitService,
                                      AepService aepService, AepReportRepository aepReportRepository) {
        this.inspectionReportRepository = inspectionReportRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.inspectionReportService = inspectionReportService;
        this.technicalVisitService = technicalVisitService;
        this.aepService = aepService;
        this.aepReportRepository = aepReportRepository;
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsForUser(User technician) {
        // Bloco 1: Checklists de Inspeção
        List<DocumentSummaryDTO> inspectionReports = inspectionReportRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(report -> {
                    DocumentSummaryDTO dto = new DocumentSummaryDTO();
                    dto.setId(report.getId());

                    // Define o nome com base no nome do arquivo PDF
                    String docType = "Checklist de Inspeção"; // Nome padrão
                    if (report.getPdfPath() != null && report.getPdfPath().contains("nrs-checklist-template")) {
                        docType = "Checklist de Inspeção NR";
                    }
                    dto.setDocumentType(docType);

                    dto.setTitle(report.getTitle());
                    dto.setClientName(report.getCompany() != null ? report.getCompany().getName() : "N/A");
                    dto.setCreationDate(report.getInspectionDate());
                    return dto; // CORREÇÃO: Adicionado o 'return'
                })
                .toList();

        // Bloco 2: Relatórios de Visita Técnica
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(visit -> {
                    DocumentSummaryDTO dto = new DocumentSummaryDTO();
                    dto.setId(visit.getId());
                    dto.setDocumentType("Relatório de Visita");
                    dto.setTitle(visit.getTitle());
                    dto.setClientName(visit.getClientCompany() != null ? visit.getClientCompany().getName() : "N/A");
                    dto.setCreationDate(visit.getVisitDate());
                    return dto; // CORREÇÃO: Adicionado o 'return'
                })
                .toList();

        // Bloco 3: Relatório AEP
        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAllByEvaluator(technician) // (Você precisará criar este método no AepReportRepository)
                .stream()
                .map(aep -> {
                    DocumentSummaryDTO dto = new DocumentSummaryDTO();
                    dto.setId(aep.getId());
                    dto.setDocumentType("Avaliação Ergonômica Preliminar");
                    dto.setTitle(aep.getEvaluatedFunction()); // Usa a "Função Avaliada" como título
                    dto.setClientName(aep.getCompany() != null ? aep.getCompany().getName() : "N/A");
                    dto.setCreationDate(aep.getEvaluationDate());
                    return dto;
                })
                .toList();

        // 3. Junta todas as listas numa só
        List<DocumentSummaryDTO> allDocuments = Stream.of(inspectionReports, technicalVisits, aepReports)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 4. Ordena a lista final pela data de criação, dos mais recentes para os mais antigos
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate).reversed());

        return allDocuments;
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {
        List<DocumentSummaryDTO> allDocuments = findAllDocumentsForUser(technician);

        // Retorna apenas os 5 primeiros da lista já ordenada
        return allDocuments.stream().limit(5).collect(Collectors.toList());
    }

    public byte[] loadPdfFileByTypeAndId(String type, Long id, User currentUser) throws IOException {
        System.out.println("--- DEBUG [Agregação]: Roteando para tipo: " + type); // DEBUG

        String fileName = null;
        byte[] pdfBytes = null;

        if ("checklist".equalsIgnoreCase(type)) {
            InspectionReport report = inspectionReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Checklist com ID " + id + " não encontrado."));
            fileName = report.getPdfPath();

        } else if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + id + " não encontrado."));
            fileName = visit.getPdfPath();

        } else if ("aep".equalsIgnoreCase(type)) {
            System.out.println("--- DEBUG [Agregação]: Chamando AepService para ID: " + id); // DEBUG
            pdfBytes = aepService.loadOrGenerateAepPdf(id, currentUser);
            System.out.println("--- DEBUG [Agregação]: Retornou do AepService. Bytes: " + (pdfBytes != null ? pdfBytes.length : "null")); // DEBUG
            // fileName permanece nulo, o que está correto

        } else {
            throw new IllegalArgumentException("Tipo de documento inválido: " + type);
        }

        // Se os bytes foram gerados (é um AEP), retorne imediatamente.
        if (pdfBytes != null) {
            System.out.println("--- DEBUG [Agregação]: Retornando bytes do AEP imediatamente."); // DEBUG
            return pdfBytes;
        }

        // Se o código chegou aqui, é um "checklist" ou "visit".
        // Precisamos garantir que o fileName não seja nulo ANTES de usá-lo.
        if (fileName == null || fileName.isBlank()) {
            System.err.println("--- ERRO DEBUG [Agregação]: fileName está nulo ou em branco para o tipo: " + type); // DEBUG
            throw new RuntimeException("Este documento não possui um PDF associado.");
        }

        // Agora, este código só é executado se tivermos um fileName válido.
        Path path;
        if ("checklist".equalsIgnoreCase(type)) {
            // O InspectionReportService salva o CAMINHO COMPLETO
            path = Paths.get(fileName);
        } else if ("visit".equalsIgnoreCase(type)) {
            // O TechnicalVisitService salva SÓ O NOME DO ARQUIVO
            path = Paths.get(fileStoragePath, fileName);
        } else {
            // Segurança: caso um tipo não mapeado chegue aqui
            throw new IOException("Lógica de caminho de PDF não definida para o tipo: " + type);
        }

        if (!Files.exists(path)) {
            throw new IOException("Arquivo PDF não encontrado no caminho: " + path);
        }
        return Files.readAllBytes(path);
    }

    @Transactional
    public void deleteDocumentByTypeAndId(String type, Long id, User currentUser) {
        // Usamos um 'if/else if' para ir ao serviço correto, sem ambiguidade
        if ("checklist".equalsIgnoreCase(type)) {
            // A lógica de deleção (incluindo verificação de segurança) já está no serviço
            inspectionReportService.deleteReport(id, currentUser);

        } else if ("visit".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço de visita técnica
            technicalVisitService.deleteVisit(id, currentUser);

        } else if ("aep".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço da AEP
            aepService.deleteAepReport(id, currentUser);

        } else {
            // Se o tipo for desconhecido, lançamos um erro
            throw new IllegalArgumentException("Tipo de documento inválido para deleção: " + type);
        }
    }
}