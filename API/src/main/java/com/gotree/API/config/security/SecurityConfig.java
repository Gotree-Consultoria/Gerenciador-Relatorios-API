package com.gotree.API.config.security;

import com.gotree.API.config.security.jwt.JwtAccessDeniedHandler;
import com.gotree.API.config.security.jwt.JwtAuthenticationEntryPoint;
import com.gotree.API.config.security.jwt.JwtAuthenticationFilter;
import com.gotree.API.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity // Habilita o uso de anotações como @PreAuthorize nos controllers
public class SecurityConfig {

	// Injeta a configuração de origens do application.properties
	@Value("${cors.allowed-origins}")
	private String[] allowedOrigins;

	@SuppressWarnings("unused")
	private final UserService userService;

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	public SecurityConfig(UserService userService, JwtAuthenticationFilter jwtAuthenticationFilter,
						  JwtAccessDeniedHandler jwtAccessDeniedHandler, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
		this.userService = userService;
		this.jwtAuthFilter = jwtAuthenticationFilter;
		this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Permite requisições de preflight (CORS)
						.requestMatchers("/auth/login").permitAll()            // Libera o endpoint de login
						.anyRequest().authenticated()                             // Exige autenticação para todas as outras requisições
				)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(jwtAccessDeniedHandler)
				)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// Usa as origens configuradas no application.properties
		configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}