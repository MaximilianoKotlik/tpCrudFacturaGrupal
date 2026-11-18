package com.utn.tp_spring_grupal.listaprecio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Precio (neto) de un artículo en una lista. Viaja con los artículos para que el front pueda recalcular los
 * renglones al cambiar de lista sin volver a elegirlos.
 */
public record PrecioListaDTO(Long listaPrecioId, BigDecimal precioVenta) {

    /** Agrupa los precios por id de artículo. */
    public static Map<Long, List<PrecioListaDTO>> porArticulo(Collection<ListaPrecioArticulo> precios) {
        if (precios.isEmpty()) {
            return new HashMap<>();
        }
        return precios.stream().collect(Collectors.groupingBy(
                lpa -> lpa.getArticulo().getId(),
                Collectors.mapping(lpa -> new PrecioListaDTO(lpa.getListaPrecio().getId(), lpa.getPrecioVenta()),
                        Collectors.toList())));
    }
}
