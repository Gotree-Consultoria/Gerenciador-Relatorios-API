package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.report.InspectionChecklistRequestDTO;
import com.gotree.API.dto.report.InspectionReportResponseDTO;
import com.gotree.API.dto.report.SaveInspectionReportRequestDTO;
import com.gotree.API.entities.InspectionReport;
import com.gotree.API.entities.User;
import com.gotree.API.mappers.InspectionReportMapper;
import com.gotree.API.services.InspectionReportService;
import com.gotree.API.services.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ReportController {

    private final ReportService reportService;
    private final InspectionReportService inspectionReportService;
    private final InspectionReportMapper inspectionReportMapper;

    // Construtor atualizado para receber os dois serviços
    public ReportController(ReportService reportService,
                            InspectionReportService inspectionReportService,
                            InspectionReportMapper inspectionReportMapper) {
        this.reportService = reportService;
        this.inspectionReportService = inspectionReportService;
        this.inspectionReportMapper = inspectionReportMapper;
    }

    // --- ENDPOINT PARA SALVAR O CHECKLIST ---
    // Este é o endpoint que o 'formsPage' chamará após coletar as assinaturas.
    @PostMapping("/inspection-reports")
    public ResponseEntity<?> saveInspectionReport(@Valid @RequestBody SaveInspectionReportRequestDTO dto,
                                                  Authentication authentication) {
        // Chama o serviço para salvar o relatório no banco e gerar/salvar o PDF
        InspectionReport savedReport = inspectionReportService.saveReportAndGeneratePdf(dto, authentication);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Relatório salvo com sucesso!");
        response.put("reportId", savedReport.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- ENDPOINT  PARA A PÁGINA "GERENCIAR DOCUMENTOS" ---
    @GetMapping("/inspection-reports")
    public ResponseEntity<List<InspectionReportResponseDTO>> getMyReports(
            Authentication authentication,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        // A chamada ao serviço agora retorna a lista de DTOs pronta
        List<InspectionReportResponseDTO> reportsDto = inspectionReportService.findReportsByTechnicianAndFilters(technician, title, type);

        // Nenhuma conversão é necessária aqui
        return ResponseEntity.ok(reportsDto);
    }

    // --- ENDPOINT  PARA A SEÇÃO "HISTÓRICO" ---
    @GetMapping("/inspection-reports/latest")
    public ResponseEntity<List<InspectionReportResponseDTO>> getMyLatestReports(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        // A chamada ao serviço agora retorna a lista de DTOs pronta
        List<InspectionReportResponseDTO> latestReportsDto = inspectionReportService.findLatestReportsByTechnician(technician);

        return ResponseEntity.ok(latestReportsDto);
    }

    // --- ENDPOINT PARA DOWNLOAD DO PDF ---
    // O link "Visualizar" nas suas tabelas deve chamar este endpoint.
    @GetMapping("/reports/download/{id}")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long id) {
        try {
            // Pede ao serviço para ler o arquivo PDF do disco
            byte[] pdfBytes = inspectionReportService.loadPdfFile(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "report_" + id + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            // Trata o erro caso o arquivo não seja encontrado no servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            // Trata o erro caso o relatório não seja encontrado no banco
            return ResponseEntity.notFound().build();
        }
    }

    // --- SEUS ENDPOINTS ANTIGOS (OPCIONAL) ---
    // Estes endpoints geravam PDF diretamente sem salvar.
    // Você pode mantê-los para testes ou removê-los se o novo fluxo os substitui completamente.
    @PostMapping(path = "/inspection-checklist/pdf-transient", produces = "application/pdf")
    public ResponseEntity<byte[]> generateTransientInspectionChecklistPdf(@RequestBody InspectionChecklistRequestDTO data) {
        // ... sua lógica antiga para gerar PDF sem salvar ...
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("checklistItems", data.getItems());
        byte[] pdfBytes = reportService.generatePdfFromHtml("checklist-inspensao-template", templateData);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "checklist_inspecao.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @DeleteMapping("/inspection-reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id, Authentication authentication) {
        // Pega o usuário logado para a verificação de segurança
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        // Chama o serviço para executar a lógica de exclusão
        inspectionReportService.deleteReport(id, currentUser);

        // Retorna uma resposta de sucesso sem conteúdo (padrão para DELETE)
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }
}