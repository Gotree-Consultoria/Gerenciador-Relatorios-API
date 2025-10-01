package com.gotree.API.config.security.jwt;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.repositories.UserRepository;
import com.gotree.API.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getServletPath();
        if (path.equals("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Token ausente ou mal formado");
        }

        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        if (username == null) {
            throw new BadCredentialsException("Token inválido");
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var user = userRepository.findByEmail(username).orElse(null);

            if (user == null || !jwtService.isTokenValid(jwt, user)) {
                throw new BadCredentialsException("Token inválido");
            }

            // 1. Crie o CustomUserDetails a partir do User encontrado
            CustomUserDetails userDetails = new CustomUserDetails(user);

            // 2. Use o userDetails (e não o 'user') como o principal da autenticação
            var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}