package com.utn.tp_spring_grupal.factura;

import java.util.List;

/**
 * Alta (POST) y edición (PUT) de factura. Al crear todos los campos salvo observaciones son obligatorios;
 * al actualizar se aplican solo los no nulos. La condición IVA se toma del cliente.
 */
public record FacturaGuardarDTO(
        String observaciones,
        Long clienteId,
        Long listaPrecioId,
        Long puntoVentaId,
        Long tipoMonedaId,
        List<FacturaDetalleDTO> detalles) {
}
