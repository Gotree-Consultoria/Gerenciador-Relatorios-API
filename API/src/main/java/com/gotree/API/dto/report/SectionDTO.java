package com.gotree.API.dto.report;

import lombok.Data;
import java.util.List;

@Data
public class SectionDTO {
    private String title;
    private boolean na; // Flag para a seção inteira
    private List<ChecklistItemDTO> items;
}
