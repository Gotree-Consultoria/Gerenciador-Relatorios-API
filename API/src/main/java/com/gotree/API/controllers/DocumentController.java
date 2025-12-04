package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.dto.document.FileDownloadDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.DocumentAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST responsável por gerenciar documentos relacionados a visitas técnicas.
 * Fornece endpoints para listar, baixar e excluir documentos gerados durante as visitas.
 * Todos os endpoints requerem autenticação do usuário.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {



    private final DocumentAggregationService documentAggregationService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentAggregationService documentAggregationService) {
        this.documentAggregationService = documentAggregationService;
    }

    /**
     * Retorna todos os documentos associados ao usuário autenticado, com filtros e paginação.
     *
     * @param authentication Objeto de autenticação
     * @param type Filtro por Tipo (visit, aep, risk)
     * @param clientName Filtro por nome do Cliente
     * @param startDate Filtro de data inicial (formato AAAA-MM-DD)
     * @param endDate Filtro de data final (formato AAAA-MM-DD)
     * @param pageable Objeto do Spring que contém a página (page=) e o tamanho (size=)
     * @return ResponseEntity com uma Página (Page) de DocumentSummaryDTO
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DocumentSummaryDTO>> getAllMyDocuments(
            Authentication authentication,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable // O Spring monta isso automaticamente
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        // Passa os novos filtros e a paginação para o serviço
        Page<DocumentSummaryDTO> documentsPage = documentAggregationService.findAllDocumentsForUser(
                technician, type, clientName, startDate, endDate, pageable
        );

        return ResponseEntity.ok(documentsPage);
    }

    /**
     * Retorna os documentos mais recentes do usuário autenticado.
     * Este endpoint é utilizado no dashboard para exibir um histórico resumido
     * dos últimos documentos gerados, limitado aos 5 mais recentes.
     *
     * @param authentication Objeto de autenticação do Spring Security contendo os detalhes do usuário
     * @return ResponseEntity com uma lista limitada de DocumentSummaryDTO
     */
    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentSummaryDTO>> getMyLatestDocuments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        List<DocumentSummaryDTO> latestDocuments = documentAggregationService.findLatestDocumentsForUser(technician);

        return ResponseEntity.ok(latestDocuments);
    }

    /**
     * Permite o download ou visualização de um documento PDF específico.
     * O documento é identificado pelo seu tipo (ex: visita, inspeção) e ID.
     * Verifica se o usuário tem permissão para acessar o documento solicitado.
     * Agora utiliza FileDownloadDTO para garantir o nome correto do arquivo.
     * @param type Tipo do documento (ex: "visit", "inspection")
     * @param id ID do documento
     * @param authentication Objeto de autenticação do Spring Security
     * @return ResponseEntity contendo o arquivo PDF ou status de erro apropriado
     */
    @GetMapping("/{type}/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadDocumentPdf(@PathVariable String type, @PathVariable Long id, Authentication authentication) {

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userDetails.user();

            // 1. Chama o novo método que retorna o DTO (Nome + Bytes)
            FileDownloadDTO fileDto = documentAggregationService.downloadDocument(type, id, currentUser);

            // 2. Retorna com o cabeçalho Content-Disposition configurado com o nome correto
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDto.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(fileDto.getData());

        } catch (IOException e) {
            logger.error("Falha ao ler o arquivo PDF do disco. Tipo: {}, ID: {}. Erro: {}", type, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exclui um documento específico com base no tipo e ID fornecidos.
     * Verifica se o usuário autenticado tem permissão para excluir o documento solicitado.
     * Apenas documentos pertencentes ao usuário podem ser excluídos.
     *
     * @param type           Tipo do documento a ser excluído (ex: "visit", "inspection")
     * @param id             ID único do documento a ser excluído
     * @param authentication Objeto de autenticação do Spring Security contendo os detalhes do usuário
     * @return ResponseEntity sem conteúdo (HTTP 204) indicando sucesso na exclusão
     */
    @DeleteMapping("/{type}/{id}") // Ex: DELETE /documents/visit/45
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(@PathVariable String type, @PathVariable Long id, Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.user();

        // Agora passamos o tipo e o ID para o serviço
        documentAggregationService.deleteDocumentByTypeAndId(type, id, currentUser);

        return ResponseEntity.noContent().build(); // Retorna 204 No Content (sucesso)
    }

    @GetMapping("/latest/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummaryDTO>> getLatestDocumentsForAdmin() {
        List<DocumentSummaryDTO> latestDocuments = documentAggregationService.findAllLatestDocumentsForAdmin();
        return ResponseEntity.ok(latestDocuments);
    }

    /**
     * Endpoint ADMIN: Lista todos os documentos do sistema.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DocumentSummaryDTO>> getAllDocumentsAdmin(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable
    ) {
        Page<DocumentSummaryDTO> documentsPage = documentAggregationService.findAllDocumentsGlobal(
                type, clientName, startDate, endDate, pageable
        );
        return ResponseEntity.ok(documentsPage);
    }
}