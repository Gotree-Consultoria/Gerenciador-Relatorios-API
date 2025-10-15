package com.gotree.API.dto.company;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnitDTO {

    private Long id;

    @NotBlank(message = "O nome da unidade é obrigatório.")
    private String name;

    // O CNPJ da unidade é opcional, então não precisa de anotação de validação aqui.
    private String cnpj;
}