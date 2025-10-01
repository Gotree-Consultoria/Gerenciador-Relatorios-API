package com.gotree.API.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gotree.API.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

}
