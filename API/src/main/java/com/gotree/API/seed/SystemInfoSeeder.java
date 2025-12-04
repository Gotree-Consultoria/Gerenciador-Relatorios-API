package com.gotree.API.seed;

import com.gotree.API.entities.SystemInfo;
import com.gotree.API.repositories.SystemInfoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.util.Base64;

@Component
public class SystemInfoSeeder implements CommandLineRunner {

    private final SystemInfoRepository systemInfoRepository;

    public SystemInfoSeeder(SystemInfoRepository systemInfoRepository) {
        this.systemInfoRepository = systemInfoRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Verifica se já existe registro. Se existir, não faz nada.
        if (systemInfoRepository.count() > 0) {
            System.out.println(">> SystemInfo já existe no banco. Ppulando seed.");
            return;
        }

        System.out.println(">> Criando registro inicial de SystemInfo...");

        SystemInfo info = new SystemInfo();
        info.setCompanyName("Go-Tree Consultoria LTDA"); // Seu Nome Padrão
        info.setCnpj("47.885.556/0001-76");              // Seu CNPJ Padrão

        // 2. Tenta carregar a logo do arquivo estático para salvar no banco como Base64
        try {
            ClassPathResource imageResource = new ClassPathResource("static/img/logo.png");
            if (imageResource.exists()) {
                byte[] imageBytes = StreamUtils.copyToByteArray(imageResource.getInputStream());
                String base64Logo = Base64.getEncoder().encodeToString(imageBytes);

                // Salva no formato puro (sem o prefixo data:image, pois o template adiciona)
                info.setLogoBase64(base64Logo);
                System.out.println(">> Logo carregada de static/img/logo.png e salva no banco.");
            } else {
                System.err.println(">> AVISO: Logo não encontrada em static/img/logo.png. Salvando sem logo.");
            }
        } catch (Exception e) {
            System.err.println(">> Erro ao converter logo para base64: " + e.getMessage());
        }

        // 3. Salva no Banco
        systemInfoRepository.save(info);
        System.out.println(">> SystemInfo inicializado com sucesso!");
    }
}
