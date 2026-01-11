package com.gotree.API.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReportWithAttachment(String to, String subject, String body, byte[] pdfBytes, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("apoio@gotreeconsultoria.com.br");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            // Anexa o PDF (ByteArrayResource é importante aqui)
            helper.addAttachment(filename, new ByteArrayResource(pdfBytes));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage());
        }
    }


    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = ativa HTML
            helper.setFrom("apoio@gotreeconsultoria.com.br"); // Se precisar forçar o remetente

            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail HTML: " + e.getMessage());
        }
    }
}
