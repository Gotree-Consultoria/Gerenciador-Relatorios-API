package com.gotree.API.services;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.report.ClientSignatureDTO;
import com.gotree.API.dto.report.InspectionReportResponseDTO;
import com.gotree.API.dto.report.SaveInspectionReportRequestDTO;
import com.gotree.API.entities.*;
import com.gotree.API.entities.enums.DocumentType;
import com.gotree.API.mappers.InspectionReportMapper;
import com.gotree.API.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InspectionReportService {

    private final InspectionReportRepository inspectionReportRepository;
    private final CompanyRepository companyRepository;
    private final UnitRepository unitRepository;
    private final SectorRepository sectorRepository;
    private final TemplateEngine templateEngine;
    private final InspectionReportMapper inspectionReportMapper;

    @Value("${app.generating-company.name}")
    private String generatingCompanyName;

    @Value("${app.generating-company.cnpj}")
    private String generatingCompanyCnpj;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public InspectionReportService(InspectionReportRepository inspectionReportRepository,
                                   CompanyRepository companyRepository,
                                   UnitRepository unitRepository,
                                   SectorRepository sectorRepository,
                                   TemplateEngine templateEngine,
                                   InspectionReportMapper inspectionReportMapper) {
        this.inspectionReportRepository = inspectionReportRepository;
        this.companyRepository = companyRepository;
        this.unitRepository = unitRepository;
        this.sectorRepository = sectorRepository;
        this.templateEngine = templateEngine;
        this.inspectionReportMapper = inspectionReportMapper;
    }

    @Transactional
    public InspectionReport saveReportAndGeneratePdf(SaveInspectionReportRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));

        Unit unit = dto.getUnitId() != null ? unitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada.")) : null;

        Sector sector = dto.getSectorId() != null ? sectorRepository.findById(dto.getSectorId())
                .orElseThrow(() -> new RuntimeException("Setor não encontrado.")) : null;

        InspectionReport report = new InspectionReport();
        report.setCompany(company);
        report.setUnit(unit);
        report.setSector(sector);
        report.setTitle(dto.getTitle());
        report.setType(DocumentType.CHECKLIST_INSPECAO);
        report.setInspectionDate(dto.getInspectionDate());
        report.setLocal(dto.getLocal());
        report.setNotes(dto.getNotes());
        report.setObservations(dto.getObservations());
        report.setResponsavelSigla(dto.getResponsavelSigla());
        report.setResponsavelRegistro(dto.getResponsavelRegistro());

        // --- Lógica de Assinaturas Consolidada ---

        // Dados do Técnico
        report.setTechnician(technician);
        report.setTechnicianSignedAt(LocalDateTime.now());
        if (dto.getTechnicianSignature() != null) {
            report.setTechnicianSignatureImageBase64(dto.getTechnicianSignature().getImageBase64());
        }

        // Dados do Cliente
        if (dto.getClientSignature() != null) {
            report.setClientSignerName(dto.getClientSignature().getSignerName());
            report.setClientSignatureImageBase64(dto.getClientSignature().getImageBase64());
            report.setClientSignatureLatitude(dto.getClientSignature().getLatitude());
            report.setClientSignatureLongitude(dto.getClientSignature().getLongitude());
            report.setClientSignedAt(LocalDateTime.now());
        }

        // Itens do Checklist (Sua lógica aqui já está perfeita)
        if (dto.getSections() != null) {
            dto.getSections().forEach(sectionDto -> {
                ReportSection section = new ReportSection();
                section.setTitle(sectionDto.getTitle());
                section.setNa(sectionDto.isNa());
                section.setReport(report);

                if (!section.isNa() && sectionDto.getItems() != null) {
                    sectionDto.getItems().forEach(itemDto -> {
                        ReportItem item = new ReportItem();
                        item.setDescription(itemDto.getDescription());
                        item.setChecked(itemDto.isChecked());
                        item.setNa(itemDto.isNa());
                        item.setSection(section);
                        section.getItems().add(item);
                    });
                }
                report.getSections().add(section);
            });
        }

        InspectionReport savedReport = inspectionReportRepository.save(report);
        byte[] pdfBytes = generatePdfForReport(savedReport);

        try {
            String fileName = "report_" + savedReport.getId() + "_" + UUID.randomUUID() + ".pdf";
            Path path = Paths.get(fileStoragePath, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, pdfBytes);

            savedReport.setPdfPath(path.toString());
            return inspectionReportRepository.save(savedReport);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o arquivo PDF.", e);
        }
    }

    @Transactional(readOnly = true) // Melhora a performance para consultas
    // 3. ATUALIZE O TIPO DE RETORNO
    public List<InspectionReportResponseDTO> findReportsByTechnicianAndFilters(User technician, String title, String type) {
        // Busca as entidades no banco
        List<InspectionReport> reports = inspectionReportRepository.findByTechnician(technician);

        // Converte para DTOs AQUI DENTRO, com a transação ainda ativa
        return inspectionReportMapper.toDtoList(reports);
    }

    // --- MÉTODO DE BUSCA DO HISTÓRICO CORRIGIDO ---
    @Transactional(readOnly = true)
    // 4. ATUALIZE O TIPO DE RETORNO
    public List<InspectionReportResponseDTO> findLatestReportsByTechnician(User technician) {
        List<InspectionReport> latestReports = inspectionReportRepository.findTop5ByTechnicianOrderByInspectionDateDesc(technician);

        // Converte para DTOs AQUI DENTRO
        return inspectionReportMapper.toDtoList(latestReports);
    }

    public byte[] loadPdfFile(Long reportId) throws IOException {
        InspectionReport report = inspectionReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Relatório com ID " + reportId + " não encontrado."));

        if (report.getPdfPath() == null || report.getPdfPath().isBlank()) {
            throw new RuntimeException("Este relatório não possui um PDF associado.");
        }

        Path path = Paths.get(report.getPdfPath());
        return Files.readAllBytes(path);
    }

    private byte[] generatePdfForReport(InspectionReport report) {
        Map<String, Object> data = new HashMap<>();
        data.put("report", report);

        data.put("generatingCompanyName", generatingCompanyName);
        data.put("generatingCompanyCnpj", generatingCompanyCnpj);

        // Define qual template usar com base no tipo do relatório
        String templateName = "checklist-inspensao-template"; // Lógica pode ser expandida aqui

        // Chama o método reutilizável
        return generatePdfFromHtml(templateName, data);
    }

    // MÉTODO 2: Lógica centralizada para converter HTML em PDF
    private byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);

        String htmlContent = templateEngine.process(templateName, context);

        // DEBUGAR
        // REMOVER DEPOIS DE TESTAR
