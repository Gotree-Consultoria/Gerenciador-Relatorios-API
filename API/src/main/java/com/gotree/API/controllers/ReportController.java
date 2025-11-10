package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.report.SaveInspectionReportRequestDTO;
import com.gotree.API.dto.report.SaveNrsReportRequestDTO; // <-- Importa o novo DTO
import com.gotree.API.entities.InspectionReport;
import com.gotree.API.entities.User;
import com.gotree.API.services.InspectionReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class ReportController {

    private final InspectionReportService inspectionReportService;

    public ReportController(InspectionReportService inspectionReportService) {
        this.inspectionReportService = inspectionReportService;
    }

    // --- ENDPOINT DO CHECKLIST---
    @PostMapping("/inspection-reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveInspectionReport(@Valid @RequestBody SaveInspectionReportRequestDTO dto,
                                                  Authentication authentication) {
        // Extrai o usuário e passa para o serviço (para habilitar a refatoração)
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();
        InspectionReport savedReport = inspectionReportService.saveReportAndGeneratePdf(dto, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Relatório salvo com sucesso!");
        response.put("reportId", savedReport.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- ENDPOINT DO CHECKLIST-NR C/NC/NA ---
    @PostMapping("/inspection-reports/nrs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveNrsInspectionReport(
            @Valid @RequestBody SaveNrsReportRequestDTO dto,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        // Chama o novo método de serviço
        InspectionReport savedReport = inspectionReportService.saveNrsReportAndGeneratePdf(dto, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Relatório NR salvo com sucesso!");
        response.put("reportId", savedReport.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}