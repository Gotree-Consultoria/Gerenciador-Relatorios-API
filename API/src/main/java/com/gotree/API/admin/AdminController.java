package com.gotree.API.admin;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dev-tools")
@Profile("test")
public class AdminController {

	private final DataBaseMaintenanceService maintenanceService;

	public AdminController(DataBaseMaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;

	}

	// http://localhost:8080/admin/dev-tools/users?token=DELETE_TEST_DATA
	@DeleteMapping("/users")
	public ResponseEntity<String> deleteAllUsers(@RequestParam String token) {
		maintenanceService.deleteAllUser(token);
		return ResponseEntity.ok("✅ Todos os usuários de teste foram apagados com sucesso.");
	}

}
