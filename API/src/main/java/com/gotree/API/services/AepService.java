package com.gotree.API.services;

import com.gotree.API.dto.aep.AepDetailDTO;
import com.gotree.API.dto.aep.AepRequestDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.CompanyRepository;
import com.gotree.API.repositories.PhysiotherapistRepository;
import com.gotree.API.repositories.SystemInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de Análises Ergonômicas Preliminares (AEP).
 * Fornece funcionalidades para criar, atualizar, carregar, gerar PDF e excluir relatórios AEP.
 */
@Service
public class AepService {



    // A LISTA MESTRE DE TODOS OS RISCOS
    private static final List<String> MASTER_RISK_LIST = Arrays.asList(
            "Trabalho em posturas incômodas ou pouco confortáveis por longos períodos",
            "Postura sentada por longos períodos",
            "Postura de pé por longos períodos",
            "Frequente deslocamento a pé durante a jornada de trabalho",
            "Trabalho com esforço físico intenso",
            "Levantamento e transporte manual de cargas ou volumes",
            "Frequente ação de puxar/empurrar cargas ou volumes",
            "Frequente execução de movimentos repetitivos",
            "Manuseio de ferramentas e/ou objetos pesados por longos períodos",
            "Exigência de uso frequente de força, pressão, preensão, flexão, extensão ou torção dos segmentos corporais",
            "Compressão de partes do corpo por superfícies rígidas ou com quinas",
            "Exigência de flexões de coluna vertebral frequentes",
            "Uso frequente de pedais",
            "Uso frequente de alavancas",
            "Exigência de elevação frequente de membros superiores",
            "Manuseio ou movimentação de cargas e volumes sem pega ou com \"pega pobre\"",
            "Uso frequente de escadas",
            "Trabalho intensivo com teclado ou outros dispositivos de entrada de dados",
            "Posto de trabalho improvisado",
            "Mobiliário sem meios de regulagem de ajuste",
            "Equipamentos e/ou máquinas sem meios de regulagem de ajuste ou sem condições de uso",
            "Posto de trabalho não planejado/adaptado para a posição sentada",
            "Assento inadequado",
            "Encosto do assento inadequado ou ausente",
            "Mobiliário ou equipamento sem espaço para movimentação de segmentos corporais",
            "Trabalho com necessidade de alcançar objetos, documentos, controles ou qualquer ponto além das zonas de alcance ideais para as características antropométricas do trabalhador",
            "Equipamentos ou mobiliários não adaptados à antropometria do trabalhador",
            "Condições de trabalho com níveis de pressão sonora fora dos parâmetros de conforto",
            "Condições de trabalho com índice de temperatura efetiva fora dos parâmetros de conforto",
            "Condições de trabalho com velocidade do ar fora dos parâmetros de conforto",
            "Condições de trabalho com umidade do ar fora dos parâmetros de conforto",
            "Condições de trabalho com Iluminação diurna inadequada",
            "Condições de trabalho com Iluminação noturna inadequada",
            "Presença de reflexos em telas, painéis, vidros, monitores ou qualquer superfície, que causem desconforto ou prejudiquem a visualização",
            "Piso escorregadio e/ou irregular"
    );

    private final AepReportRepository aepReportRepository;
    private final CompanyRepository companyRepository;
    private final ReportService reportService;
    private final PhysiotherapistRepository physioRepository;
    private final SystemInfoRepository systemInfoRepository;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public AepService(AepReportRepository aepReportRepository, CompanyRepository companyRepository,
                      ReportService reportService, PhysiotherapistRepository physioRepository,
                      SystemInfoRepository systemInfoRepository) {
        this.aepReportRepository = aepReportRepository;
        this.companyRepository = companyRepository;
        this.reportService = reportService;
        this.physioRepository = physioRepository;
        this.systemInfoRepository = systemInfoRepository;
    }


