package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.services.DocumentAggregationService;
import com.gotree.API.services.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador REST responsável por gerenciar o envio de documentos por e-mail.
 * Oferece endpoints para enviar diferentes tipos de relatórios (Risco Ocupacional, Visita Técnica, AEP)
 * aos clientes cadastrados no sistema.
 */
@RestController
@RequestMapping("/documents/email")
public class DocumentEmailController {

    private final DocumentAggregationService documentService;
    private final EmailService emailService;
    private final OccupationalRiskReportRepository riskRepo;
    private final AepReportRepository aepRepo;
    private final TechnicalVisitRepository visitRepo;

    /**
     * Construtor da classe DocumentEmailController.
     *
     * @param documentService Serviço para agregação e geração de documentos
     * @param emailService    Serviço para envio de e-mails
     * @param riskRepo        Repositório de relatórios de risco ocupacional
     * @param aepRepo         Repositório de relatórios AEP
     * @param visitRepo       Repositório de visitas técnicas
     */
    public DocumentEmailController(DocumentAggregationService documentService,
                                   EmailService emailService,
                                   OccupationalRiskReportRepository riskRepo,
                                   AepReportRepository aepRepo,
                                   TechnicalVisitRepository visitRepo) {
        this.documentService = documentService;
        this.emailService = emailService;
        this.riskRepo = riskRepo;
        this.aepRepo = aepRepo;
        this.visitRepo = visitRepo;
    }

    /**
     * Gera o PDF do documento solicitado, encontra o e-mail do cliente vinculado
     * e envia o arquivo como anexo. Atualiza o status de envio no banco.
     *
     * @param type Tipo do documento (risk, visit, aep)
     * @param id ID do documento
     * @param auth Usuário autenticado (necessário para gerar o PDF)
     */
    @PostMapping("/{type}/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendDocumentToClient(@PathVariable String type, @PathVariable Long id, Authentication auth) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        try {
            // 1. Gera/Carrega os bytes do PDF
            byte[] pdfBytes = documentService.loadPdfFileByTypeAndId(type, id, user);

            // Variáveis para processamento
            java.util.Set<com.gotree.API.entities.Client> clients = null;
            String companyName = "";
            String docName = type.toUpperCase() + "_" + id + ".pdf";
            String subjectType = "";

            // 2. Lógica de Seleção baseada no Tipo
            if ("risk".equalsIgnoreCase(type)) {
                OccupationalRiskReport report = riskRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Checklist de Risco não encontrado."));

                clients = report.getCompany().getClients(); // ALTERADO: getClients()
                companyName = report.getCompany().getName();
                subjectType = "Checklist de Riscos";

                report.setSentToClientAt(LocalDateTime.now());
                riskRepo.save(report);

            } else if ("visit".equalsIgnoreCase(type)) {
                TechnicalVisit visit = visitRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Relatório de Visita não encontrado."));

                clients = visit.getClientCompany().getClients(); // ALTERADO: getClients()
                companyName = visit.getClientCompany().getName();
                subjectType = "Relatório de Visita Técnica";

                visit.setSentToClientAt(LocalDateTime.now());
                visitRepo.save(visit);

            } else if ("aep".equalsIgnoreCase(type)) {
                AepReport aep = aepRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("AEP não encontrada."));

                clients = aep.getCompany().getClients(); // ALTERADO: getClients()
                companyName = aep.getCompany().getName();
                subjectType = "Avaliação Ergonômica (AEP)";

                aep.setSentToClientAt(LocalDateTime.now());
                aepRepo.save(aep);

            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Tipo de documento inválido: " + type));
            }

            // 3. Validação: Verifica se existem clientes e coleta e-mails válidos
            if (clients == null || clients.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "A empresa deste relatório não possui clientes vinculados."));
            }

            // Filtra clientes que têm e-mail preenchido
            java.util.List<String> validEmails = clients.stream()
                    .map(com.gotree.API.entities.Client::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .toList();

            if (validEmails.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Os clientes vinculados não possuem e-mail válido para envio."));
            }

            // 4. Construção do E-mail (Corpo HTML)
            String subject = "Documento Emitido: " + subjectType + " - " + companyName;
            String body = String.format(
                    "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>" +
                            "  <div style='background-color: #166534; padding: 24px; text-align: center;'>" +
                            "    <h2 style='color: #ffffff; margin: 0; font-weight: 600; font-size: 24px;'>Go-Tree Consultoria</h2>" +
                            "  </div>" +
                            "  <div style='padding: 32px 24px; color: #333333; line-height: 1.6;'>" +
                            "    <p style='font-size: 16px; margin-top: 0;'>Olá,</p>" +
                            "    <p style='font-size: 16px;'>Informamos que um novo documento técnico foi emitido e está disponível para sua análise.</p>" +
                            "    <div style='background-color: #f8f9fa; border-left: 4px solid #166534; padding: 16px; margin: 24px 0; border-radius: 4px;'>" +
                            "      <p style='margin: 4px 0;'><strong>📄 Documento:</strong> %s</p>" +
                            "      <p style='margin: 4px 0;'><strong>🏢 Empresa:</strong> %s</p>" +
                            "    </div>" +
                            "    <p style='font-size: 16px;'>O arquivo completo encontra-se em <strong>anexo (PDF)</strong> neste e-mail.</p>" +
                            "    <p style='margin-top: 32px;'>Estamos à disposição para quaisquer dúvidas.</p>" +
                            "    <p style='margin-bottom: 0;'>Atenciosamente,<br><strong>Equipe Go-Tree</strong></p>" +
                            "  </div>" +
                            "  <div style='background-color: #f4f4f4; padding: 16px; text-align: center; font-size: 12px; color: #666666; border-top: 1px solid #eeeeee;'>" +
                            "    <p style='margin: 4px 0;'>© Go-Tree Consultoria.</p>" +
                            "    <p style='margin: 4px 0;'>Este é um envio automático do nosso sistema.</p>" +
                            "  </div>" +
                            "</div>",
                    subjectType, companyName
            );

            // 5. Envio (Itera sobre a lista de e-mails válidos)
            for (String email : validEmails) {
                try {
                    emailService.sendReportWithAttachment(email, subject, body, pdfBytes, docName);
                } catch (Exception e) {
                    System.err.println("Erro ao enviar para: " + email + " - " + e.getMessage());
                    // Continua tentando enviar para os outros
                }
            }

            // Retorna sucesso listando os e-mails processados
            String allEmails = String.join(", ", validEmails);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Documento enviado com sucesso.",
                    "emails_enviados", allEmails
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar envio: " + e.getMessage()));
        }
    }
}