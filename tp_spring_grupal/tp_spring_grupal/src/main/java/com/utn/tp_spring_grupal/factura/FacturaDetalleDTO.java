package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.listaprecio.PrecioListaDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Renglón de factura, en entrada y salida. En entrada solo se usan articuloId, descripcion, cantidad y
 * porcentajeBonificacion: precio, alícuota e importes los calcula el backend. En salida {@code precios} trae el
 * precio del artículo en cada lista, para recalcular en pantalla al cambiar de lista.
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
        BigDecimal importeSubtotal,
        List<PrecioListaDTO> precios) {
}
