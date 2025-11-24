package com.gotree.API.controllers;

import com.gotree.API.dto.company.CompanyRequestDTO;
import com.gotree.API.dto.company.CompanyResponseDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.CompanyMapper;
import com.gotree.API.services.CompanyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsável pelo gerenciamento de empresas.
 * Fornece endpoints para operações CRUD em empresas.
 * <p>
 * Base URL: /companies
 */
@RestController
@RequestMapping("/companies")
public class CompanyController {
    
    

    private final CompanyService companyService;
    private final CompanyMapper companyMapper;

    // Injeção do Service e do Mapper via construtor
    public CompanyController(CompanyService companyService, CompanyMapper companyMapper) {
        this.companyService = companyService;
        this.companyMapper = companyMapper;
    }

    /**
     * Cria uma nova empresa no sistema.
     *
     * @param dto Objeto DTO contendo os dados da empresa a ser criada
     * @return ResponseEntity com o DTO da empresa criada e status HTTP 201 (CREATED)
     * @throws IllegalArgumentException se os dados fornecidos forem inválidos
     * @secured Requer papel ADMIN
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponseDTO> createCompany(@Valid @RequestBody CompanyRequestDTO dto) {
        Company newCompany = companyService.createCompany(dto);
        CompanyResponseDTO responseDto = companyMapper.toDto(newCompany);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

//    /**
//     * Lista todas as empresas cadastradas no sistema.
//     *
//     * @return ResponseEntity com a lista de DTOs das empresas e status HTTP 200 (OK)
//     * @secured Requer autenticação
//     */
//    @GetMapping
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<List<CompanyResponseDTO>> findAll() {
//        List<Company> companies = companyService.findAll();
//        List<CompanyResponseDTO> responseDtos = companyMapper.toDtoList(companies);
//        return ResponseEntity.ok(responseDtos);
//    }

    /**
     * Busca uma empresa específica pelo seu ID.
     *
     * @param id ID da empresa a ser buscada
     * @return ResponseEntity com o DTO da empresa encontrada e status HTTP 200 (OK)
     * @throws ResourceNotFoundException se a empresa não for encontrada
     * @secured Requer autenticação
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponseDTO> findById(@PathVariable Long id) {
        Company company = companyService.findById(id);
        CompanyResponseDTO responseDto = companyMapper.toDto(company);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Lista todas as empresas com paginação.
     * Exemplo de chamada: GET /companies?page=0&size=10&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<Page<CompanyResponseDTO>> getAll(
                                                            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        // O serviço agora retorna Page<CompanyResponseDTO>
        Page<CompanyResponseDTO> companies = companyService.findAllPaginated(pageable);

        return ResponseEntity.ok(companies);
    }

    /**
     * Atualiza os dados de uma empresa existente.
     *
     * @param id  ID da empresa a ser atualizada
     * @param dto Objeto DTO contendo os novos dados da empresa
     * @return ResponseEntity com o DTO da empresa atualizada e status HTTP 200 (OK)
     * @throws ResourceNotFoundException se a empresa não for encontrada
     * @throws IllegalArgumentException  se os dados fornecidos forem inválidos
     * @secured Requer papel ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponseDTO> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequestDTO dto) {
        Company updatedCompany = companyService.updateCompany(id, dto);
        CompanyResponseDTO responseDto = companyMapper.toDto(updatedCompany);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Remove uma empresa do sistema.
     *
     * @param id ID da empresa a ser removida
     * @return ResponseEntity com status HTTP 204 (NO_CONTENT) ou um erro 404/409
     * @secured Requer papel ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        try {
            companyService.deleteCompany(id);
            // 204 No Content: Sucesso, sem corpo de resposta
            return ResponseEntity.noContent().build();

        } catch (IllegalStateException e) {
            // 409 Conflict: A regra de negócio (relatório em uso) impediu
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            // 404 Not Found: A empresa não existia
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}