package com.gotree.API.controllers;

import com.gotree.API.services.UnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUnit(@PathVariable Long id) {
        try {
            unitService.deleteUnit(id);
            // 204 No Content: Sucesso, sem corpo de resposta
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            // 409 Conflict: A regra de negócio impediu
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            // 404 Not Found: A unidade não existia
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}