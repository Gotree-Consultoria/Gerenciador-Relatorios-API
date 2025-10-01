package com.gotree.API.services;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import com.gotree.API.dto.company.CompanyRequestDTO;
import com.gotree.API.dto.company.UnitDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.Sector;
import com.gotree.API.entities.Unit;
import com.gotree.API.repositories.CompanyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Company createCompany(CompanyRequestDTO dto) {
        // É mais eficiente criar o validador uma vez
        CNPJValidator validator = new CNPJValidator();

        // Validação do CNPJ da empresa principal com Stella
        try {
            validator.assertValid(dto.getCnpj());
        } catch (InvalidStateException e) {
            // Lança uma exceção de argumento inválido
            throw new IllegalArgumentException("CNPJ da empresa principal é inválido: " + dto.getCnpj());
        }

        // Validação para garantir que a empresa sempre tenha setores
        if (dto.getSectors() == null || dto.getSectors().isEmpty()) {
            throw new IllegalArgumentException("A empresa deve ter pelo menos um setor.");
        }

        Company company = new Company();
        company.setName(dto.getName()); // Chamada única, a duplicada foi removida
        company.setCnpj(dto.getCnpj());

        // Itera sobre os DTOs das unidades para criar as entidades
        if (dto.getUnits() != null) {
            for (UnitDTO unitDto : dto.getUnits()) {
                // Valida o CNPJ da unidade APENAS SE ele não for nulo ou vazio
                if (unitDto.getCnpj() != null && !unitDto.getCnpj().trim().isEmpty()) {
                    try {
                        validator.assertValid(unitDto.getCnpj());
                    } catch (InvalidStateException e) {
                        throw new IllegalArgumentException("CNPJ da unidade '" + unitDto.getName() + "' é inválido: " + unitDto.getCnpj());
                    }
                }
                Unit unit = new Unit();
                unit.setName(unitDto.getName());

                unit.setCnpj(unitDto.getCnpj());
                unit.setCompany(company); // Associa a unidade à empresa

                // Adicionar a unidade à lista da empresa
                company.getUnits().add(unit);
            }
        }

        // Lógica para Setores (obrigatório)
        for (String sectorName : dto.getSectors()) {
            Sector sector = new Sector();
            sector.setName(sectorName);
            sector.setCompany(company);
            company.getSectors().add(sector);
        }

        //Salva a empresa (e suas unidades, por causa do Cascade) e retorna o objeto persistido
        return companyRepository.save(company);
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada com o ID: " + id)); // Use sua exceção customizada aqui
    }

    @Transactional
    public Company updateCompany(Long id, CompanyRequestDTO dto) {
        Company company = findById(id); // Reutiliza o método findById para buscar e tratar erro

        // Valida CNPJ, se for alterado
        // Atualiza os campos
        company.setName(dto.getName());
        company.setCnpj(dto.getCnpj());

        // Lógica para atualizar/adicionar/remover unidades e setores

        return companyRepository.save(company);
    }

    public void deleteCompany(Long id) {
        Company company = findById(id);
        companyRepository.delete(company);
    }
}
