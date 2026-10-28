package com.utn.tp_spring_grupal.listaprecio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ListaPrecioArticuloRepository
        extends JpaRepository<ListaPrecioArticulo, Long>, JpaSpecificationExecutor<ListaPrecioArticulo> {

    /** Precio de un artículo en una lista concreta. */
    Optional<ListaPrecioArticulo> findByListaPrecioIdAndArticuloId(Long listaPrecioId, Long articuloId);

    /** Búsqueda del explorer: trae artículo, rubro y marca en una sola consulta (sin N+1). */
    @Override
    @EntityGraph(attributePaths = { "articulo", "articulo.rubro", "articulo.marca", "articulo.alicuotaIva" })
    Page<ListaPrecioArticulo> findAll(Specification<ListaPrecioArticulo> spec, Pageable pageable);
}
