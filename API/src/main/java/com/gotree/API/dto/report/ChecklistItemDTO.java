package com.gotree.API.dto.report;

import lombok.Data;

@Data
public class ChecklistItemDTO {
    private boolean checked;
    private String description;
    private boolean na; // Flag para o item individual
}