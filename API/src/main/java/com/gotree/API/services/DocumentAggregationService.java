package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.dto.document.FileDownloadDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentAggregationService {

    private final TechnicalVisitRepository technicalVisitRepository;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;
    private final AepReportRepository aepReportRepository;
    private final RiskChecklistService riskChecklistService;
    private final OccupationalRiskReportRepository riskReportRepository;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public DocumentAggregationService(TechnicalVisitRepository technicalVisitRepository,
                                      TechnicalVisitService technicalVisitService,
                                      AepService aepService, AepReportRepository aepReportRepository,
                                      RiskChecklistService riskChecklistService, OccupationalRiskReportRepository riskReportRepository) {
        this.technicalVisitRepository = technicalVisitRepository;
        this.technicalVisitService = technicalVisitService;
        this.aepService = aepService;
        this.aepReportRepository = aepReportRepository;
        this.riskChecklistService = riskChecklistService;
        this.riskReportRepository = riskReportRepository;
    }

    // ===================================================================================
    // 1. MÉTODOS PÚBLICOS (ENTRADA)
    // ===================================================================================

    /**
     * TÉCNICO: Recupera documentos COM filtros e paginação.
     */
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDTO> findAllDocumentsForUser(
            User technician,
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        // 1. Busca dados específicos do técnico
        List<DocumentSummaryDTO> rawDocs = fetchRawDocumentsByTechnician(technician);

        // 2. Aplica a lógica comum de filtro/paginação
        return processDocumentList(rawDocs, typeFilter, clientFilter, startDate, endDate, pageable);
    }

    /**
     * ADMIN: Recupera TODOS os documentos do sistema, com filtros e paginação.
     */
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDTO> findAllDocumentsGlobal(
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        // 1. Busca dados globais
        List<DocumentSummaryDTO> rawDocs = fetchRawDocumentsGlobal();

        // 2. Aplica a MESMA lógica comum
        return processDocumentList(rawDocs, typeFilter, clientFilter, startDate, endDate, pageable);
    }

    /**
     * DASHBOARD: Retorna a LISTA COMPLETA ordenada (sem paginação).
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsListForUser(User technician) {
        List<DocumentSummaryDTO> docs = fetchRawDocumentsByTechnician(technician);
        // Apenas ordena
        docs.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return docs;
    }

    /**
     * WIDGET: Recupera os 5 mais recentes.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {
        List<DocumentSummaryDTO> docs = fetchRawDocumentsByTechnician(technician);
        docs.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return docs.stream().limit(5).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllLatestDocumentsForAdmin() {
        List<DocumentSummaryDTO> docs = fetchRawDocumentsGlobal();
        docs.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return docs.stream().limit(5).collect(Collectors.toList());
    }

    // ===================================================================================
    // 2. FETCHERS (Busca de Dados Brutos)
    // ===================================================================================

    private List<DocumentSummaryDTO> fetchRawDocumentsByTechnician(User technician) {
        List<DocumentSummaryDTO> visits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream().map(this::mapVisitToSummaryDto).toList();

        List<DocumentSummaryDTO> aeps = aepReportRepository.findAllByEvaluator(technician)
                .stream().map(this::mapAepToSummaryDto).toList();

        List<DocumentSummaryDTO> risks = riskReportRepository.findByTechnicianOrderByInspectionDateDesc(technician)
                .stream().map(this::mapRiskToSummaryDto).toList();

        return Stream.of(visits, aeps, risks)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<DocumentSummaryDTO> fetchRawDocumentsGlobal() {
        List<DocumentSummaryDTO> visits = technicalVisitRepository.findAll()
                .stream().map(this::mapVisitToSummaryDto).toList();

        List<DocumentSummaryDTO> aeps = aepReportRepository.findAll()
                .stream().map(this::mapAepToSummaryDto).toList();

        List<DocumentSummaryDTO> risks = riskReportRepository.findAll()
                .stream().map(this::mapRiskToSummaryDto).toList();

        return Stream.of(visits, aeps, risks)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    // ===================================================================================
    // 3. PROCESSOR (Lógica Centralizada de Filtro e Paginação)
    // ===================================================================================

    private Page<DocumentSummaryDTO> processDocumentList(
            List<DocumentSummaryDTO> allDocuments,
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        Stream<DocumentSummaryDTO> stream = allDocuments.stream();

        // 1. Filtro por TIPO
        if (typeFilter != null && !typeFilter.isBlank()) {
            final String typeInput = typeFilter.trim();
            stream = stream.filter(doc -> {
                if ("visit".equalsIgnoreCase(typeInput)) return "Relatório de Visita".equals(doc.getDocumentType());
                if ("aep".equalsIgnoreCase(typeInput)) return "Avaliação Ergonômica Preliminar".equals(doc.getDocumentType());
                if ("risk".equalsIgnoreCase(typeInput)) return "Checklist de Riscos".equals(doc.getDocumentType());
                return false;
            });
        }

        // 2. Filtro por NOME DO CLIENTE
        if (clientFilter != null && !clientFilter.isBlank()) {
            String filter = clientFilter.toLowerCase().trim();
            stream = stream.filter(doc ->
                    doc.getClientName() != null &&
                            doc.getClientName().toLowerCase().contains(filter)
            );
        }

        // 3. Filtro por DATA
        if (startDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null && !doc.getCreationDate().isBefore(startDate));
        }
        if (endDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null && !doc.getCreationDate().isAfter(endDate));
        }

        // 4. Coleta e Ordena
        List<DocumentSummaryDTO> filteredList = stream.collect(Collectors.toList());
        filteredList.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));

        // 5. Paginação Manual
        long totalElements = filteredList.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<DocumentSummaryDTO> paginatedList;

        if (startItem >= totalElements) {
            paginatedList = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, (int) totalElements);
            paginatedList = filteredList.subList(startItem, toIndex);
        }

        return new PageImpl<>(paginatedList, pageable, totalElements);
    }

    // ===================================================================================
    // 4. MÉTODOS DE ARQUIVO E DELEÇÃO (Mantidos iguais)
    // ===================================================================================

    public byte[] loadPdfFileByTypeAndId(String type, Long id, User currentUser) throws IOException {
        String fileName = null;
        byte[] pdfBytes = null;

        if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório de Visita não encontrado."));
            fileName = visit.getPdfPath();
        } else if ("aep".equalsIgnoreCase(type)) {
            pdfBytes = aepService.loadOrGenerateAepPdf(id, currentUser);
        } else if ("risk".equalsIgnoreCase(type)) {
            OccupationalRiskReport report = riskReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));
            fileName = report.getPdfPath();
        } else {
            throw new IllegalArgumentException("Tipo de documento inválido: " + type);
        }

        if (pdfBytes != null) return pdfBytes;
        if (fileName == null || fileName.isBlank()) throw new RuntimeException("Este documento não possui um PDF associado.");

        Path path = ("visit".equalsIgnoreCase(type)) ? Paths.get(fileStoragePath, fileName) : Paths.get(fileName);

        if (!Files.exists(path)) throw new IOException("Arquivo PDF não encontrado.");
        return Files.readAllBytes(path);
    }

    @Transactional
    public void deleteDocumentByTypeAndId(String type, Long id, User currentUser) {
        if ("visit".equalsIgnoreCase(type)) technicalVisitService.deleteVisit(id, currentUser);
        else if ("aep".equalsIgnoreCase(type)) aepService.deleteAepReport(id, currentUser);
        else if ("risk".equalsIgnoreCase(type)) riskChecklistService.deleteReport(id, currentUser);
        else throw new IllegalArgumentException("Tipo de documento inválido: " + type);
    }

    /**
     * Recupera o PDF e gera um nome amigável para download.
     */
    @Transactional(readOnly = true)
    public FileDownloadDTO downloadDocument(String type, Long id, User currentUser) throws IOException {
        String pdfPathOnDisk = null;
        byte[] pdfBytes = null;

        // Variáveis para montar o nome
        String docTypeLabel = "";
        String title = "";
        String companyName = "";
        LocalDate date = LocalDate.now();

        if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));

            pdfPathOnDisk = visit.getPdfPath();
            docTypeLabel = "Visita Tecnica";
            title = visit.getTitle();
            companyName = visit.getClientCompany().getName();
            date = visit.getVisitDate();

        } else if ("aep".equalsIgnoreCase(type)) {
            AepReport aep = aepReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("AEP não encontrada."));

            // AEP pode gerar em tempo real se não tiver path, assumindo lógica similar ao seu service
            if (aep.getPdfPath() == null) {
                pdfBytes = aepService.loadOrGenerateAepPdf(id, currentUser);
            } else {
                pdfPathOnDisk = aep.getPdfPath();
            }

            docTypeLabel = "AEP";
            title = aep.getEvaluatedFunction(); // Ou outro campo de título
            companyName = aep.getCompany().getName();
            date = aep.getEvaluationDate();

        } else if ("risk".equalsIgnoreCase(type)) {
            OccupationalRiskReport report = riskReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Checklist não encontrado."));

            pdfPathOnDisk = report.getPdfPath();
            docTypeLabel = "Checklist Risco";
            title = report.getTitle();
            companyName = report.getCompany().getName();
            date = report.getInspectionDate();
        }

        // 1. Carrega os bytes (se já não foram gerados em memória para AEP)
        if (pdfBytes == null) {
            if (pdfPathOnDisk == null) throw new RuntimeException("Arquivo não encontrado no servidor.");
            Path path = ("visit".equalsIgnoreCase(type)) ? Paths.get(fileStoragePath, pdfPathOnDisk) : Paths.get(pdfPathOnDisk);
            pdfBytes = Files.readAllBytes(path);
        }

        // 2. Sanitiza e Monta o Nome do Arquivo
        // Formato: TIPO - TITULO - EMPRESA - DD-MM-YYYY.pdf
        String safeTitle = sanitizeFilename(title);
        String safeCompany = sanitizeFilename(companyName);
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        String finalFilename = String.format("%s - %s - %s - %s.pdf",
                docTypeLabel, safeTitle, safeCompany, dateStr);

        return new FileDownloadDTO(finalFilename, pdfBytes);
    }

    // Remove caracteres especiais que quebram o download
    private String sanitizeFilename(String input) {
        if (input == null) return "SemNome";
        // Mantém apenas letras, números, espaços, traços e underscores
        return input.replaceAll("[^a-zA-Z0-9 \\-_\\.]", "").trim();
    }

    // ===================================================================================
    // 5. HELPERS DE MAPEAMENTO (Com E-mail e Nome do Técnico)
    // ===================================================================================

    public DocumentSummaryDTO mapVisitToSummaryDto(TechnicalVisit visit) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(visit.getId());
        dto.setDocumentType("Relatório de Visita");
        dto.setTitle(visit.getTitle());
        dto.setCreationDate(visit.getVisitDate());
        fillCommonFields(dto, visit.getClientCompany(), visit.getSentToClientAt(), visit.getTechnicianSignatureImageBase64(), visit.getTechnician());
        return dto;
    }

    public DocumentSummaryDTO mapAepToSummaryDto(AepReport aep) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(aep.getId());
        dto.setDocumentType("Avaliação Ergonômica Preliminar");
        dto.setTitle(aep.getEvaluatedFunction());
        dto.setCreationDate(aep.getEvaluationDate());
        fillCommonFields(dto, aep.getCompany(), aep.getSentToClientAt(), null, aep.getEvaluator());
        return dto;
    }

    public DocumentSummaryDTO mapRiskToSummaryDto(OccupationalRiskReport report) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(report.getId());
        dto.setDocumentType("Checklist de Riscos");
        dto.setTitle(report.getTitle());
        dto.setCreationDate(report.getInspectionDate());
        fillCommonFields(dto, report.getCompany(), report.getSentToClientAt(), report.getTechnicianSignatureImageBase64(), report.getTechnician());
        return dto;
    }

    private void fillCommonFields(DocumentSummaryDTO dto, Company company, LocalDateTime sentAt, String signatureBase64, User technician) {
        if (company != null) {
            dto.setClientName(company.getName());
            if (company.getClient() != null) {
                dto.setClientEmail(company.getClient().getEmail());
            } else {
                dto.setClientEmail(null);
            }
        } else {
            dto.setClientName("N/A");
            dto.setClientEmail(null);
        }

        if (technician != null) {
            dto.setTechnicianName(technician.getName());
        }

        dto.setEmailSent(sentAt != null);
        dto.setSigned(signatureBase64 != null && !signatureBase64.isBlank());
    }
}