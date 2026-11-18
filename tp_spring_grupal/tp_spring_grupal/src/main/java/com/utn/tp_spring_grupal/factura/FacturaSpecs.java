package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.emisor.CondicionIvaEmisor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Filtros del listado de facturas. Las fechas filtran por fecha de CREACIÓN (AuditoriaApp.fechaAlta).
 * Las facturas dadas de baja (fechaBaja no nula) nunca se listan.
 */
public final class FacturaSpecs {

    private FacturaSpecs() {
    }

    /**
     * @param filtro filtros opcionales
     * @param emisor condición del emisor: define la letra de las facturas que todavía no tienen tipo guardado
     */
    public static Specification<FacturaVenta> filtrar(FacturaFiltro filtro, CondicionIvaEmisor emisor) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isNull(root.get("fechaBaja")));
            if (filtro.fechaDesde() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<Date>get("fechaAlta"), aDate(filtro.fechaDesde())));
            }
            if (filtro.fechaHasta() != null) {
                ps.add(cb.lessThan(root.<Date>get("fechaAlta"), aDate(filtro.fechaHasta().plusDays(1))));
            }
            if (filtro.estado() != null) {
                ps.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            if (filtro.montoMinimo() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("importeTotal"), filtro.montoMinimo()));
            }
            if (filtro.clienteId() != null) {
                ps.add(cb.equal(root.get("cliente").get("id"), filtro.clienteId()));
            }
            String cae = filtro.cae() != null ? filtro.cae().trim() : "";
            if (!cae.isEmpty()) {
                ps.add(cb.like(root.get("cae"), "%" + escaparLike(cae) + "%", '\\'));
            }
            if (filtro.tipoComprobante() != null) {
                ps.add(porTipo(root, cb, filtro.tipoComprobante(), emisor));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Tipo guardado al emitir, o (si no hay) el que resulta de emisor x condición IVA del receptor. */
    private static Predicate porTipo(Root<FacturaVenta> root, CriteriaBuilder cb, TipoComprobante tipo,
                                     CondicionIvaEmisor emisor) {
        Path<Integer> guardado = root.get("tipoComprobante");
        Predicate conTipoGuardado = cb.equal(guardado, tipo.getCodigoAfip());
        Predicate sinTipoGuardado = cb.isNull(guardado);
        Path<Integer> codigoReceptor = root.get("condicionIva").get("codigoAfip");

        Predicate calculado;
        if (!emisor.discriminaIva()) {
            calculado = tipo == TipoComprobante.C ? cb.conjunction() : cb.disjunction();
        } else if (tipo == TipoComprobante.A) {
            calculado = codigoReceptor.in(TipoComprobanteResolver.RECEPTORES_CLASE_A);
        } else if (tipo == TipoComprobante.B) {
            calculado = cb.not(codigoReceptor.in(TipoComprobanteResolver.RECEPTORES_CLASE_A));
        } else {
            calculado = cb.disjunction();
        }
        return cb.or(conTipoGuardado, cb.and(sinTipoGuardado, calculado));
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Date aDate(LocalDate dia) {
        return Date.from(dia.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
