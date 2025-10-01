package com.gotree.API.config.security;

import com.gotree.API.entities.User;
import lombok.Getter; // Importa a anotação @Getter
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial; // Importa a anotação @Serial
import java.util.Collection;

@Getter // @Getter no nível da classe
public class CustomUserDetails implements UserDetails {

	@Serial // @Serial ao serialVersionUID
	private static final long serialVersionUID = 1L;

	private final User user;

	public CustomUserDetails(User user) {
		this.user = user;
	}


	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return user.getAuthorities();
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}