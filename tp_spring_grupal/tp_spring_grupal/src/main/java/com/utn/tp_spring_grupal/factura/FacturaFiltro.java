package com.utn.tp_spring_grupal.factura;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Filtros del listado y de los reportes de facturas. Todos opcionales (null = sin filtrar).
 *
 * @param fechaDesde      fecha de creación desde (inclusive)
 * @param fechaHasta      fecha de creación hasta (inclusive)
 * @param montoMinimo     importeTotal &gt;= montoMinimo
 * @param clienteId       facturas de ese cliente
 * @param tipoComprobante A / B / C: el guardado al emitir o, si no se emitió, el que resulta de la condición IVA
 * @param cae             el CAE contiene este texto
 */
public record FacturaFiltro(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        EstadoFactura estado,
        BigDecimal montoMinimo,
        Long clienteId,
        TipoComprobante tipoComprobante,
        String cae) {

    public static final FacturaFiltro VACIO = new FacturaFiltro(null, null, null, null, null, null, null);
}
