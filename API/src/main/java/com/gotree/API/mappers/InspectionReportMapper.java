package com.gotree.API.mappers;

import com.gotree.API.dto.report.InspectionReportResponseDTO;
import com.gotree.API.entities.InspectionReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InspectionReportMapper {

    // Ensina o MapStruct a pegar o nome da empresa de dentro do objeto Company
    @Mapping(source = "company.name", target = "companyName")
    InspectionReportResponseDTO toDto(InspectionReport entity);

    List<InspectionReportResponseDTO> toDtoList(List<InspectionReport> entities);
}