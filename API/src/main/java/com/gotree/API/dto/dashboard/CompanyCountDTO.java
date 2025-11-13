package com.gotree.API.dto.dashboard;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class CompanyCountDTO {
    private String companyName;
    private long documentCount;
}