package com.gotree.API.services;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import org.springframework.stereotype.Service;

@Service
public class CpfValidatorService {

    public void validateCpf(String cpf) {
        CPFValidator validator = new CPFValidator();
        try {
            validator.assertValid(cpf);
        } catch (InvalidStateException e) {
            throw new IllegalArgumentException("CPF inválido: " + cpf, e);
        }
    }
}
