package com.utn.tp_spring_grupal.listaprecio;

import java.math.BigDecimal;

/** Articulo dentro de una lista de precios, con el precio neto de esa lista y su alicuota de IVA. */
public record ListaPrecioArticuloDTO(
        Long articuloId,
        String codigo,
        String denominacion,
        String rubro,
        String marca,
        BigDecimal precioVenta,
        BigDecimal alicuotaPorcentaje) {
}
