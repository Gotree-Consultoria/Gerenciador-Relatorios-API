package com.gotree.API.controllers;

import com.gotree.API.config.security.ClientUserDetails;
import com.gotree.API.dto.auth.AuthenticationResponseDTO;
import com.gotree.API.dto.client.ClientFirstAccessRequestDTO;
import com.gotree.API.dto.client.ClientLoginDTO;
import com.gotree.API.dto.client.ClientSetupPasswordDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.repositories.ClientRepository;
import com.gotree.API.services.ClientPortalService;
import com.gotree.API.services.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client-portal")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ClientRepository clientRepository;

    public ClientPortalController(ClientPortalService clientPortalService, AuthenticationManager authenticationManager,
                                  JwtService jwtService, ClientRepository clientRepository) {
        this.clientPortalService = clientPortalService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clientRepository = clientRepository;
    }

    /**
     * Passo 1: Cliente informa e-mail.
     * Se existir, gera código e envia por e-mail.
     */
    @PostMapping("/first-access/request")
    public ResponseEntity<Void> requestAccess(@RequestBody @Valid ClientFirstAccessRequestDTO dto) {
        clientPortalService.requestFirstAccess(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Passo 2: Cliente informa código e nova senha.
     * Sistema salva a senha.
     */
    @PostMapping("/first-access/setup")
    public ResponseEntity<Void> setupPassword(@RequestBody @Valid ClientSetupPasswordDTO dto) {
        clientPortalService.setupPassword(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Busca agenda das empresas vinculadas ao cliente logado.
     * Requer que o cliente já esteja autenticado via JWT (login normal).
     */
    @GetMapping("/agenda")
    public ResponseEntity<List<AgendaEvent>> getMyAgenda(Authentication authentication) {
        // Assume que o authentication.getName() retorna o email do cliente (configurado no token JWT)
        String email = authentication.getName();
        List<AgendaEvent> events = clientPortalService.getClientAgenda(email);
        return ResponseEntity.ok(events);
    }

    /**
     * Enpoint para login do cliente ao portal
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@RequestBody @Valid ClientLoginDTO dto) {
        // 1. O AuthenticationManager usa o UserService.loadUserByUsername()
        // Como atualizamos o UserService, ele vai encontrar o cliente e validar a senha
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        // 2. Busca o cliente para passar para o gerador de token
        var client = clientRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        // 3. Gera o token
        // O ClientUserDetails implementa UserDetails, então o JwtService deve aceitá-lo
        var jwtToken = jwtService.generateToken(new ClientUserDetails(client));

        return ResponseEntity.ok(new AuthenticationResponseDTO(jwtToken));
    }

    // TODO: endpoint para cliente alterar a senha
}