package com.gotree.API.dto.report;

import com.gotree.API.entities.enums.DocumentType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class InspectionReportResponseDTO {
    private Long id;
    private String title;
    private DocumentType type;
    private LocalDate inspectionDate;
    private String companyName;
    private boolean digitallySigned;
}