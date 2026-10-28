package com.utn.tp_spring_grupal.factura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Factura para listado y detalle. En el listado {@code detalles} es null. */
public record FacturaDTO(
        Long id,
        Long numeroFactura,
        EstadoFactura estado,
        String tipoComprobante,
        LocalDateTime fechaCreacion,
        LocalDate fechaEmision,
        Long clienteId,
        String clienteDenominacion,
        Long listaPrecioId,
        String listaPrecioDenominacion,
        Long condicionIvaId,
        String condicionIvaDenominacion,
        Long puntoVentaId,
        String puntoVentaDescripcion,
        Long tipoMonedaId,
        BigDecimal cotizacion,
        BigDecimal importeNeto,
        BigDecimal importeIva,
        BigDecimal importeTotal,
        String cae,
        LocalDate caeFechaVencimiento,
        String motivoRechazo,
        String observaciones,
        long cantidadItems,
        List<FacturaDetalleDTO> detalles) {
}
