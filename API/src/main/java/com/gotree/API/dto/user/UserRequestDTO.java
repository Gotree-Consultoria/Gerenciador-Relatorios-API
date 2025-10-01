package com.gotree.API.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gotree.API.entities.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequestDTO {

	@NotBlank(message = "Seu Nome é obrigatório")
	private String name;

	@NotBlank(message = "Seu email é obrigatório")
	private String email;

	@NotBlank(message = "Sua senha é obrigatório")
	@Size(min = 8, message = "Sua senha deve ter no minimo 8 caracteres")
	private String password;

	@NotNull(message = "Sua idade é obrigatória")
	@Past(message = "A data de nascimento deve ser no passado")
	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate birthDate;

	@Size(min = 8, max = 20, message = "Telefone deve ter entre 8 e 20 caracteres")
	@NotBlank(message = "Seu telefone é obrigatório")
	private String phone;

	@NotBlank
	private String cpf;

	private UserRole role;

	private String siglaConselhoClasse;
	private String conselhoClasse;
	private String especialidade;

}