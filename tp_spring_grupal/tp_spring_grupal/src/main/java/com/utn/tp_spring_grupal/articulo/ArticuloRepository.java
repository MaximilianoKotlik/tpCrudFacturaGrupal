package com.utn.tp_spring_grupal.articulo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long>, JpaSpecificationExecutor<Articulo> {

    /** Trae rubro, marca y alicuota en la misma consulta (sin N+1). */
    @Override
    @EntityGraph(attributePaths = { "rubro", "marca", "alicuotaIva" })
    Page<Articulo> findAll(Specification<Articulo> spec, Pageable pageable);
}
