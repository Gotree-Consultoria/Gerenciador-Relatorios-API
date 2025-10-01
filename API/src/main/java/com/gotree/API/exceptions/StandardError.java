package com.gotree.API.exceptions;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public class StandardError {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    // Aparece apenas se ela não estiver vazia
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ValidationError> validationErrors = new ArrayList<>();

    public StandardError(Instant timestamp, Integer status, String error, String message, String path) {
        super();
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;

    }

    // Region Getters/Setters

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void addValidationError(String field, String message) {
        this.validationErrors.add(new ValidationError(field, message));
    }

    // endregion

}