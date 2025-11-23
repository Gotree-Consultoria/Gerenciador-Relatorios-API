package com.gotree.API.services;

import com.gotree.API.dto.company.CompanyRequestDTO;
import com.gotree.API.dto.company.UnitDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.Sector;
import com.gotree.API.entities.Unit;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ExcelImportService {

    private final CompanyService companyService;

    public ExcelImportService(CompanyService companyService) {
        this.companyService = companyService;
    }

    public void importCompanies(MultipartFile file) throws IOException {
        // Mapa para agrupar linhas pelo CNPJ da empresa
        // Chave: CNPJ, Valor: DTO da empresa sendo montado
        Map<String, CompanyRequestDTO> companiesMap = new HashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // Lê a primeira aba

            // Itera sobre as linhas (pula o cabeçalho se i=0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 1. Ler dados da linha (com tratamento de nulos)
                String compName = getCellValue(row, 0);
                String compCnpjRaw = getCellValue(row, 1);

                if (compName.isEmpty() || compCnpjRaw.isEmpty()) continue; // Pula linha inválida

                String compCnpj = compCnpjRaw.replaceAll("[^0-9]", ""); // Limpa CNPJ

                // 2. Recupera ou Cria o DTO da Empresa
                CompanyRequestDTO companyDto = companiesMap.computeIfAbsent(compCnpj, k -> {
                    CompanyRequestDTO dto = new CompanyRequestDTO();
                    dto.setName(compName);
                    dto.setCnpj(compCnpj);
                    dto.setUnits(new ArrayList<>());
                    dto.setSectors(new ArrayList<>());
                    return dto;
                });

                // 3. Adiciona Unidade (se houver nessa linha)
                String unitName = getCellValue(row, 2);
                String unitCnpjRaw = getCellValue(row, 3);

                if (!unitName.isEmpty()) {
                    UnitDTO unitDto = new UnitDTO();
                    unitDto.setName(unitName);
                    if (!unitCnpjRaw.isEmpty()) {
                        unitDto.setCnpj(unitCnpjRaw.replaceAll("[^0-9]", ""));
                    }
                    // Evita duplicatas na lista do DTO
                    boolean unitExists = companyDto.getUnits().stream()
                            .anyMatch(u -> u.getName().equalsIgnoreCase(unitName));
                    if (!unitExists) {
                        companyDto.getUnits().add(unitDto);
                    }
                }

                // 4. Adiciona Setor (se houver nessa linha)
                String sectorName = getCellValue(row, 4);
                if (!sectorName.isEmpty()) {
                    if (!companyDto.getSectors().contains(sectorName)) {
                        companyDto.getSectors().add(sectorName);
                    }
                }
            }
        }

        // 5. Processa os DTOs agrupados
        // Aqui usamos a inteligência do seu CompanyService (create ou update)
        for (CompanyRequestDTO dto : companiesMap.values()) {
            saveOrUpdateCompany(dto);
        }
    }

    private void saveOrUpdateCompany(CompanyRequestDTO dto) {
        // Verifica se a empresa já existe pelo CNPJ
        Optional<Company> existing = companyService.findAll().stream()
                .filter(c -> c.getCnpj().equals(dto.getCnpj()))
                .findFirst();

        if (existing.isPresent()) {
            // Se existe, chama o UPDATE (que agora tem a lógica de merge correta)
            companyService.updateCompany(existing.get().getId(), dto);
        } else {
            // Se não existe, chama o CREATE
            companyService.createCompany(dto);
        }
    }

    // Helper para ler célula como String com segurança
    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";

        // Força leitura como string (mesmo se for número no Excel)
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    public ByteArrayInputStream exportCompaniesToExcel() throws IOException {
        String[] columns = {"Nome Empresa", "CNPJ Empresa", "Nome Unidade", "CNPJ Unidade", "Nome Setor"};

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Empresas");

            // 1. Cria o Cabeçalho
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);

                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // 2. Busca dados do banco (Usa o companyService que já está injetado)
            List<Company> companies = companyService.findAll();

            int rowIdx = 1;

            for (Company company : companies) {
                // Converte Sets para Lists para poder acessar por índice
                List<Unit> units = new ArrayList<>(company.getUnits());
                List<Sector> sectors = new ArrayList<>(company.getSectors());

                // Descobre qual lista é maior para saber quantas linhas essa empresa vai ocupar
                int maxRows = Math.max(Math.max(units.size(), sectors.size()), 1);

                for (int i = 0; i < maxRows; i++) {
                    Row row = sheet.createRow(rowIdx++);

                    // Colunas A e B (Dados da Empresa - Repete em todas as linhas)
                    row.createCell(0).setCellValue(company.getName());
                    row.createCell(1).setCellValue(formatCnpj(company.getCnpj()));

                    // Colunas C e D (Unidade - se houver nessa linha)
                    if (i < units.size()) {
                        Unit unit = units.get(i);
                        row.createCell(2).setCellValue(unit.getName());
                        if (unit.getCnpj() != null) {
                            row.createCell(3).setCellValue(formatCnpj(unit.getCnpj()));
                        }
                    }

                    // Coluna E (Setor - se houver nessa linha)
                    if (i < sectors.size()) {
                        row.createCell(4).setCellValue(sectors.get(i).getName());
                    }
                }
            }

            // Ajusta largura das colunas
            for(int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Helper para formatar visualmente o CNPJ
    private String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" +
                cnpj.substring(12, 14);
    }
}