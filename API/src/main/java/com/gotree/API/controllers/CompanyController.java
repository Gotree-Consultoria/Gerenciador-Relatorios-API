package com.gotree.API.controllers;

import com.gotree.API.dto.company.CompanyRequestDTO;
import com.gotree.API.dto.company.CompanyResponseDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.mappers.CompanyMapper;
import com.gotree.API.services.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies")
// Protege todos os endpoints deste controller, permitindo acesso apenas para ADMIN
@PreAuthorize("hasRole('ADMIN')")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyMapper companyMapper;

    // Injeção do Service e do Mapper via construtor
    public CompanyController(CompanyService companyService, CompanyMapper companyMapper) {
        this.companyService = companyService;
        this.companyMapper = companyMapper;
    }

    /**
     * Endpoint para criar uma nova empresa.
     * Recebe um DTO, passa para o serviço e retorna o DTO da empresa criada.
     */
    @PostMapping
    public ResponseEntity<CompanyResponseDTO> createCompany(@Valid @RequestBody CompanyRequestDTO dto) {
        Company newCompany = companyService.createCompany(dto);
        CompanyResponseDTO responseDto = companyMapper.toDto(newCompany);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Endpoint para listar todas as empresas cadastradas.
     */
    @GetMapping
    public ResponseEntity<List<CompanyResponseDTO>> findAll() {
        List<Company> companies = companyService.findAll();
        List<CompanyResponseDTO> responseDtos = companyMapper.toDtoList(companies);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Endpoint para buscar uma empresa específica pelo seu ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> findById(@PathVariable Long id) {
        Company company = companyService.findById(id);
        CompanyResponseDTO responseDto = companyMapper.toDto(company);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Endpoint para atualizar os dados de uma empresa.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequestDTO dto) {
        Company updatedCompany = companyService.updateCompany(id, dto);
        CompanyResponseDTO responseDto = companyMapper.toDto(updatedCompany);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Endpoint para deletar uma empresa.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build(); // Retorna status 204 No Content
    }
}