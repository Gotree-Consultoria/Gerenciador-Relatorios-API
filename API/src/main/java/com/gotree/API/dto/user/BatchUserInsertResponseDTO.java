package com.gotree.API.dto.user;

import java.util.List;

public class BatchUserInsertResponseDTO {

	private List<UserResponseDTO> successUsers;
	private List<FailedUserDTO> failedUsers;

	public BatchUserInsertResponseDTO(List<UserResponseDTO> successUsers, List<FailedUserDTO> failedUsers) {
		super();
		this.successUsers = successUsers;
		this.failedUsers = failedUsers;
	}

	public List<UserResponseDTO> getSuccessUsers() {
		return successUsers;
	}

	public void setSuccessUsers(List<UserResponseDTO> successUsers) {
		this.successUsers = successUsers;
	}

	public List<FailedUserDTO> getFailedUsers() {
		return failedUsers;
	}

	public void setFailedUsers(List<FailedUserDTO> failedUsers) {
		this.failedUsers = failedUsers;
	}

}
