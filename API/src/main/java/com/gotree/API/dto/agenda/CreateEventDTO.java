package com.gotree.API.dto.agenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateEventDTO {
    private String title;
    private String description;

    @NotNull(message = "A data é obrigatória.")
    private LocalDate eventDate;

    @NotBlank(message = "O tipo de evento é obrigatório.")
    private String eventType;
}