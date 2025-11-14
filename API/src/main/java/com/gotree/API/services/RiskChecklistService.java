package com.gotree.API.services;

import com.gotree.API.dto.risk.EvaluatedFunctionRequestDTO;
import com.gotree.API.dto.risk.SaveRiskReportRequestDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.*;
import com.gotree.API.utils.RiskCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo gerenciamento de checklists de riscos ocupacionais.
 * Permite criar, atualizar, deletar e gerar PDFs dos relatórios de avaliação de riscos.
 */
@Service
public class RiskChecklistService {
    

    private final OccupationalRiskReportRepository reportRepository;
    private final CompanyRepository companyRepository;
    private final UnitRepository unitRepository;
    private final SectorRepository sectorRepository;
    private final ReportService reportService;
    private final SystemInfoRepository systemInfoRepository; // Para a Logo

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public RiskChecklistService(OccupationalRiskReportRepository reportRepository,
                                CompanyRepository companyRepository,
                                UnitRepository unitRepository,
                                SectorRepository sectorRepository,
                                ReportService reportService,
                                SystemInfoRepository systemInfoRepository) {
        this.reportRepository = reportRepository;
        this.companyRepository = companyRepository;
        this.unitRepository = unitRepository;
        this.sectorRepository = sectorRepository;
        this.reportService = reportService;
        this.systemInfoRepository = systemInfoRepository;
    }

    /**
     * Cria um novo relatório de riscos ocupacionais e gera seu PDF.
     *
     * @param dto        Objeto contendo os dados do relatório a ser criado
     * @param technician Usuário técnico responsável pelo relatório
     * @return Relatório criado e salvo com o PDF gerado
     * @throws RuntimeException se a empresa não for encontrada
     */
    @Transactional
    public OccupationalRiskReport createAndGeneratePdf(SaveRiskReportRequestDTO dto, User technician) {
        // 1. Buscando Entidades Relacionadas
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        Unit unit = dto.getUnitId() != null ? unitRepository.findById(dto.getUnitId()).orElse(null) : null;
        Sector sector = dto.getSectorId() != null ? sectorRepository.findById(dto.getSectorId()).orElse(null) : null;

        // 2. Criando o Relatório
        OccupationalRiskReport report = new OccupationalRiskReport();
        report.setCompany(company);
        report.setUnit(unit);
        report.setSector(sector);
        report.setTechnician(technician);
        report.setInspectionDate(dto.getInspectionDate());

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            report.setTitle(dto.getTitle());
        } else {
            // Título padrão caso o front não mande nada
            report.setTitle("Checklist de Riscos - " + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(dto.getInspectionDate()));
        }

        if (dto.getTechnicianSignatureImageBase64() != null && !dto.getTechnicianSignatureImageBase64().isBlank()) {
            report.setTechnicianSignatureImageBase64(stripDataUrlPrefix(dto.getTechnicianSignatureImageBase64()));
            report.setTechnicianSignedAt(java.time.LocalDateTime.now());
        }

        // 3. Mapeando Funções e Riscos
        mapFunctionsDtoToEntity(dto.getFunctions(), report);

        // 4. Salva no banco (gera ID)
        OccupationalRiskReport savedReport = reportRepository.save(report);

