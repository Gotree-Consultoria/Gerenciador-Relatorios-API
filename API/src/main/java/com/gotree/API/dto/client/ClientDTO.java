package com.gotree.API.dto.client;

import lombok.Data;
import java.util.List;

@Data
public class ClientDTO {
    private Long id;
    private String name;
    private String email;

    // Lista de IDs das empresas vinculadas (para enviar do front para o back)
    private List<Long> companyIds;

    // Lista de Nomes das empresas (para mostrar na tabela do front)
    private List<String> companyNames;
}