package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.user.BatchUserInsertResponseDTO;
import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.dto.user.UserResponseDTO;
import com.gotree.API.dto.user.UserUpdateDTO;
import com.gotree.API.entities.User;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
	 * Endpoints do Admin
	 */

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponseDTO>> findAll() {
		List<User> users = userService.findAll();
		return ResponseEntity.ok(userMapper.toDtoList(users));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
		User user = userService.findById(id);
		return ResponseEntity.ok(userMapper.toDto(user));
	}

	@PostMapping("/insert")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> insertUser(@RequestBody @Valid UserRequestDTO dto) {
		User createdUser = userService.insertUser(dto);
		return ResponseEntity.ok(userMapper.toDto(createdUser));
	}

	@PostMapping("/batch")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BatchUserInsertResponseDTO> insertMultiplerUsers(
			@RequestBody List<UserRequestDTO> userDTOs) {

		BatchUserInsertResponseDTO result = userService.insertUsers(userDTOs);
		return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
	}

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
	public ResponseEntity<UserResponseDTO> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 *
	 *Endpoint Profile ADMIN e USER
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {

		// 1. O Spring Security nos dá o objeto 'Authentication', cujo 'Principal'
		//    é o nosso CustomUserDetails que configuramos.
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		// 2. A partir do CustomUserDetails, pegamos a entidade User completa.
		User loggedInUser = userDetails.getUser();

		// 3. Usamos o mapper (que já deve estar injetado no seu controller)
		//    para converter a entidade User em um DTO de resposta seguro.
		UserResponseDTO userDto = userMapper.toDto(loggedInUser);

		// 4. Retornamos o DTO com todos os dados solicitados e o status OK (200).
		return ResponseEntity.ok(userDto);
	}

}
