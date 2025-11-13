package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
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
/**
 * Serviço responsável por agregar e gerenciar diferentes tipos de documentos no sistema,
 * incluindo Relatórios de Visita Técnica e Avaliações Ergonômicas Preliminares (AEP).
 * Fornece funcionalidades para buscar, carregar e deletar documentos.
 */
@Service
public class DocumentAggregationService {

    private final TechnicalVisitRepository technicalVisitRepository;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;
    private final AepReportRepository aepReportRepository;

    @Value("${file.storage.path}") // Injete o caminho base aqui também
    private String fileStoragePath;

    public DocumentAggregationService(TechnicalVisitRepository technicalVisitRepository,
                                      TechnicalVisitService technicalVisitService,
                                      AepService aepService, AepReportRepository aepReportRepository) {
        this.technicalVisitRepository = technicalVisitRepository;
        this.technicalVisitService = technicalVisitService;
        this.aepService = aepService;
        this.aepReportRepository = aepReportRepository;
    }

    /**
     * Recupera todos os documentos associados a um técnico específico.
     * Inclui tanto Relatórios de Visita Técnica quanto Avaliações Ergonômicas Preliminares.
     *
     * @param technician O usuário técnico para o qual os documentos serão buscados
     * @return Lista de DocumentSummaryDTO contendo todos os documentos ordenados por data de criação
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsForUser(User technician) {

        // Bloco 2: Relatórios de Visita Técnica
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(this::mapVisitToSummaryDto) // Refatorado para helper
                .toList();

        // Bloco 3: Relatório AEP
        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAllByEvaluator(technician)
                .stream()
                .map(this::mapAepToSummaryDto) // Refatorado para helper
                .toList();

        // 3. Junta as listas
        List<DocumentSummaryDTO> allDocuments = Stream.of(technicalVisits, aepReports)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 4. Ordena
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));

        return allDocuments;
    }

    /**
     * Recupera os 5 documentos mais recentes associados a um técnico específico.
     *
     * @param technician O usuário técnico para o qual os documentos serão buscados
     * @return Lista limitada a 5 DocumentSummaryDTO ordenados por data de criação
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {
        List<DocumentSummaryDTO> allDocuments = findAllDocumentsForUser(technician);

        // Retorna apenas os 5 primeiros da lista já ordenada
        return allDocuments.stream().limit(5).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllLatestDocumentsForAdmin() {
        // 1. Busca TODOS os relatórios (sem filtro de usuário)
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAll()
                .stream()
                .map(this::mapVisitToSummaryDto)
                .toList();

        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAll()
                .stream()
                .map(this::mapAepToSummaryDto)
                .toList();

        // 2. Junta as listas
        List<DocumentSummaryDTO> allDocuments = Stream.of(technicalVisits, aepReports)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 3. Ordena e pega os 5 mais recentes
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return allDocuments.stream().limit(5).collect(Collectors.toList());
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

        // Agora, este código só é executado se tivermos um fileName válido.
        Path path;
        if ("visit".equalsIgnoreCase(type)) {
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

        } else {
            // Se o tipo for desconhecido, lançamos um erro
            throw new IllegalArgumentException("Tipo de documento inválido para deleção: " + type);
        }
    }

    // --- HELPERS DE MAPEAMENTO ---
    /**
     * Converte um TechnicalVisit em DTO de resumo.
     */
    public DocumentSummaryDTO mapVisitToSummaryDto(TechnicalVisit visit) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(visit.getId());
        dto.setDocumentType("Relatório de Visita");
        dto.setTitle(visit.getTitle());
        dto.setClientName(visit.getClientCompany() != null ? visit.getClientCompany().getName() : "N/A");
        dto.setCreationDate(visit.getVisitDate());
        return dto;
    }

    /**
     * Converte um AepReport em DTO de resumo.
     */
    public DocumentSummaryDTO mapAepToSummaryDto(AepReport aep) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(aep.getId());
        dto.setDocumentType("Avaliação Ergonômica Preliminar");
        dto.setTitle(aep.getEvaluatedFunction());
        dto.setClientName(aep.getCompany() != null ? aep.getCompany().getName() : "N/A");
        dto.setCreationDate(aep.getEvaluationDate());
        return dto;
    }
}