package com.gotree.API.seed;

import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.entities.User;
import com.gotree.API.enums.UserRole;
import com.gotree.API.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;


/**
 * Esta classe é uma "seeder" temporária para o banco de dados.
 * Ela é executada ao iniciar a aplicação e cria um usuário administrador
 * se ele ainda não existir no banco de dados.
 * Os dados do usuário de teste são fixos no código.
 */
@Component
@Profile({"dev", "local"}) // Executa nos perfis de desenvolvimento ou local
public class SeedDatabaseWithAdminUser implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDatabaseWithAdminUser.class);

    private final UserService userService;

    public SeedDatabaseWithAdminUser(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        log.info("Verificando se o usuário administrador de teste já existe...");

        // Dados do usuário de teste fixos no código
        String adminEmail = "ti@gotreeconsultoria.com.br";
        String adminPassword = "gotreeteste";
        String adminName = "Administrador GoTree";
        String adminBirthDate = "1994-08-06";
        String adminPhone = "81989674212";
        String adminCpf = "048.193.645-96";


        // Tenta encontrar o usuário pelo email para evitar duplicatas
        Optional<User> existingAdmin = userService.findByEmail(adminEmail);

        if (existingAdmin.isEmpty()) {
            log.info("Usuário administrador não encontrado. Criando novo usuário...");

            UserRequestDTO adminDto = new UserRequestDTO();
            adminDto.setName(adminName);
            adminDto.setEmail(adminEmail);
            adminDto.setPassword(adminPassword);
            adminDto.setRole(UserRole.ADMIN); // Define a role como ADMIN
            adminDto.setCpf(adminCpf);
            adminDto.setPhone(adminPhone);
            adminDto.setBirthDate(LocalDate.parse(adminBirthDate)); // Transforma a string em LocalDate

            try {
                userService.insertUser(adminDto);
                log.info("Usuário administrador '{}' criado com sucesso.", adminEmail);
            } catch (Exception e) {
                log.error("Erro ao criar o usuário administrador:", e);
            }
        } else {
            log.info("Usuário administrador '{}' já existe. Nenhuma ação necessária.", adminEmail);
        }
    }
}
