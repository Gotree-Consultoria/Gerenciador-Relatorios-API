package com.gotree.API.dto.user;

public class FailedUserDTO {

	private String email;
	private String reason;

	public FailedUserDTO(String email, String reason) {
		this.email = email;
		this.reason = reason;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
