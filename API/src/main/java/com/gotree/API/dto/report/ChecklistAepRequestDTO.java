package com.gotree.API.dto.report;

import lombok.Data;
import java.util.List;

@Data
public class ChecklistAepRequestDTO {
    private List<ChecklistItemDTO> items;
}