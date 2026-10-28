package com.utn.tp_spring_grupal.articulo;

import java.math.BigDecimal;

public record ArticuloDTO(Long id, String codigo, String denominacion, String rubro, String marca,
                          BigDecimal alicuotaPorcentaje) {
}
