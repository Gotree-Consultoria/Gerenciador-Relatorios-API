package com.gotree.API.services;

import com.gotree.API.dto.visit.CreateTechnicalVisitRequestDTO;
import com.gotree.API.dto.visit.VisitFindingDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.Sector;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.Unit;
import com.gotree.API.entities.User;
import com.gotree.API.entities.VisitFinding;
import com.gotree.API.repositories.CompanyRepository;
import com.gotree.API.repositories.SectorRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.repositories.UnitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço responsável por gerenciar visitas técnicas, incluindo criação,
 * geração de relatórios PDF, e manipulação de imagens associadas.
 */
@Service
public class TechnicalVisitService {
    

    private final TechnicalVisitRepository technicalVisitRepository;
    private final CompanyRepository companyRepository;
    private final ReportService reportService; // Para gerar o PDF
    private final UnitRepository unitRepository;
    private final SectorRepository sectorRepository;


    @Value("${file.storage.path}")
    private String fileStoragePath;

    @Value("${app.generating-company.name}")
    private String generatingCompanyName;

    @Value("${app.generating-company.cnpj}")
    private String generatingCompanyCnpj;

    public TechnicalVisitService(TechnicalVisitRepository technicalVisitRepository,
                                 CompanyRepository companyRepository,
                                 ReportService reportService,
                                 UnitRepository unitRepository,
                                 SectorRepository sectorRepository) {
        this.technicalVisitRepository = technicalVisitRepository;
        this.companyRepository = companyRepository;
        this.reportService = reportService;
        this.unitRepository = unitRepository;
        this.sectorRepository = sectorRepository;

    }