        // 5. Gera o PDF
        return generatePdf(savedReport);
    }

    /**
     * ATUALIZA UM RELATÓRIO EXISTENTE.
     * Regra de Ouro: Se já estiver assinado, lança erro e impede edição.
     */
    /**
     * Atualiza um relatório existente e regenera seu PDF.
     *
     * @param id          ID do relatório a ser atualizado
     * @param dto         Objeto contendo os novos dados do relatório
     * @param currentUser Usuário atual realizando a atualização
     * @return Relatório atualizado com novo PDF gerado
     * @throws RuntimeException      se o relatório não for encontrado
     * @throws SecurityException     se o usuário não tiver permissão
     * @throws IllegalStateException se o relatório já estiver assinado
     */
    @Transactional
    public OccupationalRiskReport updateReport(Long id, SaveRiskReportRequestDTO dto, User currentUser) {
        // 1. Busca o relatório existente
        OccupationalRiskReport report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));

        // 2. Validação de Segurança (Dono)
        if (!report.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para alterar este relatório.");
        }

        // 3. REGRA DE NEGÓCIO: Bloqueio se já assinado
        if (report.getTechnicianSignatureImageBase64() != null && !report.getTechnicianSignatureImageBase64().isBlank()) {
            throw new IllegalStateException("Este relatório já foi assinado e finalizado. Não é possível editá-lo.");
        }

        // 4. Atualiza Dados Básicos
        report.setInspectionDate(dto.getInspectionDate());

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            report.setTitle(dto.getTitle());
        }

        if (dto.getUnitId() != null) report.setUnit(unitRepository.findById(dto.getUnitId()).orElse(null));
        if (dto.getSectorId() != null) report.setSector(sectorRepository.findById(dto.getSectorId()).orElse(null));

        // 5. Atualiza Assinatura (Se o usuário decidiu assinar AGORA na edição)
        if (dto.getTechnicianSignatureImageBase64() != null && !dto.getTechnicianSignatureImageBase64().isBlank()) {
            report.setTechnicianSignatureImageBase64(stripDataUrlPrefix(dto.getTechnicianSignatureImageBase64()));
            report.setTechnicianSignedAt(LocalDateTime.now());
        }

        // 6. Atualiza a Lista de Funções/Riscos
        // Limpa a lista antiga (o orphanRemoval=true na entidade vai deletar do banco)
        report.getEvaluatedFunctions().clear();
        // Adiciona as novas
        mapFunctionsDtoToEntity(dto.getFunctions(), report);

        // 7. Salva e Regenera PDF
        OccupationalRiskReport updatedReport = reportRepository.save(report);
        return generatePdf(updatedReport);
    }

    /**
     * Exclui um relatório e seu arquivo PDF associado.
     *
     * @param id          ID do relatório a ser excluído
     * @param currentUser Usuário atual realizando a exclusão
     * @throws RuntimeException  se o relatório não for encontrado
     * @throws SecurityException se o usuário não tiver permissão
     */
    @Transactional
    public void deleteReport(Long id, User currentUser) {
        OccupationalRiskReport report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado"));

        if (!report.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão para deletar.");
        }

        // Apaga o PDF físico
        if (report.getPdfPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(report.getPdfPath()));
            } catch (IOException e) {
                System.err.println("Erro ao deletar arquivo: " + e.getMessage());
            }
        }

        reportRepository.delete(report);
    }

    @Transactional(readOnly = true)
    public SaveRiskReportRequestDTO findReportForEdit(Long id, User currentUser) {
        // 1. Busca o relatório
        OccupationalRiskReport report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));

        // 2. Validação de Segurança
        if (!report.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para ver este relatório.");
        }

        // 3. Converte a Entidade para o DTO
        SaveRiskReportRequestDTO dto = new SaveRiskReportRequestDTO();
        dto.setTitle(report.getTitle());
        dto.setInspectionDate(report.getInspectionDate());
        dto.setCompanyId(report.getCompany().getId());
        dto.setUnitId(report.getUnit() != null ? report.getUnit().getId() : null);
        dto.setSectorId(report.getSector() != null ? report.getSector().getId() : null);

        // 4. Converte a lista de funções (o mais complexo)
        List<EvaluatedFunctionRequestDTO> functionDtos = report.getEvaluatedFunctions().stream()
                .map(funcEntity -> {
                    EvaluatedFunctionRequestDTO funcDto = new EvaluatedFunctionRequestDTO();
                    funcDto.setFunctionName(funcEntity.getFunctionName());
                    funcDto.setSelectedRiskCodes(funcEntity.getSelectedRiskCodes());
                    return funcDto;
                }).collect(Collectors.toList());

        dto.setFunctions(functionDtos);

        return dto;
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Mapeia as funções do DTO para entidades do relatório.
     *
     * @param functionDtos Lista de DTOs das funções avaliadas
     * @param report       Relatório ao qual as funções serão vinculadas
     */
    private void mapFunctionsDtoToEntity(List<EvaluatedFunctionRequestDTO> functionDtos, OccupationalRiskReport report) {
        if (functionDtos != null) {
            for (EvaluatedFunctionRequestDTO funcDto : functionDtos) {
                EvaluatedFunction evalFunc = new EvaluatedFunction();
                evalFunc.setFunctionName(funcDto.getFunctionName());
                evalFunc.setSelectedRiskCodes(funcDto.getSelectedRiskCodes());
                evalFunc.setReport(report);
                report.getEvaluatedFunctions().add(evalFunc);
            }
        }
    }

    /**
     * Gera o arquivo PDF do relatório usando template HTML.
     *
     * @param report Relatório para o qual o PDF será gerado
     * @return Relatório atualizado com o caminho do novo PDF
     * @throws RuntimeException se houver erro ao salvar o PDF
     */
    private OccupationalRiskReport generatePdf(OccupationalRiskReport report) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("report", report);

        // Busca Dados da Go-Tree (Logo, CNPJ, Nome)
        SystemInfo myInfo = systemInfoRepository.findFirst();
        if (myInfo != null) {
            templateData.put("generatingCompanyName", myInfo.getCompanyName());
            templateData.put("generatingCompanyCnpj", myInfo.getCnpj());
            templateData.put("generatingCompanyLogo", myInfo.getLogoBase64());
        } else {
            // Fallback
            templateData.put("generatingCompanyName", "Go-Tree Consultoria");
            templateData.put("generatingCompanyCnpj", "47.885.556/0001-76");
            templateData.put("generatingCompanyLogo", null);
        }

        // Prepara dados dos riscos para o Template (Converte IDs em Textos)
        List<Map<String, Object>> functionsData = new ArrayList<>();
        for (EvaluatedFunction func : report.getEvaluatedFunctions()) {
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("name", func.getFunctionName());

            List<RiskCatalog.RiskItem> risks = new ArrayList<>();
            if (func.getSelectedRiskCodes() != null) {
                for (Integer code : func.getSelectedRiskCodes()) {
                    RiskCatalog.RiskItem item = RiskCatalog.getByCode(code);
                    if (item != null) risks.add(item);
                }
            }
            funcMap.put("risks", risks);
            functionsData.add(funcMap);
        }
        templateData.put("functionsData", functionsData);

        // Gera PDF
        byte[] pdfBytes = reportService.generatePdfFromHtml("risk-checklist-template", templateData);

        try {
            // Apaga PDF antigo se existir (para economizar espaço/limpeza)
            if (report.getPdfPath() != null) {
                Files.deleteIfExists(Paths.get(report.getPdfPath()));
            }

            String fileName = "RISK_CHECKLIST_" + report.getId() + "_" + UUID.randomUUID() + ".pdf";
            Path path = Paths.get(fileStoragePath, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, pdfBytes);

            report.setPdfPath(path.toString());
            return reportRepository.save(report);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar PDF: " + e.getMessage());
        }
    }

    /**
     * Remove o prefixo data:image da string base64.
     *
     * @param dataUrl String base64 da imagem com prefixo
     * @return String base64 limpa sem prefixo
     */
    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl == null) return null;
        int commaIndex = dataUrl.indexOf(',');
        return commaIndex != -1 ? dataUrl.substring(commaIndex + 1) : dataUrl;
    }
}