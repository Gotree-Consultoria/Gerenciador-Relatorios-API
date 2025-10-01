package com.gotree.API.dto.user;

import jakarta.validation.constraints.Size;

public class UserUpdateDTO {

	@Size(min = 8, max = 50, message = "Seu nome deve ter entre 3 e 50 caracteres")
	private String name;
	@Size(min = 8, max = 20, message = "Telefone deve ter entre 8 e 20 caracteres")
	private String phone;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}