    /**
     * Cria uma nova visita técnica e gera o relatório PDF correspondente.
     *
     * @param dto        Objeto contendo os dados da visita técnica a ser criada
     * @param technician Usuário técnico responsável pela visita
     * @return A entidade TechnicalVisit criada e salva
     * @throws RuntimeException se a empresa cliente não for encontrada ou houver erro ao salvar arquivos
     */
    @Transactional
    public TechnicalVisit createAndGeneratePdf(CreateTechnicalVisitRequestDTO dto, User technician) {
        // 1. Buscar a empresa cliente
        Company clientCompany = companyRepository.findById(dto.getClientCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa cliente com ID " + dto.getClientCompanyId() + " não encontrada."));

        Unit unit = dto.getUnitId() != null ? unitRepository.findById(dto.getUnitId()).orElse(null) : null;
        Sector sector = dto.getSectorId() != null ? sectorRepository.findById(dto.getSectorId()).orElse(null) : null;

        // 2. Mapear DTO para a entidade principal
        TechnicalVisit visit = new TechnicalVisit();
        visit.setTitle(dto.getTitle());
        visit.setClientCompany(clientCompany);
        visit.setUnit(unit);
        visit.setSector(sector);
        visit.setTechnician(technician);
        visit.setVisitDate(dto.getVisitDate());
        visit.setStartTime(dto.getStartTime());
        visit.setEndTime(LocalTime.now()); // Hora final é a hora da geração
        visit.setLocation(dto.getLocation());
        visit.setSummary(dto.getSummary());

        // Mapear os dados do agendamento da próxima visita
        visit.setNextVisitDate(dto.getNextVisitDate());

        // Mapear dados das assinaturas
        visit.setTechnicianSignatureImageBase64(stripDataUrlPrefix(dto.getTechnicianSignatureImageBase64()));
        visit.setTechnicianSignedAt(LocalDateTime.now());
        visit.setClientSignatureImageBase64(stripDataUrlPrefix(dto.getClientSignatureImageBase64()));
        visit.setClientSignerName(dto.getClientSignerName());
        visit.setClientSignedAt(LocalDateTime.now());
        visit.setClientSignatureLatitude(dto.getClientSignatureLatitude());
        visit.setClientSignatureLongitude(dto.getClientSignatureLongitude());

        // 3. Processar e salvar as imagens e dados dos "findings"
        if (dto.getFindings() != null) {
            dto.getFindings().forEach(findingDto -> {
                VisitFinding finding = mapFindingDtoToEntity(findingDto);
                finding.setTechnicalVisit(visit); // Associa o "achado" à visita
                visit.getFindings().add(finding);
            });
        }

        // 4. Salvar tudo no banco de dados pela primeira vez para gerar os IDs
        TechnicalVisit savedVisit = technicalVisitRepository.save(visit);

        // 5. Gerar o PDF
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("visit", savedVisit);
        templateData.put("generatingCompanyName", generatingCompanyName);
        templateData.put("generatingCompanyCnpj", generatingCompanyCnpj);

        byte[] pdfBytes = reportService.generatePdfFromHtml("visit-report-template", templateData);

        try {
            String fileName = "technical_visit_" + savedVisit.getId() + "_" + UUID.randomUUID() + ".pdf";
            Path path = Paths.get(fileStoragePath, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, pdfBytes);

            // ALTERAÇÃO: Salve apenas o nome do arquivo.
            savedVisit.setPdfPath(fileName);
            return technicalVisitRepository.save(savedVisit); // Salva novamente com o caminho do PDF

        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar a foto 1 do achado: " + e.getMessage(), e);
        }
    }

    /**
     * Converte um DTO de achados da visita para sua entidade correspondente,
     * incluindo o processamento e salvamento das imagens associadas.
     *
     * @param dto DTO contendo os dados do achado da visita
     * @return Uma nova instância de VisitFinding com os dados convertidos
     */
    private VisitFinding mapFindingDtoToEntity(VisitFindingDTO dto) {
        VisitFinding finding = new VisitFinding();
        // Lógica para salvar a IMAGEM 1
        if (dto.getPhotoBase64_1() != null && !dto.getPhotoBase64_1().isEmpty()) {
            try {
                // Limpa o prefixo e decodifica
                byte[] imageBytes = Base64.getDecoder().decode(stripDataUrlPrefix(dto.getPhotoBase64_1()));
                String imageFileName = "finding_" + UUID.randomUUID() + ".jpg";
                Path imagePath = Paths.get(fileStoragePath, "visit_photos", imageFileName);
                Files.createDirectories(imagePath.getParent());
                Files.write(imagePath, imageBytes);
                // Salva o caminho absoluto do arquivo, que será usado com 'file:///' no template
                finding.setPhotoPath1(imagePath.toAbsolutePath().toString().replace("\\", "/"));
            } catch (IOException e) {
                System.err.println("Falha ao salvar a foto 1 do achado: " + e.getMessage());
            }
        }

        // LÓGICA ADICIONADA PARA A IMAGEM 2
        if (dto.getPhotoBase64_2() != null && !dto.getPhotoBase64_2().isEmpty()) {
            try {
                // Limpa o prefixo e decodifica
                byte[] imageBytes = Base64.getDecoder().decode(stripDataUrlPrefix(dto.getPhotoBase64_2()));
                String imageFileName = "finding_" + UUID.randomUUID() + ".jpg";
                Path imagePath = Paths.get(fileStoragePath, "visit_photos", imageFileName);
                Files.createDirectories(imagePath.getParent()); // Não há problema em chamar de novo
                Files.write(imagePath, imageBytes);
                // Salva o caminho absoluto do arquivo
                finding.setPhotoPath2(imagePath.toAbsolutePath().toString().replace("\\", "/"));
            } catch (IOException e) {
                System.err.println("Falha ao salvar a foto 2 do achado: " + e.getMessage());
            }
        }

        // Mapeia o resto dos campos
        finding.setDescription(dto.getDescription());
        finding.setConsequences(dto.getConsequences());
        finding.setLegalGuidance(dto.getLegalGuidance());
        finding.setResponsible(dto.getResponsible());
        finding.setPenalties(dto.getPenalties());
        finding.setDeadline(dto.getDeadline());
        finding.setRecurrence(dto.isRecurrence());

        // Converte a String de prioridade para o Enum de forma segura
        if (dto.getPriority() != null && !dto.getPriority().isBlank()) {
            try {
                finding.setPriority(VisitFinding.Priority.valueOf(dto.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Se a prioridade for inválida (ex: "qualquercoisa"), ignora e deixa nulo
                System.err.println("Prioridade inválida recebida: " + dto.getPriority());
            }
        }
        // Se a prioridade for nula ou em branco, ela simplesmente não será definida, evitando, erros.

        return finding;
    }

    /**
     * Remove o prefixo 'data:image/...' de uma string Base64.
     *
     * @param dataUrl String contendo a URL de dados da imagem
     * @return String Base64 sem o prefixo ou null se a entrada for null
     */
    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl == null) return null;
        int commaIndex = dataUrl.indexOf(',');
        return commaIndex != -1 ? dataUrl.substring(commaIndex + 1) : dataUrl;
    }

    /**
     * Busca todas as visitas técnicas associadas a um técnico específico.
     *
     * @param technician Usuário técnico para filtrar as visitas
     * @return Lista de visitas técnicas ordenadas por data em ordem decrescente
     */
    @Transactional(readOnly = true)
    public List<TechnicalVisit> findAllByTechnician(User technician) {
        return technicalVisitRepository.findByTechnicianOrderByVisitDateDesc(technician);
    }

    /**
     * Exclui uma visita técnica e seus arquivos associados.
     *
     * @param visitId     ID da visita técnica a ser excluída
     * @param currentUser Usuário atual que está tentando excluir a visita
     * @throws RuntimeException  se a visita não for encontrada
     * @throws SecurityException se o usuário atual não for o técnico que criou a visita
     */
    @Transactional
    public void deleteVisit(Long visitId, User currentUser) {
        // 1. Busca o relatório no banco de dados
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + visitId + " não encontrado."));

        // 2. VERIFICAÇÃO DE SEGURANÇA: Garante que só o técnico que criou pode apagar.
        if (!visit.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a deletar este relatório de visita.");
        }

        // 3. APAGA O ARQUIVO PDF DO DISCO
        try {
            if (visit.getPdfPath() != null && !visit.getPdfPath().isBlank()) {
                Path pdfPath = Paths.get(visit.getPdfPath());
                Files.deleteIfExists(pdfPath);
            }
        } catch (IOException e) {
            // Loga o erro, mas não impede a exclusão do registro do banco
            System.err.println("Falha ao deletar o arquivo PDF da visita: " + visit.getPdfPath());
        }

        // 4. APAGA O REGISTRO DO BANCO DE DADOS
        technicalVisitRepository.deleteById(visitId);
    }
}