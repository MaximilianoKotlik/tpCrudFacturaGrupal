package com.utn.tp_spring_grupal.cliente;

public record ClienteDTO(Long id, String denominacion, String cuitCuil, Long condicionIvaId,
                         String condicionIvaDenominacion) {
}
