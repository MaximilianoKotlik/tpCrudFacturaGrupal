package com.utn.tp_spring_grupal.factura;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long>, JpaSpecificationExecutor<FacturaVenta> {

    // Las relaciones to-one viajan en la misma consulta del listado (sin N+1 y sin paginar en memoria).
    @Override
    @EntityGraph(attributePaths = { "cliente", "listaPrecio", "condicionIva", "puntoVenta", "tipoMoneda" })
    Page<FacturaVenta> findAll(Specification<FacturaVenta> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "cliente", "listaPrecio", "condicionIva", "puntoVenta", "tipoMoneda" })
    List<FacturaVenta> findAll(Specification<FacturaVenta> spec, Sort sort);

    /** Cantidad de renglones por factura para una página de ids: [facturaId, cantidad]. */
    @Query("select d.factura.id, count(d) from FacturaVentaDetalle d where d.factura.id in :ids group by d.factura.id")
    List<Object[]> contarRenglones(@Param("ids") Collection<Long> ids);
}
