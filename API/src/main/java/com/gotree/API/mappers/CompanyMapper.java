package com.gotree.API.mappers;

import com.gotree.API.dto.company.CompanyRequestDTO;
import com.gotree.API.dto.company.CompanyResponseDTO;
import com.gotree.API.dto.company.SectorResponseDTO;
import com.gotree.API.dto.company.UnitDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.Sector;
import com.gotree.API.entities.Unit;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    // 1. Ignoramos as listas no mapeamento principal
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "units", ignore = true)
    @Mapping(target = "sectors", ignore = true)
    Company toEntity(CompanyRequestDTO dto);

    // Para a conversão de volta para DTO, podemos ensinar o MapStruct
    // a converter as listas de entidades em listas de DTOs/Strings.
    @Mapping(source = "units", target = "units")
    @Mapping(source = "sectors", target = "sectors")
    CompanyResponseDTO toDto(Company company);

    List<CompanyResponseDTO> toDtoList(List<Company> companies);

    // Métodos auxiliares para a conversão de DTO
    default List<UnitDTO> mapUnitsToUnitDTOs(Set<Unit> units) {
        if (units == null) {
            return null;
        }
        return units.stream().map(unit -> {
            UnitDTO dto = new UnitDTO();
            dto.setId(unit.getId());
            dto.setName(unit.getName());
            dto.setCnpj(unit.getCnpj());
            return dto;
        }).collect(Collectors.toList());
    }

    default List<SectorResponseDTO> mapSectorsToSectorResponseDTOs(Set<Sector> sectors) {
        if (sectors == null) {
            return null;
        }
        return sectors.stream().map(sector -> {
            SectorResponseDTO dto = new SectorResponseDTO();
            dto.setId(sector.getId());
            dto.setName(sector.getName());
            return dto;
        }).collect(Collectors.toList());
    }


    // 2. Criamos um método que será executado APÓS o mapeamento de toEntity
    @AfterMapping
    default void mapUnitsAndSectors(@MappingTarget Company company, CompanyRequestDTO dto) {
        // Lógica para Unidades
        if (dto.getUnits() != null) {
            company.getUnits().clear();
            for (UnitDTO unitDto : dto.getUnits()) {
                Unit unit = new Unit();
                unit.setName(unitDto.getName());
                unit.setCnpj(unitDto.getCnpj());
                unit.setCompany(company); // Associa a unidade à empresa (essencial!)
                company.getUnits().add(unit);
            }
        }

        // Lógica para Setores
        if (dto.getSectors() != null) {
            company.getSectors().clear();
            for (String sectorName : dto.getSectors()) {
                Sector sector = new Sector();
                sector.setName(sectorName);
                sector.setCompany(company); // Associa o setor à empresa (essencial!)
                company.getSectors().add(sector);
            }
        }
    }
}