    /**
     * Salva ou atualiza os dados de uma AEP no banco de dados.
     *
     * @param dto        DTO contendo os dados da AEP a serem salvos
     * @param evaluator  Usuário que está realizando a avaliação
     * @param existingId ID da AEP existente (null para nova AEP)
     * @return AepReport entidade salva
     * @throws RuntimeException  se a empresa ou fisioterapeuta não forem encontrados
     * @throws SecurityException se o usuário não estiver autorizado a editar a AEP
     */
    @Transactional
    public AepReport saveAepData(AepRequestDTO dto, User evaluator, Long existingId) {
        // Se um ID foi fornecido, busca o relatório para edição
        AepReport aep = (existingId != null)
                ? aepReportRepository.findById(existingId).orElseThrow(() -> new RuntimeException("AEP não encontrada"))
                : new AepReport();

        // Validação de segurança (só o criador ou admin pode editar)
        if (existingId != null && !aep.getEvaluator().getId().equals(evaluator.getId())) {
            // Você pode adicionar uma verificação de "ROLE_ADMIN" aqui
            throw new SecurityException("Usuário não autorizado a editar esta AEP.");
        }

        // Busca a entidade Physiotherapist com base no ID vindo do DTO
        Physiotherapist physio = (dto.getPhysiotherapistId() != null)
                ? physioRepository.findById(dto.getPhysiotherapistId())
                .orElseThrow(() -> new RuntimeException("Fisioterapeuta não encontrado."))
                : null; // Permite salvar sem um fisio

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));

        aep.setCompany(company);
        aep.setEvaluator(evaluator);
        aep.setEvaluationDate(dto.getEvaluationDate());
        aep.setEvaluatedFunction(dto.getEvaluatedFunction());
        aep.setSelectedRisks(dto.getSelectedRiskIds());

        // Dados da Fisio
        aep.setPhysiotherapist(physio);

        // Se o documento foi editado, limpa o caminho do PDF antigo
        if (existingId != null && aep.getPdfPath() != null) {
            deletePdfFile(aep.getPdfPath()); // Deleta o arquivo físico
            aep.setPdfPath(null); // Limpa o caminho no banco
        }

        return aepReportRepository.save(aep);
    }


    /**
     * Carrega um PDF existente ou gera um novo PDF para uma AEP.
     *
     * @param id          ID da AEP
     * @param currentUser Usuário atual solicitando o PDF
     * @return array de bytes contendo o PDF
     * @throws IOException      em caso de erro na manipulação do arquivo
     * @throws RuntimeException se a AEP não for encontrada
     */
    @Transactional
    public byte[] loadOrGenerateAepPdf(Long id, User currentUser) throws IOException {
        AepReport aep = aepReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AEP com ID " + id + " não encontrada."));

        // Se o PDF já foi gerado e salvo, apenas o retorna
        if (aep.getPdfPath() != null && !aep.getPdfPath().isBlank()) {
            try {
                Path path = Paths.get(aep.getPdfPath());
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
            } catch (Exception e) {
                // Se o arquivo não existir (ex: foi apagado do disco), geramos um novo
            }
        }

        // Se o PDF não existe (novo ou editado), GERA UM NOVO
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("aep", aep);
        templateData.put("company", aep.getCompany());
        templateData.put("evaluator", aep.getEvaluator());

        // Busca dados da empresa do banco de dados
        SystemInfo sysInfo = systemInfoRepository.findFirst();
        if (sysInfo != null) {
            templateData.put("generatingCompanyName", sysInfo.getCompanyName());
            templateData.put("generatingCompanyCnpj", sysInfo.getCnpj());
        } else {
            // Fallback para valores padrão se não encontrar no banco
            templateData.put("generatingCompanyName", "Go-Tree Consultoria LTDA");
            templateData.put("generatingCompanyCnpj", "47.885.556/0001-76");
        }

        templateData.put("allRisks", MASTER_RISK_LIST);
        templateData.put("selectedRisks", aep.getSelectedRisks());

        byte[] pdfBytes = reportService.generatePdfFromHtml("aep-template", templateData);

        // Salva o novo PDF no disco e atualiza a entidade
        String fileName = "AEP_" + aep.getId() + "_" + UUID.randomUUID() + ".pdf";
        Path path = Paths.get(fileStoragePath, fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, pdfBytes);

        aep.setPdfPath(path.toString()); // Salva o caminho do NOVO PDF
        aepReportRepository.save(aep);

        return pdfBytes;
    }

    /**
     * Busca os detalhes de uma AEP específica.
     */
    @Transactional(readOnly = true)
    public AepDetailDTO findAepDetails(Long id, User currentUser) {
        AepReport aep = aepReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AEP com ID " + id + " não encontrada."));

        // Verificação de segurança
        if (!aep.getEvaluator().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a visualizar esta AEP.");
        }

        // Mapeamento manual da Entidade para o DTO de Detalhes
        AepDetailDTO dto = new AepDetailDTO();
        dto.setId(aep.getId());
        if (aep.getCompany() != null) {
            dto.setCompanyId(aep.getCompany().getId());
            dto.setCompanyName(aep.getCompany().getName());
            dto.setCompanyCnpj(aep.getCompany().getCnpj());
        }
        dto.setEvaluationDate(aep.getEvaluationDate());
        dto.setEvaluatedFunction(aep.getEvaluatedFunction());

        // --- CORREÇÃO DO LAZY INITIALIZATION ---
        // Cria uma NOVA lista (ArrayList) para forçar o carregamento imediato dos dados
        if (aep.getSelectedRisks() != null) {
            dto.setSelectedRisks(new java.util.ArrayList<>(aep.getSelectedRisks()));
        } else {
            dto.setSelectedRisks(new java.util.ArrayList<>());
        }
        // ---------------------------------------

        dto.setPhysiotherapistId(aep.getPhysiotherapist() != null ? aep.getPhysiotherapist().getId() : null);
        dto.setEvaluatorId(aep.getEvaluator().getId());
        dto.setEvaluatorName(aep.getEvaluator().getName());

        return dto;
    }

    /**
     * Deleta uma AEP e seu arquivo PDF associado.
     *
     * @param id          ID da AEP a ser deletada
     * @param currentUser Usuário atual solicitando a deleção
     * @throws RuntimeException  se a AEP não for encontrada
     * @throws SecurityException se o usuário não estiver autorizado a deletar a AEP
     */
    @Transactional
    public void deleteAepReport(Long id, User currentUser) {
        AepReport aep = aepReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AEP com ID " + id + " não encontrada."));

        if (!aep.getEvaluator().getId().equals(currentUser.getId())) {
            // Adicionar verificação de ROLE_ADMIN se necessário
            throw new SecurityException("Usuário não autorizado a deletar esta AEP.");
        }

        if (aep.getPdfPath() != null) {
            deletePdfFile(aep.getPdfPath());
        }

        aepReportRepository.delete(aep);
    }

    /**
     * Deleta um arquivo PDF do sistema de arquivos.
     *
     * @param pdfPath Caminho completo do arquivo PDF a ser deletado
     */
    private void deletePdfFile(String pdfPath) {
        try {
            Path path = Paths.get(pdfPath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Falha ao deletar arquivo PDF antigo: " + pdfPath);
        }
    }
}