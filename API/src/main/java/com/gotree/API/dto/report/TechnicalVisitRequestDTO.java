package com.gotree.API.dto.report;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TechnicalVisitRequestDTO {
    private Long companyId;
    private Long unitId;
    private LocalDate visitDate;
    private String summary;
    private List<PhotoRecordDTO> photoRecords;
}