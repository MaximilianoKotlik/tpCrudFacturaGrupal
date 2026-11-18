package com.utn.tp_spring_grupal.listaprecio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Articulo dentro de una lista de precios, con el precio neto de esa lista y su alicuota de IVA.
 * {@code precios} trae el precio del articulo en cada lista donde figura.
 */
public record ListaPrecioArticuloDTO(
        Long articuloId,
        String codigo,
        String denominacion,
        String rubro,
        String marca,
        BigDecimal precioVenta,
        BigDecimal alicuotaPorcentaje,
        List<PrecioListaDTO> precios) {
}