//        System.out.println("---- INÍCIO DO HTML GERADO PARA O PDF ----");
//        System.out.println(htmlContent);
//        System.out.println("---- FIM DO HTML GERADO PARA O PDF ----");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF a partir do HTML", e);
        }
    }

    @Transactional
    public void deleteReport(Long reportId, User currentUser) {
        // 1. Busca o relatório no banco de dados
        InspectionReport report = inspectionReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Relatório com ID " + reportId + " não encontrado."));

        // 2. VERIFICAÇÃO DE SEGURANÇA: Garante que só o técnico que criou (ou um admin) pode apagar.
        if (!report.getTechnician().getId().equals(currentUser.getId())) {
            // Você pode adicionar uma verificação de role de ADMIN aqui também se quiser
            throw new SecurityException("Usuário não autorizado a deletar este relatório.");
        }

        // 3. APAGA O ARQUIVO PDF DO DISCO
        try {
            if (report.getPdfPath() != null && !report.getPdfPath().isBlank()) {
                Path pdfPath = Paths.get(report.getPdfPath());
                Files.deleteIfExists(pdfPath);
            }
        } catch (IOException e) {
            // Logar o erro, mas não impedir a exclusão do registro do banco
            System.err.println("Falha ao deletar o arquivo PDF: " + report.getPdfPath());
        }

        // 4. APAGA O REGISTRO DO BANCO DE DADOS
        inspectionReportRepository.deleteById(reportId);
    }
}