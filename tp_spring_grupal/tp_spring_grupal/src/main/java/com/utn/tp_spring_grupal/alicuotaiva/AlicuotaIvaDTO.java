package com.utn.tp_spring_grupal.alicuotaiva;

import java.math.BigDecimal;

public record AlicuotaIvaDTO(Long id, int codigoAfip, BigDecimal porcentaje, String denominacion) {
}
