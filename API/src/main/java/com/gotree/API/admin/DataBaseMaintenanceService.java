package com.gotree.API.admin;

import com.gotree.API.repositories.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@Profile("test")
public class DataBaseMaintenanceService {

	private final String token = "DELETE_TEST_DATA";
	private final UserRepository userRepository;
	private final DataSource dataSource;

	public DataBaseMaintenanceService(UserRepository userRepository, DataSource dataSource) {
		this.userRepository = userRepository;
		this.dataSource = dataSource;
	}

	public void deleteAllUser(String token) {
		if (!token.equals(token)) {
			throw new SecurityException("Token inválido");
		}

		String dataBaseName = getDatabaseName();

		if (!"docgenTest".equalsIgnoreCase(dataBaseName)) {
			throw new SecurityException("Limpeza permitida apenas no banco docgenTest. Banco atual: " + dataBaseName);
		}

		userRepository.deleteAll();
	}

	// Pega nome do banco de dados de acordo com a conexão.
	public String getDatabaseName() {
		try (var connection = dataSource.getConnection()) {
			return connection.getCatalog();
		} catch (Exception e) {
			throw new RuntimeException("Erro ao obter o nome do banco de dados", e);
		}
	}

	public String getToken() {
		return token;
	}

}
