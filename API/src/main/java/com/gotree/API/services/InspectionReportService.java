package com.gotree.API.services;

import com.gotree.API.dto.report.*;
import com.gotree.API.entities.*;
import com.gotree.API.entities.enums.DocumentType;
import com.gotree.API.entities.enums.NrsCheckStatus;
import com.gotree.API.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InspectionReportService {

    // --- Repositórios ---
    private final InspectionReportRepository inspectionReportRepository;
    private final ReportService reportService;
    private final CompanyRepository companyRepository;
    private final UnitRepository unitRepository;
    private final SectorRepository sectorRepository;
    // (Não precisamos dos novos repos NrsSection/Item aqui, o Cascade.ALL cuida)

    @Value("${file.storage.path}")
    private String fileStoragePath;
    @Value("${app.generating-company.name}")
    private String generatingCompanyName;
    @Value("${app.generating-company.cnpj}")
    private String generatingCompanyCnpj;

    public InspectionReportService(InspectionReportRepository inspectionReportRepository,
                                   ReportService reportService,
                                   CompanyRepository companyRepository,
                                   UnitRepository unitRepository,
                                   SectorRepository sectorRepository) {
        this.inspectionReportRepository = inspectionReportRepository;
        this.reportService = reportService;
        this.companyRepository = companyRepository;
        this.unitRepository = unitRepository;
        this.sectorRepository = sectorRepository;
    }

    // ==========================================
    // --- CHECKLIST ---
    // ==========================================
    @Transactional
    public InspectionReport saveReportAndGeneratePdf(SaveInspectionReportRequestDTO dto, User technician) {

        // 1. Constrói o relatório com todos os dados comuns
        InspectionReport report = buildCommonReport(
                dto.getCompanyId(), dto.getUnitId(), dto.getSectorId(), technician,
                dto.getTitle(), dto.getInspectionDate(), dto.getLocal(),
                dto.getNotes(), dto.getObservations(), dto.getResponsavelSigla(),
                dto.getResponsavelRegistro(), dto.isUseDigitalSignature(),
                dto.getTechnicianSignature(), dto.getClientSignature()
        );

        // 2. Adiciona a lógica ÚNICA deste checklist (mapeia 'sections')
        if (dto.getSections() != null) {
            for (SectionDTO sectionDto : dto.getSections()) {
                ReportSection section = new ReportSection();
                section.setTitle(sectionDto.getTitle());
                section.setNa(sectionDto.isNa());
                section.setReport(report);

                if (sectionDto.getItems() != null) {
                    for (ChecklistItemDTO itemDto : sectionDto.getItems()) {
                        ReportItem item = new ReportItem();
                        item.setDescription(itemDto.getDescription());
                        item.setChecked(itemDto.isChecked());
                        item.setNa(itemDto.isNa());
                        item.setSection(section);
                        section.getItems().add(item);
                    }
                }
                report.getSections().add(section);
            }
        }

        // 3. Gera o PDF e salva
        return generateAndSavePdf(report, "checklist-inspensao-template");
    }

    // ==========================================
    // --- CHECKLIST-NR ---
    // ==========================================
    @Transactional
    public InspectionReport saveNrsReportAndGeneratePdf(SaveNrsReportRequestDTO dto, User technician) {

        // 1. Constrói o relatório com todos os dados comuns (usando o MESMO helper)
        InspectionReport report = buildCommonReport(
                dto.getCompanyId(), dto.getUnitId(), dto.getSectorId(), technician,
                dto.getTitle(), dto.getInspectionDate(), dto.getLocal(),
                dto.getNotes(), dto.getObservations(), dto.getResponsavelSigla(),
                dto.getResponsavelRegistro(), dto.isUseDigitalSignature(),
                dto.getTechnicianSignature(), dto.getClientSignature()
        );

        // 2. Adiciona a lógica ÚNICA deste checklist (mapeia 'nrsSections')
        if (dto.getNrsSections() != null) {
            for (NrsSectionDTO sectionDto : dto.getNrsSections()) {
                NrsSection nrsSection = new NrsSection();
                nrsSection.setTitle(sectionDto.getTitle());
                nrsSection.setReport(report);

                // 1. Mapeia os itens filhos (como antes)
                if (sectionDto.getItems() != null) {
                    for (NrsChecklistItemDTO itemDto : sectionDto.getItems()) {
                        NrsItem nrsItem = new NrsItem();
                        nrsItem.setDescription(itemDto.getDescription());
                        nrsItem.setJustification(itemDto.getJustification());
                        nrsItem.setStatus(convertStringToNrsStatus(itemDto.getStatus()));
                        nrsItem.setSection(nrsSection);
                        nrsSection.getItems().add(nrsItem);
                    }
                }

                // 2. (NOVO) Calcula e salva o status da seção-pai
                nrsSection.setSummaryStatus(calculateSectionSummaryStatus(nrsSection.getItems()));

                report.getNrsSections().add(nrsSection);
            }
        }

        // 3. Gera o PDF e salva
        // CRIE UM NOVO TEMPLATE HTML com este nome!
        return generateAndSavePdf(report, "nrs-checklist-template");
    }

    // ==========================================
    // --- MÉTODOS AUXILIARES PRIVADOS ---
    // ==========================================

    /**
     * Calcula o status resumo de uma Seção baseado na sua regra de negócio.
     */
    private NrsCheckStatus calculateSectionSummaryStatus(List<NrsItem> items) {
        if (items == null || items.isEmpty()) {
            return null; // Ou NrsCheckStatus.NAO_APLICA, dependendo da sua regra
        }

        int totalItems = items.size();
        int naCount = 0;
        int ncCount = 0;
        int cCount = 0;

        for (NrsItem item : items) {
            if (item == null || item.getStatus() == null) {
                continue; // Ignora itens nulos ou com status nulo
            }
            switch (item.getStatus()) {
                case NAO_APLICA:
                    naCount++;
                    break;
                case NAO_CONFORME:
                    ncCount++;
                    break;
                case CONFORME:
                    cCount++;
                    break;
            }
        }

        // Sua Regra: "se todos as opções abaixo estiverem marcadas como NA, o mesmo deve acontecer..."
        if (naCount == totalItems) {
            return NrsCheckStatus.NAO_APLICA;
        }

        // Sua Regra: "se todos as opções abaixo estiverem marcadas como NC..."
        // (Assumindo que "todos" significa "todos os que não são NA")
        if (ncCount > 0 && (ncCount + naCount) == totalItems) {
            return NrsCheckStatus.NAO_CONFORME;
        }

        // Se chegou aqui e não tem NC, é CONFORME
        if (cCount > 0 && ncCount == 0) {
            return NrsCheckStatus.CONFORME;
        }

        // Se houver qualquer mistura (ex: C e NC), podemos retornar null ou um status "MISTO"
        // Para o seu template (C, NC, NA), o CONFORME é o mais provável.
        if (cCount > 0) {
            return NrsCheckStatus.CONFORME;
        }

        // Se não for nem C, nem NC, nem NA (ex: todos nulos)
        return null;
    }

    /**
     * Constrói a entidade InspectionReport com todos os dados comuns
     * (cabeçalho, assinaturas, etc.), sem salvar no banco.
     */
    private InspectionReport buildCommonReport(
            Long companyId, Long unitId, Long sectorId, User technician,
            String title, LocalDate inspectionDate, String local, String notes,
            String observations, String responsavelSigla, String responsavelRegistro,
            boolean useDigitalSignature, TechnicianSignatureDTO technicianSignature,
            ClientSignatureDTO clientSignature
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));
        Unit unit = (unitId != null) ? unitRepository.findById(unitId).orElse(null) : null;
        Sector sector = (sectorId != null) ? sectorRepository.findById(sectorId).orElse(null) : null;

        InspectionReport report = new InspectionReport();
        report.setTechnician(technician);
        report.setCompany(company);
        report.setUnit(unit);
        report.setSector(sector);
        report.setType(DocumentType.CHECKLIST_INSPECAO);
        report.setTitle(title);
        report.setInspectionDate(inspectionDate);
        report.setLocal(local);
        report.setNotes(notes);
        report.setObservations(observations);
        report.setResponsavelSigla(responsavelSigla);
        report.setResponsavelRegistro(responsavelRegistro);
        report.setDigitallySigned(useDigitalSignature);

        if (technicianSignature != null) {
            report.setTechnicianSignatureImageBase64(stripDataUrlPrefix(technicianSignature.getImageBase64()));
            report.setTechnicianSignedAt(LocalDateTime.now());
        }
        if (clientSignature != null) {
            report.setClientSignerName(clientSignature.getSignerName());
            report.setClientSignatureImageBase64(stripDataUrlPrefix(clientSignature.getImageBase64()));
            report.setClientSignatureLatitude(clientSignature.getLatitude());
            report.setClientSignatureLongitude(clientSignature.getLongitude());
            report.setClientSignedAt(LocalDateTime.now());
        }

        return report;
    }

    /**
     * Salva o relatório, gera o PDF, salva o arquivo e atualiza o relatório.
     */
    private InspectionReport generateAndSavePdf(InspectionReport report, String templateName) {
        // Salva uma vez para obter o ID
        InspectionReport savedReport = inspectionReportRepository.save(report);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("report", savedReport);
        templateData.put("company", savedReport.getCompany());
        templateData.put("generatingCompanyName", generatingCompanyName);
        templateData.put("generatingCompanyCnpj", generatingCompanyCnpj);

        byte[] pdfBytes = reportService.generatePdfFromHtml(templateName, templateData);

        try {
            String fileName = templateName + "_" + savedReport.getId() + "_" + UUID.randomUUID() + ".pdf";
            Path path = Paths.get(fileStoragePath, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, pdfBytes);
            savedReport.setPdfPath(path.toString());

            return inspectionReportRepository.save(savedReport); // Salva novamente com o caminho

        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o arquivo PDF: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // --- METODO PÚBLICO: DELETAR RELATÓRIO --- (ADICIONE ESTE BLOCO)
    // ==========================================

    /**
     * Deleta um Relatório de Inspeção e seu arquivo PDF associado.
     */
    @Transactional
    public void deleteReport(Long reportId, User currentUser) {
        // 1. Encontre o relatório ou lance um erro
        InspectionReport report = inspectionReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Relatório de Inspeção com ID " + reportId + " não encontrado."));

        // 2. Verificação de Segurança: Só o técnico que criou pode apagar.
        if (!report.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a deletar este relatório de inspeção.");
        }

        // 3. Delete o arquivo PDF do disco
        try {
            if (report.getPdfPath() != null && !report.getPdfPath().isBlank()) {
                Path pdfPath = Paths.get(report.getPdfPath());
                Files.deleteIfExists(pdfPath);
            }
        } catch (IOException e) {
            // Loga o erro, mas não impede a exclusão do registro do banco
            System.err.println("Falha ao deletar o arquivo PDF da inspeção: " + report.getPdfPath() + " | Erro: " + e.getMessage());
        }

        // 4. Delete o relatório do banco de dados
        // (O Cascade.ALL cuidará de deletar as 'sections' e 'nrsSections' filhas)
        inspectionReportRepository.delete(report);
    }


    // ==========================================
    // --- MÉTODOS AUXILIARES PRIVADOS ---
    // (Seus métodos 'buildCommonReport', 'generateAndSavePdf', etc. ficam aqui)
    // ==========================================

    // --- Outros Métodos Auxiliares ---

    private NrsCheckStatus convertStringToNrsStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return NrsCheckStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Status inválido recebido: " + status);
            return null;
        }
    }

    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl == null) return null;
        int commaIndex = dataUrl.indexOf(',');
        return commaIndex != -1 ? dataUrl.substring(commaIndex + 1) : dataUrl;
    }
}