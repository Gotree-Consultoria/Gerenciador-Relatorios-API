package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.dashboard.AdminStatsDTO;
import com.gotree.API.dto.dashboard.MyStatsDTO;
import com.gotree.API.dto.dashboard.UserDocumentStatsDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;


    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retorna os KPIs do usuário logado (Técnico).
     */
    @GetMapping("/my-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyStatsDTO> getMyStats(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        MyStatsDTO stats = dashboardService.getMyStats(currentUser);
        return ResponseEntity.ok(stats);
    }

    /**
     * Retorna os KPIs globais (Admin).
     */
    @GetMapping("/admin-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsDTO> getAdminStats() {
        AdminStatsDTO stats = dashboardService.getAdminStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retorna estatísticas de documentos por usuário, com filtros (Admin).
     */
    @GetMapping("/admin-stats/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDocumentStatsDTO>> getAdminDocumentStats(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type, // "VISIT" ou "AEP"
            @RequestParam(required = false) Long companyId
    ) {
        List<UserDocumentStatsDTO> stats = dashboardService.getAdminDocumentStats(userId, type, companyId);
        return ResponseEntity.ok(stats);
    }
}