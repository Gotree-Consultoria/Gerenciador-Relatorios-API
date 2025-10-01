package com.gotree.API.repositories;


import com.gotree.API.entities.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByCnpj(String cnpj);
}
