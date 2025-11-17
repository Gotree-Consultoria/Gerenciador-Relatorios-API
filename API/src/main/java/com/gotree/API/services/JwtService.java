package com.gotree.API.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Serviço responsável por gerenciar operações relacionadas a JSON Web Tokens (JWT).
 * Fornece funcionalidades para geração, validação e extração de informações de tokens.
 */
@Service
public class JwtService {


    @Value("${jwt.secret_path}")
    private String secretKeyPath;

    private static final long EXPIRATION_TIME = 86_400_000L; // 1 dia

    // Gera o token JWT com base nos dados do usuário

    /**
     * Gera um token JWT para um usuário específico.
     *
     * @param userDetails detalhes do usuário para quem o token será gerado
     * @return String contendo o token JWT gerado
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder().setSubject(userDetails.getUsername()).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
    }

    // Verifica se o token é valido

    /**
     * Verifica se um token JWT é válido para um determinado usuário.
     *
     * @param token       o token JWT a ser validado
     * @param userDetails detalhes do usuário para validação
     * @return true se o token for válido, false caso contrário
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExperired(token);
    }

    // Extrai o username (email nesse caso) do token

    /**
     * Extrai o nome do usuário do token JWT.
     *
     * @param token o token JWT do qual será extraído o username
     * @return String contendo o username extraído do token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);

    }

    // Verifica se o token já expirou

    /**
     * Verifica se um token JWT está expirado.
     *
     * @param token o token JWT a ser verificado
     * @return true se o token estiver expirado, false caso contrário
     */
    private boolean isTokenExperired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extrai a data de expiração do token

    /**
     * Extrai a data de expiração do token JWT.
     *
     * @param token o token JWT do qual será extraída a data de expiração
     * @return Date contendo a data de expiração do token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Metodo genérico para extrair qualquer informação do token

    /**
     * Extrai uma informação específica do token JWT.
     *
     * @param token          o token JWT do qual será extraída a informação
     * @param claimsResolver função que define qual informação será extraída
     * @param <T>            tipo do objeto que será retornado
     * @return T objeto contendo a informação extraída do token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extrai todas as informações do token (claims = afirmações)

    /**
     * Extrai todas as claims (informações) do token JWT.
     *
     * @param token o token JWT do qual serão extraídas as claims
     * @return Claims objeto contendo todas as claims do token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
    }

    // Converte a string secretKey em uma chave real.

    /**
     * Gera uma chave de assinatura a partir da chave secreta.
     *
     * @return Key objeto contendo a chave de assinatura
     */
    private Key getSignInKey() {
        try {
            // Lê os bytes do arquivo no caminho que foi injetado
            byte[] keyBytes = Files.readAllBytes(Paths.get(secretKeyPath));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IOException e) {
            // Isso vai impedir a API de subir se o secret não for encontrado
            throw new RuntimeException("Falha ao ler o arquivo da chave secreta do JWT", e);
        }
    }

}
