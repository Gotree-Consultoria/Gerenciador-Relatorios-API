package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.LoginRequestDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador responsável por gerenciar as operações de autenticação.
 * Fornece endpoints para login e geração de tokens JWT.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {


	/**
	 * Gerenciador de autenticação do Spring Security
	 */
	private final AuthenticationManager authenticationManager;

	/**
	 * Serviço responsável pela geração e validação de tokens JWT
	 */
	private final JwtService jwtService;

	/**
	 * Autentica um usuário e gera um token JWT.
	 *
	 * @param request DTO contendo as credenciais do usuário (email e senha)
	 * @return ResponseEntity contendo o token JWT, flag de reset de senha e role do usuário
	 * @throws org.springframework.security.core.AuthenticationException se as credenciais forem inválidas
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request) {

		// Cria o "pacote de login" com o email e senha fornecidos
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getEmail(),
				request.getPassword());

		// Faz a autenticação de fato (verifica se existe, se a senha está certa, etc)
		Authentication authentication = authenticationManager.authenticate(authToken);

		// Após autenticar pega os dados do usuário
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		User user = userDetails.user();

		System.out.println(user.getPasswordResetRequired());

		// Gera o token jwt com base nesse usuário

		String jwt = jwtService.generateToken(userDetails);

		return ResponseEntity.ok(Map.of(
				"token", jwt,
				"passwordResetRequired", user.getPasswordResetRequired(),
				"role", user.getRole()
		));
	}

}