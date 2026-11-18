package com.utn.tp_spring_grupal.condicioniva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CondicionIvaRepository extends JpaRepository<CondicionIva, Long> {

    Optional<CondicionIva> findFirstByCodigoAfip(int codigoAfip);
}
