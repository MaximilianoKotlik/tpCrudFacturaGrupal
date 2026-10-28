package com.utn.tp_spring_grupal.factura;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Filtros del listado de facturas. Las fechas filtran por fecha de CREACIÓN (AuditoriaApp.fechaAlta). */
public final class FacturaSpecs {

    private FacturaSpecs() {
    }

    /**
     * @param desde       inclusive (00:00 del día), o null
     * @param hasta       inclusive (hasta el final del día), o null
     * @param estado      o null
     * @param montoMinimo importeTotal &gt;= montoMinimo, o null
     */
    public static Specification<FacturaVenta> filtrar(LocalDate desde, LocalDate hasta, EstadoFactura estado,
                                                      BigDecimal montoMinimo) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<Date>get("fechaAlta"), aDate(desde)));
            }
            if (hasta != null) {
                ps.add(cb.lessThan(root.<Date>get("fechaAlta"), aDate(hasta.plusDays(1))));
            }
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (montoMinimo != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("importeTotal"), montoMinimo));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static Date aDate(LocalDate dia) {
        return Date.from(dia.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
