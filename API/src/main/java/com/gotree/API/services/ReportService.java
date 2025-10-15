package com.gotree.API.services;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
// CORREÇÃO: Altere o import do Context
import org.thymeleaf.context.Context; // <-- Importação Correta
import org.xhtmlrenderer.pdf.ITextRenderer;

// import javax.naming.Context; // <-- REMOVA ESTA LINHA

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class ReportService {

    private final TemplateEngine templateEngine;

    public ReportService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        // Agora esta linha usará a classe Context correta do Thymeleaf
        Context context = new Context();
        context.setVariables(data);

        // 2. Processa o template HTML com os dados
        String htmlContent = templateEngine.process(templateName, context);

        // 3. Usa o Flying Saucer para renderizar o HTML como PDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            // Lide com a exceção apropriadamente
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }
}
