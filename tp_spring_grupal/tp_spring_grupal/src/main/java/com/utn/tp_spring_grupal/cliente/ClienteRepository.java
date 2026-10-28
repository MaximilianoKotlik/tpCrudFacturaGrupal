package com.utn.tp_spring_grupal.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    /** Trae la condicion IVA (y los one-to-one) en la misma consulta del explorer (sin N+1). */
    @Override
    @EntityGraph(attributePaths = { "condicionIva", "contacto", "domicilio" })
    Page<Cliente> findAll(Specification<Cliente> spec, Pageable pageable);
}
