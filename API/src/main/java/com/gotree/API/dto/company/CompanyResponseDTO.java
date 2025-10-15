package com.gotree.API.dto.company;

import lombok.Data;

import java.util.List;

@Data
public class CompanyResponseDTO {

    private Long id;
    private String name;
    private String cnpj;
    private List<UnitDTO> units;
    private List<SectorResponseDTO> sectors;
}
