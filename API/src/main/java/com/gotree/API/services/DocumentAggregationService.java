package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * Serviço responsável por agregar e gerenciar diferentes tipos de documentos no sistema,
 * incluindo Relatórios de Visita Técnica, Avaliações Ergonômicas Preliminares (AEP)
 * e Relatórios de Riscos Ocupacionais (Checklists).
 * Fornece funcionalidades para buscar, carregar, filtrar e deletar documentos.
 */
@Service
public class DocumentAggregationService {

    private final TechnicalVisitRepository technicalVisitRepository;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;
    private final AepReportRepository aepReportRepository;
    private final RiskChecklistService riskChecklistService;
    private final OccupationalRiskReportRepository riskReportRepository;

    @Value("${file.storage.path}") // Injete o caminho base aqui também
    private String fileStoragePath;

    /**
     * Construtor do serviço de agregação de documentos.
     *
     * @param technicalVisitRepository Repositório de visitas técnicas
     * @param technicalVisitService    Serviço de visitas técnicas
     * @param aepService               Serviço de AEP
     * @param aepReportRepository      Repositório de relatórios AEP
     * @param riskChecklistService     Serviço de checklist de riscos
     * @param riskReportRepository     Repositório de relatórios de riscos ocupacionais
     */
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

    /**
     * Recupera todos os documentos associados a um técnico, com filtros e paginação.
     * ATENÇÃO: Esta implementação faz paginação EM MEMÓRIA.
     */
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDTO> findAllDocumentsForUser(
            User technician,
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        // 1. Relatórios de Visita Técnica (igual)
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(this::mapVisitToSummaryDto)
                .toList();

        // 2. Relatórios AEP (igual)
        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAllByEvaluator(technician)
                .stream()
                .map(this::mapAepToSummaryDto)
                .toList();

        // 3. Checklist de Riscos (igual)
        List<DocumentSummaryDTO> riskReports = riskReportRepository.findByTechnicianOrderByInspectionDateDesc(technician)
                .stream()
                .map(this::mapRiskToSummaryDto)
                .toList();

        Stream<DocumentSummaryDTO> stream = Stream.of(technicalVisits, aepReports, riskReports)
                .flatMap(List::stream);

        // 4. --- APLICA OS FILTROS ---

        // Filtro por TIPO (igual)
        if (typeFilter != null && !typeFilter.isBlank()) {
            final String typeInput = typeFilter.trim();
            stream = stream.filter(doc -> {
                if ("visit".equalsIgnoreCase(typeInput)) return "Relatório de Visita".equals(doc.getDocumentType());
                if ("aep".equalsIgnoreCase(typeInput)) return "Avaliação Ergonômica Preliminar".equals(doc.getDocumentType());
                if ("risk".equalsIgnoreCase(typeInput)) return "Checklist de Riscos".equals(doc.getDocumentType());
                return false; // Correção da lógica anterior
            });
        }

        // Filtro por NOME DO CLIENTE (igual)
        if (clientFilter != null && !clientFilter.isBlank()) {
            String filter = clientFilter.toLowerCase().trim();
            stream = stream.filter(doc ->
                    doc.getClientName() != null &&
                            doc.getClientName().toLowerCase().contains(filter)
            );
        }

        // --- NOVO: FILTRO POR DATA ---
        if (startDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null &&
                    !doc.getCreationDate().isBefore(startDate));
        }
        if (endDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null &&
                    !doc.getCreationDate().isAfter(endDate));
        }

        // 5. Coleta e Ordena
        List<DocumentSummaryDTO> filteredList = stream.collect(Collectors.toList());
        // A ordenação do Pageable é complexa de aplicar aqui,
        // então mantemos a ordenação padrão por data de criação.
        filteredList.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));

        // 6. --- NOVO: PAGINAÇÃO MANUAL (EM MEMÓRIA) ---

        // Calcula o total de elementos (antes de fatiar)
        long totalElements = filteredList.size();

        // Pega os dados da paginação (Ex: page=1, size=20)
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber(); // O Spring Pageable começa em 0

        // Calcula o índice inicial
        int startItem = currentPage * pageSize;

        List<DocumentSummaryDTO> paginatedList;

        if (startItem >= totalElements) {
            // Se o índice inicial for maior que a lista, retorna vazio
            paginatedList = Collections.emptyList();
        } else {
            // Calcula o índice final
            int toIndex = Math.min(startItem + pageSize, (int) totalElements);
            // "Fatia" a lista
            paginatedList = filteredList.subList(startItem, toIndex);
        }

        // 7. Retorna o objeto Page
        // O PageImpl é a implementação concreta de Page
        return new PageImpl<>(paginatedList, pageable, totalElements);
    }

    /**
     * Recupera os 5 documentos mais recentes associados a um técnico específico.
     * (CORRIGIDO PARA USAR PAGINAÇÃO)
     *
     * @param technician O usuário técnico para o qual os documentos serão buscados
     * @return Lista limitada a 5 DocumentSummaryDTO ordenados por data de criação
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {

        // 1. Criamos um "pedido" para a Página 0, com 5 itens de tamanho.
        Pageable pageRequest = PageRequest.of(0, 5);

        // 2. Chamamos o metodo paginado, passando null para os filtros
        // e o nosso "pedido" de página.
        Page<DocumentSummaryDTO> documentsPage = findAllDocumentsForUser(
                technician,
                null, // typeFilter
                null, // clientFilter
                null, // startDate
                null, // endDate
                pageRequest // pageable
        );

        // 3. O 'Page' já contém a lista dos 5 itens.
        // Usamos .getContent() para retornar apenas a lista.
        return documentsPage.getContent();
    }

    /**
     * Recupera os 5 documentos mais recentes do sistema para visualização administrativa.
     * Inclui todos os tipos de documentos sem restrição por usuário.
     *
     * @return Lista limitada a 5 DocumentSummaryDTO ordenados por data de criação decrescente
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllLatestDocumentsForAdmin() {
        // 1. Busca TODOS os relatórios
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAll()
                .stream()
                .map(this::mapVisitToSummaryDto)
                .toList();

        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAll()
                .stream()
                .map(this::mapAepToSummaryDto)
                .toList();

        List<DocumentSummaryDTO> riskReports = riskReportRepository.findAll()
                .stream()
                .map(this::mapRiskToSummaryDto)
                .toList();

        // 2. Junta as listas
        List<DocumentSummaryDTO> allDocuments = Stream.of(technicalVisits, aepReports, riskReports)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 3. Ordena e pega os 5 mais recentes
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return allDocuments.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * Retorna a LISTA COMPLETA de documentos para um usuário (sem paginação).
     * Usado por serviços internos como o Dashboard para calcular estatísticas.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsListForUser(User technician) {
        // 1. Busca todos os tipos
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(this::mapVisitToSummaryDto)
                .toList();

        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAllByEvaluator(technician)
                .stream()
                .map(this::mapAepToSummaryDto)
                .toList();

        List<DocumentSummaryDTO> riskReports = riskReportRepository.findByTechnicianOrderByInspectionDateDesc(technician)
                .stream()
                .map(this::mapRiskToSummaryDto)
                .toList();

        // 2. Junta as listas
        Stream<DocumentSummaryDTO> stream = Stream.of(technicalVisits, aepReports, riskReports)
                .flatMap(List::stream);

        // 3. Coleta e Retorna a lista completa
        List<DocumentSummaryDTO> allDocuments = stream.collect(Collectors.toList());

        // Ordena (o dashboard não precisa, mas é uma boa prática)
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));

        return allDocuments;
    }

    /**
     * Carrega o arquivo PDF de um documento específico com base em seu tipo e ID.
     *
     * @param type        Tipo do documento ("visit" ou "aep")
     * @param id          ID do documento
     * @param currentUser Usuário atual que está solicitando o documento
     * @return Array de bytes contendo o PDF do documento
     * @throws IOException              Se houver erro ao ler o arquivo
     * @throws RuntimeException         Se o documento não for encontrado
     * @throws IllegalArgumentException Se o tipo de documento for inválido
     */
    public byte[] loadPdfFileByTypeAndId(String type, Long id, User currentUser) throws IOException {

        String fileName = null;
        byte[] pdfBytes = null;

        if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + id + " não encontrado."));
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

        // Se os bytes foram gerados (é um AEP), retorne imediatamente.
        if (pdfBytes != null) {
            return pdfBytes;
        }

        // Se o código chegou aqui, é um "checklist" ou "visit".
        // Precisamos garantir que o fileName não seja nulo ANTES de usá-lo.
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Este documento não possui um PDF associado.");
        }

        // Este código só é executado se tivermos um fileName válido.
        Path path;
        if ("visit".equalsIgnoreCase(type)) {
            // O TechnicalVisitService salva SÓ O NOME DO ARQUIVO
            path = Paths.get(fileStoragePath, fileName);
        } else if ("risk".equalsIgnoreCase(type)) {
            // O RiskChecklistService salva o CAMINHO COMPLETO
            path = Paths.get(fileName);
        } else {
            // Segurança: caso um tipo não mapeado chegue aqui
            throw new IOException("Lógica de caminho de PDF não definida para o tipo: " + type);
        }

        if (!Files.exists(path)) {
            throw new IOException("Arquivo PDF não encontrado no caminho: " + path);
        }
        return Files.readAllBytes(path);
    }

    /**
     * Deleta um documento específico com base em seu tipo e ID.
     *
     * @param type        Tipo do documento ("visit" ou "aep")
     * @param id          ID do documento a ser deletado
     * @param currentUser Usuário atual que está solicitando a deleção
     * @throws IllegalArgumentException Se o tipo de documento for inválido
     */
    @Transactional
    public void deleteDocumentByTypeAndId(String type, Long id, User currentUser) {
        // Usamos um 'if/else if' para ir ao serviço correto, sem ambiguidade
        if ("visit".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço de visita técnica
            technicalVisitService.deleteVisit(id, currentUser);

        } else if ("aep".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço da AEP
            aepService.deleteAepReport(id, currentUser);

        } else if ("risk".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço do checklist
            riskChecklistService.deleteReport(id, currentUser);
        } else {
                // Se o tipo for desconhecido, lançamos um erro
                throw new IllegalArgumentException("Tipo de documento inválido: " + type);
            }
        }

    // --- HELPERS DE MAPEAMENTO ---
    /**
     * Converte um objeto TechnicalVisit em um DocumentSummaryDTO.
     *
     * @param visit A visita técnica a ser convertida
     * @return DocumentSummaryDTO contendo as informações resumidas da visita
     */
    public DocumentSummaryDTO mapVisitToSummaryDto(TechnicalVisit visit) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(visit.getId());
        dto.setDocumentType("Relatório de Visita");
        dto.setTitle(visit.getTitle());
        dto.setClientName(visit.getClientCompany() != null ? visit.getClientCompany().getName() : "N/A");
        dto.setCreationDate(visit.getVisitDate());
        // Define se está assinado (para o cadeado)
        dto.setSigned(visit.getTechnicianSignatureImageBase64() != null && !visit.getTechnicianSignatureImageBase64().isBlank());
        return dto;
    }

    /**
     * Converte um objeto AepReport em um DocumentSummaryDTO.
     *
     * @param aep O relatório AEP a ser convertido
     * @return DocumentSummaryDTO contendo as informações resumidas da avaliação ergonômica
     */
    public DocumentSummaryDTO mapAepToSummaryDto(AepReport aep) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(aep.getId());
        dto.setDocumentType("Avaliação Ergonômica Preliminar");
        dto.setTitle(aep.getEvaluatedFunction());
        dto.setClientName(aep.getCompany() != null ? aep.getCompany().getName() : "N/A");
        dto.setCreationDate(aep.getEvaluationDate());
        dto.setSigned(false); // AEPs geralmente são editáveis (ajuste se tiver assinatura)
        return dto;
    }

    /**
     * Converte um objeto OccupationalRiskReport em um DocumentSummaryDTO.
     *
     * @param report O relatório de riscos ocupacionais a ser convertido
     * @return DocumentSummaryDTO contendo as informações resumidas do checklist de riscos
     */
    public DocumentSummaryDTO mapRiskToSummaryDto(OccupationalRiskReport report) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(report.getId());
        dto.setDocumentType("Checklist de Riscos");
        dto.setTitle(report.getTitle()); // "Checklist - Riscos Ocupacionais"
        dto.setClientName(report.getCompany() != null ? report.getCompany().getName() : "N/A");
        dto.setCreationDate(report.getInspectionDate());
        // Define se está assinado
        dto.setSigned(report.getTechnicianSignatureImageBase64() != null && !report.getTechnicianSignatureImageBase64().isBlank());
        return dto;
    }
}