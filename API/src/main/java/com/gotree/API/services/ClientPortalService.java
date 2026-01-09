package com.gotree.API.services;

import com.gotree.API.dto.client.ClientFirstAccessRequestDTO;
import com.gotree.API.dto.client.ClientSetupPasswordDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.Client;
import com.gotree.API.entities.Company;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.ClientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClientPortalService {

    private final ClientRepository clientRepository;
    private final AgendaEventRepository agendaEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ClientPortalService(ClientRepository clientRepository,
                               AgendaEventRepository agendaEventRepository,
                               PasswordEncoder passwordEncoder, EmailService emailService) {
        this.clientRepository = clientRepository;
        this.agendaEventRepository = agendaEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Passo 1: Solicitar primeiro acesso
    @Transactional
    public void requestFirstAccess(ClientFirstAccessRequestDTO dto) {
        Client client = clientRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("E-mail não encontrado na base de clientes."));

        // Gera código de 6 caracteres
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        client.setAccessCode(code);
        client.setAccessCodeExpiration(LocalDateTime.now().plusMinutes(15));

        clientRepository.save(client);

        sendAccessCodeEmail(client.getName(), client.getEmail(), code);
    }

    private void sendAccessCodeEmail(String clientName, String email, String code) {
        String subject = "Seu Código de Acesso - Go-Tree Portal";

        String body = String.format(
                "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>" +
                        "  <div style='background-color: #166534; padding: 24px; text-align: center;'>" +
                        "    <h2 style='color: #ffffff; margin: 0; font-weight: 600; font-size: 24px;'>Go-Tree Consultoria</h2>" +
                        "  </div>" +
                        "  <div style='padding: 32px 24px; color: #333333; line-height: 1.6;'>" +
                        "    <p style='font-size: 16px; margin-top: 0;'>Olá, <strong>%s</strong>.</p>" +
                        "    <p style='font-size: 16px;'>Recebemos uma solicitação de primeiro acesso para o seu usuário.</p>" +
                        "    <div style='background-color: #f8f9fa; border-left: 4px solid #166534; padding: 16px; margin: 24px 0; border-radius: 4px; text-align: center;'>" +
                        "      <p style='margin: 0; font-size: 14px; color: #666;'>Seu código de verificação é:</p>" +
                        "      <p style='margin: 8px 0; font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #166534;'>%s</p>" +
                        "      <p style='margin: 0; font-size: 12px; color: #999;'>Válido por 15 minutos</p>" +
                        "    </div>" +
                        "    <p style='font-size: 14px;'>Copie este código e insira na tela de definição de senha para liberar seu acesso.</p>" +
                        "    <p style='margin-top: 32px; font-size: 14px; color: #666;'>Se você não solicitou este código, ignore este e-mail.</p>" +
                        "  </div>" +
                        "  <div style='background-color: #f4f4f4; padding: 16px; text-align: center; font-size: 12px; color: #666666; border-top: 1px solid #eeeeee;'>" +
                        "    <p style='margin: 4px 0;'>© Go-Tree Consultoria.</p>" +
                        "  </div>" +
                        "</div>",
                (clientName != null ? clientName : "Cliente"),
                code
        );

        // Chama o EmailService
        emailService.sendHtmlEmail(email, subject, body);
    }

    // Passo 2: Validar código e criar senha
    @Transactional
    public void setupPassword(ClientSetupPasswordDTO dto) {
        Client client = clientRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        if (client.getAccessCode() == null ||
                !client.getAccessCode().equals(dto.getAccessCode()) ||
                client.getAccessCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código inválido ou expirado.");
        }

        // Define a senha criptografada
        client.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        // Limpa o código para não ser usado novamente
        client.setAccessCode(null);
        client.setAccessCodeExpiration(null);

        clientRepository.save(client);
    }

    // Funcionalidade: Buscar agenda do cliente logado
    public List<AgendaEvent> getClientAgenda(String clientEmail) {
        // 1. Busca o cliente e suas empresas
        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        // 2. Extrai os IDs das empresas vinculadas a este cliente
        List<Long> companyIds = client.getCompanies().stream()
                .map(Company::getId)
                .toList();

        if (companyIds.isEmpty()) {
            return List.of();
        }

        // 3. Busca os eventos usando a Query ajustada
        return agendaEventRepository.findByClientCompanyIds(companyIds);
    }
}