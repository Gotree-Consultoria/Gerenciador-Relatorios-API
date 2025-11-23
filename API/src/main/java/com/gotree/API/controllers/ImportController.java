package com.gotree.API.controllers;

import com.gotree.API.services.ExcelImportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/import")
public class ImportController {

    private final ExcelImportService excelImportService;

    public ImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @PostMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')") // Apenas admin deve importar
    public ResponseEntity<String> importCompanies(@RequestParam("file") MultipartFile file) {
        try {
            excelImportService.importCompanies(file);
            return ResponseEntity.ok("Importação concluída com sucesso!");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao ler o arquivo: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro na importação: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> exportCompanies() throws IOException {
        ByteArrayInputStream in = excelImportService.exportCompaniesToExcel(); // Ou excelService

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=empresas_exportadas.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}