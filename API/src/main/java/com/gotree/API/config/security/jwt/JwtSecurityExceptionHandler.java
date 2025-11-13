package com.gotree.API.config.security.jwt;

import com.gotree.API.exceptions.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class JwtSecurityExceptionHandler {

	// Credenciais errada (ex: senha errada)
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<StandardError> badCredentials(HttpServletRequest request) {

		StandardError err = new StandardError(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Não autorizado",
				"Credenciais inválidas", request.getRequestURI());

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
	}
	
	@ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> accessDenied(HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                "Você não tem permissão para acessar este recurso",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }
	

}
