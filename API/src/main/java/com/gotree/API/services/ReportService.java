package com.gotree.API.services;

import com.gotree.API.entities.SystemInfo;
import com.gotree.API.repositories.SystemInfoRepository;
import com.lowagie.text.pdf.BaseFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private final TemplateEngine templateEngine;
    private final SystemInfoRepository systemInfoRepository;

    public ReportService(TemplateEngine templateEngine, SystemInfoRepository systemInfoRepository) {
        this.templateEngine = templateEngine;
        this.systemInfoRepository = systemInfoRepository;
    }

    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        // 1. INJEÇÃO AUTOMÁTICA: Garante que a logo e dados da empresa estejam no mapa
        enrichDataWithSystemInfo(data);

        Context context = new Context();
        context.setVariables(data);

        logger.info("Gerando HTML para o template: {}", templateName);
        String htmlContent = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // Configuração da Fonte Montserrat (Mantida do seu código)
            try {
                URL fontUrl = getClass().getResource("/fonts/Montserrat.ttf");
                if (fontUrl != null) {
                    renderer.getFontResolver().addFont(fontUrl.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception e) {
                logger.error("Aviso: Fonte Montserrat não carregada.", e);
            }

            // BaseURI para recursos locais (caso ainda use alguma imagem estática)
            String baseUri = new File(".").toURI().toString();
            renderer.setDocumentFromString(htmlContent, baseUri);

            renderer.layout();
            renderer.createPDF(outputStream);

            logger.info("PDF gerado com sucesso.");
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("==== FALHA CRÍTICA NA GERAÇÃO DO PDF ====", e);
            throw new RuntimeException("Erro ao renderizar o PDF.", e);
        }
    }

    /**
     * Busca os dados da empresa no banco e injeta no mapa se não existirem.
     */
    private void enrichDataWithSystemInfo(Map<String, Object> data) {
        try {
            // Tenta pegar do banco
            SystemInfo info = systemInfoRepository.findFirst();

            if (info != null) {
                data.putIfAbsent("generatingCompanyName", info.getCompanyName());
                data.putIfAbsent("generatingCompanyCnpj", info.getCnpj());

                if (info.getLogoBase64() != null && !info.getLogoBase64().isBlank()) {
                    // Remove prefixo se existir, para padronizar
                    String cleanBase64 = stripDataUrlPrefix(info.getLogoBase64());
                    data.put("generatingCompanyLogo", cleanBase64);
                }
            } else {
                // FALLBACK: Se o banco estiver vazio (primeira execução), tenta carregar do arquivo estático
                // Isso atende seu desejo de usar o static como backup
                logger.warn("SystemInfo não encontrado no banco. Tentando logo padrão estática.");
                loadStaticLogoFallback(data);
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar dados do sistema para o relatório", e);
        }
    }

    /**
     * Fallback: Tenta ler src/main/resources/static/img/logo.png e converter para Base64
     * Isso garante que funcione mesmo se o banco estiver vazio.
     */
    private void loadStaticLogoFallback(Map<String, Object> data) {
        try {
            ClassPathResource resource = new ClassPathResource("static/img/logo.png");
            if (resource.exists()) {
                byte[] imageBytes = resource.getInputStream().readAllBytes();
                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                data.put("generatingCompanyLogo", base64);

                // Valores padrão se não tiver no banco
                data.putIfAbsent("generatingCompanyName", "Minha Empresa (Padrão)");
                data.putIfAbsent("generatingCompanyCnpj", "00.000.000/0001-00");
            }
        } catch (IOException e) {
            logger.warn("Logo estática padrão não encontrada.");
        }
    }

    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl == null) return null;
        int commaIndex = dataUrl.indexOf(',');
        return commaIndex != -1 ? dataUrl.substring(commaIndex + 1) : dataUrl;
    }
}