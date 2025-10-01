package com.gotree.API.exceptions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ResourceExceptionHandler {

    // Violação de integridade (Ex.: e-mail duplicado)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> handleDataIntegrity(DataIntegrityViolationException e,
                                                             HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Violação de integridade",
                e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    // Recurso não encontrado (Ex.: User ID inexistente)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> handleResourceNotFound(ResourceNotFoundException e,
                                                                HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Recurso não encontrado",
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);

    }

    // Erros de validação de campos (DTO @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidationErros(MethodArgumentNotValidException e,
                                                               HttpServletRequest request) {

        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        StandardError err = new StandardError(Instant.now(), status.value(), "Erro de validação", "Campos inválidos",
                request.getRequestURI());

        e.getBindingResult().getFieldErrors()
                .forEach(error -> err.addValidationError(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(status).body(err);
    }

    // Exceções não tratadas (Erro inesperado no sistema)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGlobalException(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(), "Erro interno no servidor", e.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    // Verifica exceção de formato de datas.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> handleInvalidFormat(HttpMessageNotReadableException e,
                                                             HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String mensagem = "Erro ao processar requisição.";

        // Verifica se a causa foi uma data malformada
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException invalidEx && invalidEx.getTargetType().equals(LocalDate.class)) {
            mensagem = "Formato de data inválido. Use o padrão: dd/MM/yyyy.";
        }

        StandardError err = new StandardError(Instant.now(), status.value(), "Erro de leitura do corpo JSON", mensagem,
                request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    // Verifica exceção em cpf
    @ExceptionHandler(CpfValidationException.class)
    public ResponseEntity<Object> handleCpfValidationException(CpfValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validação de CPF");
        body.put("message", ex.getMessage());
        body.put("path", "/users");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

}

