package com.gotree.API.services;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import com.gotree.API.dto.company.CompanyRequestDTO;
import com.gotree.API.dto.company.UnitDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.Sector;
import com.gotree.API.entities.Unit;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.CompanyRepository;
import com.gotree.API.repositories.JobRoleRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set; // Import necessário
import java.util.stream.Collectors; // Import necessário

/**
 * Serviço responsável por gerenciar operações relacionadas a empresas.
 * Fornece funcionalidades para criar, buscar, atualizar e deletar empresas,
 * além de gerenciar suas unidades e setores.
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    private final OccupationalRiskReportRepository riskReportRepository;
    private final AepReportRepository aepReportRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final JobRoleRepository jobRoleRepository;

    /**
     * Construtor do serviço de empresas.
     *
     * @param companyRepository repositório para operações de persistência de empresas
     */
    public CompanyService(CompanyRepository companyRepository,
                          OccupationalRiskReportRepository riskReportRepository,
                          AepReportRepository aepReportRepository,
                          TechnicalVisitRepository technicalVisitRepository,
                          JobRoleRepository jobRoleRepository) {
        this.companyRepository = companyRepository;
        this.riskReportRepository = riskReportRepository;
        this.aepReportRepository = aepReportRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.jobRoleRepository = jobRoleRepository;
    }

    /**
     * Cria uma nova empresa com suas unidades e setores.
     *
     * @param dto objeto contendo os dados da empresa a ser criada
     * @return a empresa criada e persistida
     * @throws IllegalArgumentException se o CNPJ for inválido ou se não houver setores definidos
     */
    @Transactional
    public Company createCompany(CompanyRequestDTO dto) {
        CNPJValidator validator = new CNPJValidator();

        // 1. Validação do CNPJ principal
        try {
            validator.assertValid(dto.getCnpj());
        } catch (InvalidStateException e) {
            throw new IllegalArgumentException("CNPJ da empresa principal é inválido: " + dto.getCnpj());
        }

        // 2. Cria a entidade
        Company company = new Company();
        company.setName(dto.getName());
        company.setCnpj(dto.getCnpj());

        // 3. Usa os helpers para mapear as coleções
        mapUnitsToCompany(company, dto.getUnits(), validator);
        mapSectorsToCompany(company, dto.getSectors());

        // 4. Salva a nova empresa
        return companyRepository.save(company);
    }

    /**
     * Retorna todas as empresas cadastradas.
     *
     * @return lista com todas as empresas
     */
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    /**
     * Busca uma empresa pelo seu ID.
     *
     * @param id identificador da empresa
     * @return a empresa encontrada
     * @throws RuntimeException se a empresa não for encontrada
     */
    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada com o ID: " + id)); // Use sua exceção customizada aqui
    }

    /**
     * Atualiza os dados de uma empresa existente.
     *
     * @param id  identificador da empresa a ser atualizada
     * @param dto objeto contendo os novos dados da empresa
     * @return a empresa atualizada
     * @throws RuntimeException se a empresa não for encontrada
     */
    @Transactional
    public Company updateCompany(Long id, CompanyRequestDTO dto) {
        // 1. Busca a empresa existente
        Company company = findById(id);
        CNPJValidator validator = new CNPJValidator();

        // 2. Valida o CNPJ principal
        validateCnpj(dto.getCnpj(), "principal", validator);

        // 3. Atualiza os campos simples
        company.setName(dto.getName());
        company.setCnpj(dto.getCnpj());

        // 4. Reutiliza os helpers para ATUALIZAR as coleções
        mergeUnits(company, dto.getUnits(), validator);
        mergeSectors(company, dto.getSectors());

        // 5. Salva as alterações
        // O @Transactional já faria o commit, mas o save() é explícito
        return companyRepository.save(company);
    }

    // --- MÉTODOS PRIVADOS (HELPERS) ---

    /**
     * Helper de MERGE para Unidades (Usado pelo updateCompany)
     * Compara a lista do DTO com a do banco e faz Inserts, Updates ou Deletes.
     */
    private void mergeUnits(Company company, List<UnitDTO> unitDtos, CNPJValidator validator) {
        // 1. Converte DTOs em um Set de entidades (com ID nulo)
        Set<Unit> unitsFromDto = mapUnitDtosToEntities(unitDtos, company, validator);

        // 2. Remove da coleção da empresa as unidades que não estão mais no DTO
        // O @EqualsAndHashCode(of = {"name", "cnpj"}) na Unit.java é crucial aqui
        // O orphanRemoval=true fará o DELETE no banco.
        company.getUnits().retainAll(unitsFromDto);

        // 3. Adiciona na coleção da empresa as unidades novas do DTO
        // O Set garante que unidades "iguais" (mesmo name/cnpj) não sejam duplicadas.
        // O CascadeType.ALL fará o INSERT no banco.
        company.getUnits().addAll(unitsFromDto);
    }

    /**
     * Helper de MERGE para Setores (Usado pelo updateCompany)
     */
    private void mergeSectors(Company company, List<String> sectorNames) {
        // 1. Converte DTOs em um Set de entidades (com ID nulo)
        Set<Sector> sectorsFromDto = mapSectorNamesToEntities(sectorNames, company);

        // 2. Remove da coleção da empresa os setores que não estão mais no DTO
        company.getSectors().retainAll(sectorsFromDto);

        // 3. Adiciona na coleção da empresa os setores novos do DTO
        company.getSectors().addAll(sectorsFromDto);
    }


    /**
     * Helper que CONVERTE DTOs de Unidade em Entidades (sem salvar)
     */
    private Set<Unit> mapUnitDtosToEntities(List<UnitDTO> unitDtos, Company company, CNPJValidator validator) {
        if (unitDtos == null) {
            return Set.of(); // Retorna um Set vazio
        }

        return unitDtos.stream()
                .map(unitDto -> {
                    // Valida o CNPJ da unidade (se existir)
                    validateCnpj(unitDto.getCnpj(), unitDto.getName(), validator);

                    Unit unit = new Unit();
                    unit.setName(unitDto.getName());
                    unit.setCnpj(unitDto.getCnpj());
                    unit.setCompany(company); // Define o relacionamento
                    return unit;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Helper que CONVERTE Nomes de Setores em Entidades (sem salvar)
     */
    private Set<Sector> mapSectorNamesToEntities(List<String> sectorNames, Company company) {
        if (sectorNames == null || sectorNames.isEmpty()) {
            throw new IllegalArgumentException("A empresa deve ter pelo menos um setor.");
        }

        return sectorNames.stream()
                .map(name -> {
                    Sector sector = new Sector();
                    sector.setName(name);
                    sector.setCompany(company); // Define o relacionamento
                    return sector;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Helper de VALIDAÇÃO de CNPJ
     */
    private void validateCnpj(String cnpj, String unitName, CNPJValidator validator) {
        if (cnpj == null || cnpj.trim().isEmpty()) {
            return; // É nulo, o que é permitido
        }
        try {
            validator.assertValid(cnpj);
        } catch (InvalidStateException e) {
            String message = unitName.equals("principal")
                    ? "CNPJ da empresa principal é inválido: " + cnpj
                    : "CNPJ da unidade '" + unitName + "' é inválido: " + cnpj;
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Remove uma empresa do sistema após verificar dependências.*
     * @param id identificador da empresa a ser removida
     * @throws RuntimeException se a empresa não for encontrada
     * @throws IllegalStateException se a empresa estiver vinculada a relatórios
     */
    @Transactional
    public void deleteCompany(Long id) {
        // 1. Verifica se a empresa existe
        if (!companyRepository.existsById(id)) {
            throw new RuntimeException("Empresa não encontrada com o ID: " + id);
        }

        // 2. APLICA A SUA REGRA DE NEGÓCIO
        if (riskReportRepository.existsByCompany_Id(id)) {
            throw new IllegalStateException("Esta empresa não pode ser excluída, pois está sendo usada em Checklists de Risco.");
        }

        if (aepReportRepository.existsByCompany_Id(id)) {
            throw new IllegalStateException("Esta empresa não pode ser excluída, pois está sendo usada em relatórios AEP.");
        }

        if (technicalVisitRepository.existsByClientCompany_Id(id)) {
            throw new IllegalStateException("Esta empresa não pode ser excluída, pois está sendo usada em Relatórios de Visita.");
        }

        // Se não tem relatórios, limpar os cargos vinculados à empresa
        jobRoleRepository.deleteByCompanyId(id);

        // 3. Se passou em todas as verificações, exclui
        companyRepository.deleteById(id);
    }


    // --- MÉTODOS PRIVADOS (HELPERS) ---

    /**
     * Helper para mapear DTOs de Unidade para a Entidade Company.
     * Esta lógica agora é usada tanto pelo create quanto pelo update.
     */
    private void mapUnitsToCompany(Company company, List<UnitDTO> unitDtos, CNPJValidator validator) {
        // 1. Limpa a lista antiga (necessário para o update, não afeta o create)
        // O 'orphanRemoval=true' na entidade Company deletará as unidades antigas.
        company.getUnits().clear();

        if (unitDtos == null) {
            return; // Se a lista for nula, não há nada a fazer.
        }

        // 2. Converte os DTOs em Entidades
        Set<Unit> newUnits = unitDtos.stream()
                .map(unitDto -> {
                    // Valida o CNPJ da unidade (se existir)
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
                    unit.setCompany(company); // Define o relacionamento
                    return unit;
                })
                .collect(Collectors.toSet());

        // 3. Adiciona a nova lista
        // O 'CascadeType.ALL' na entidade Company inserirá os novos.
        company.getUnits().addAll(newUnits);
    }

    /**
     * Helper para mapear Nomes de Setores para a Entidade Company.
     */
    private void mapSectorsToCompany(Company company, List<String> sectorNames) {
        // 1. Limpa a lista antiga (Isso garante que se o usuário enviou vazio, a empresa ficará sem setores)
        company.getSectors().clear();

        // 2. CORREÇÃO: Se a lista for nula ou vazia, apenas encerra o método (sem erro)
        if (sectorNames == null || sectorNames.isEmpty()) {
            return;
        }

        // 3. Converte os nomes em Entidades
        Set<Sector> newSectors = sectorNames.stream()
                .map(name -> {
                    Sector sector = new Sector();
                    sector.setName(name);
                    sector.setCompany(company); // Define o relacionamento
                    return sector;
                })
                .collect(Collectors.toSet());

        // 4. Adiciona a nova lista
        company.getSectors().addAll(newSectors);
    }
}