package com.utn.tp_spring_grupal.factura;

import java.math.BigDecimal;

/**
 * Renglón de factura, en entrada y salida. En entrada solo se usan articuloId, descripcion, cantidad y
 * porcentajeBonificacion: precio, alícuota e importes los calcula el backend.
 */
public record FacturaDetalleDTO(
        Long articuloId,
        String descripcion,
        Integer cantidad,
        BigDecimal porcentajeBonificacion,
        BigDecimal precioUnitario,
        BigDecimal alicuotaPorcentaje,
        BigDecimal importeNeto,
        BigDecimal importeIva,
        BigDecimal importeSubtotal) {
}
