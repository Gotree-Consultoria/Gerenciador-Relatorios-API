package com.gotree.API.dto.user;

import com.gotree.API.entities.User;
import com.gotree.API.entities.enums.UserRole;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapperDTO {

	/**
	 * Converte o DTO de requisição em uma Entidade User.
	 * Assume que os novos campos existem em UserRequestDTO.
	 */
	public static User toEntity(UserRequestDTO dto) {
		User user = new User();
		user.setName(dto.getName());
		user.setEmail(dto.getEmail());
		user.setPassword(dto.getPassword());
		user.setBirthDate(dto.getBirthDate());
		user.setPhone(dto.getPhone());
		user.setCpf(dto.getCpf());
		user.setRole(UserRole.USER); // Define a role padrão para novos usuários
		user.setPasswordResetRequired(false);

		// Mapeando os novos campos
		user.setSiglaConselhoClasse(dto.getSiglaConselhoClasse());
		user.setConselhoClasse(dto.getConselhoClasse());
		user.setEspecialidade(dto.getEspecialidade());

		return user;
	}

	/**
	 * Converte uma Entidade User em um DTO de resposta.
	 * Assume que os novos campos existem em UserResponseDTO.
	 */
	public static UserResponseDTO toDto(User user) {
		UserResponseDTO dto = new UserResponseDTO();
		dto.setId(user.getId());
		dto.setName(user.getName());
		dto.setEmail(user.getEmail());
		dto.setPhone(user.getPhone());
		dto.setRole(user.getRole().getRoleName());

		// Mapeando os novos campos
		dto.setSiglaConselhoClasse(user.getSiglaConselhoClasse());
		dto.setConselhoClasse(user.getConselhoClasse());
		dto.setEspecialidade(user.getEspecialidade());

		return dto;
	}

	/**
	 * Converte uma lista de Entidades User em uma lista de DTOs de resposta.
	 * Este método não precisa de alteração, pois ele reutiliza o toDto().
	 */
	public static List<UserResponseDTO> toDtoList(List<User> users) {
		return users.stream().map(UserMapperDTO::toDto).collect(Collectors.toList());
	}
}