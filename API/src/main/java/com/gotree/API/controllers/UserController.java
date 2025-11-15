package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.user.BatchUserInsertResponseDTO;
import com.gotree.API.dto.user.ChangePasswordRequestDTO;
import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.dto.user.UserResponseDTO;
import com.gotree.API.dto.user.UserUpdateDTO;
import com.gotree.API.entities.User;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.UserMapper;
import com.gotree.API.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/users")
public class UserController {
	
	


	private final UserService userService;
	private final UserMapper userMapper;

	public UserController(UserService userService, UserMapper userMapper) {
		this.userService = userService;
		this.userMapper = userMapper;
	}

	/**
	 * Lista todos os usuários do sistema.
	 * Acesso restrito a administradores.
	 *
	 * @return ResponseEntity contendo lista de UserResponseDTO com todos os usuários
	 */
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponseDTO>> findAll() {
		List<User> users = userService.findAll();
		return ResponseEntity.ok(userMapper.toDtoList(users));
	}

	/**
	 * Busca um usuário específico por ID.
	 * Acesso restrito a administradores.
	 *
	 * @param id ID do usuário a ser buscado
	 * @return ResponseEntity contendo UserResponseDTO com dados do usuário
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
		User user = userService.findById(id);
		return ResponseEntity.ok(userMapper.toDto(user));
	}

	/**
	 * Cria um novo usuário no sistema.
	 * Acesso restrito a administradores.
	 *
	 * @param dto UserRequestDTO contendo dados do novo usuário
	 * @return ResponseEntity contendo UserResponseDTO do usuário criado
	 */
	@PostMapping("/insert")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> insertUser(@RequestBody @Valid UserRequestDTO dto) {
		User createdUser = userService.insertUser(dto);
		return ResponseEntity.ok(userMapper.toDto(createdUser));
	}

	/**
	 * Cria múltiplos usuários em lote.
	 * Acesso restrito a administradores.
	 *
	 * @param userDTOs Lista de UserRequestDTO contendo dados dos novos usuários
	 * @return ResponseEntity contendo BatchUserInsertResponseDTO com resultado da operação
	 */
	@PostMapping("/batch")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BatchUserInsertResponseDTO> insertMultiplerUsers(
			@RequestBody List<UserRequestDTO> userDTOs) {

		BatchUserInsertResponseDTO result = userService.insertUsers(userDTOs);
		return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
	}

	/**
	 * Atualiza dados de um usuário existente.
	 * Acesso restrito a administradores.
	 *
	 * @param id  ID do usuário a ser atualizado
	 * @param dto UserUpdateDTO contendo novos dados
	 * @return ResponseEntity contendo UserResponseDTO com dados atualizados
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody @Valid UserUpdateDTO dto) {
		User updateUser = userService.updateUser(id, dto);
		return ResponseEntity.ok(userMapper.toDto(updateUser));
	}

	@PutMapping("/admin/reset-password/{id}")
	@PreAuthorize("hasRole('ADMIN')") // garante que só admin pode chamar
	public ResponseEntity<?> resetPassword(@PathVariable Long id) {
		userService.resetPassword(id);
		return ResponseEntity
				.ok(Map.of("message", "Senha redefinida com sucesso e campo passwordResetRequired Ativado"));

	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteUser(@PathVariable Long id) {
		try {
			userService.deleteUser(id);
			// 204 No Content: Sucesso
			return ResponseEntity.noContent().build();

		} catch (IllegalStateException e) {
			// 409 Conflict: A regra de negócio (relatório em uso) impediu
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));

		} catch (ResourceNotFoundException e) {
			// 404 Not Found: O usuário não existia
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
		}
	}

	/**
	 * Retorna dados do usuário atualmente autenticado.
	 * Disponível para todos os usuários autenticados.
	 *
	 * @param authentication Objeto de autenticação do Spring Security
	 * @return ResponseEntity contendo UserResponseDTO com dados do usuário logado
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {

		// 1. O Spring Security nos dá o objeto 'Authentication', cujo 'Principal'
		//    é o nosso CustomUserDetails que configuramos.
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		// 2. A partir do CustomUserDetails, pegamos a entidade User completa.
		User loggedInUser = userDetails.user();

		// 3. Usamos o mapper (que já deve estar injetado no seu controller)
		//    para converter a entidade User em um DTO de resposta seguro.
		UserResponseDTO userDto = userMapper.toDto(loggedInUser);

		// 4. Retornamos o DTO com todos os dados solicitados e o status OK (200).
		return ResponseEntity.ok(userDto);
	}


	/**
	 * Altera a senha do usuário atualmente autenticado.
	 * Disponível para todos os usuários autenticados.
	 *
	 * @param authentication Objeto de autenticação do Spring Security
	 * @param dto            ChangePasswordRequestDTO contendo a nova senha
	 * @return ResponseEntity com mensagem de sucesso
	 */
	@PutMapping("/me/change-password")
	@PreAuthorize("isAuthenticated()") // Garante que o utilizador esteja logado
	public ResponseEntity<?> changePassword(Authentication authentication,
											@Valid @RequestBody ChangePasswordRequestDTO dto) {
		// Pega o e-mail do utilizador autenticado de forma segura
		String userEmail = authentication.getName();

		userService.changePassword(userEmail, dto.getNewPassword());

		return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
	}

}
