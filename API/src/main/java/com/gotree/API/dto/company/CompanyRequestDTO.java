package com.gotree.API.dto.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CompanyRequestDTO {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    private String name;

    @NotBlank(message = "O CNPJ da empresa é obrigatório.")
    private String cnpj;

    @Valid // Habilita a validação para os objetos DENTRO da lista de unidades
    private List<UnitDTO> units;

    private List<String> sectors;
}