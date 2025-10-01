package com.gotree.API.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

	private Long id;
	private String name;
	private String email;
	private String phone;
	private String role;
	private String siglaConselhoClasse;
	private String conselhoClasse;
	private String especialidade;

	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate birthDate;
	private String cpf;

}