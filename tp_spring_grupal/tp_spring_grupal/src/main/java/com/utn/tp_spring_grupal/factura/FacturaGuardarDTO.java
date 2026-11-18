package com.utn.tp_spring_grupal.factura;

import java.util.List;

/**
 * Alta (POST) y edición (PUT) de factura. Al crear todos los campos salvo observaciones son obligatorios;
 * al actualizar se aplican solo los no nulos. La condición IVA se toma del cliente.
 *
 * <p>{@code consumidorFinal = true} factura a un Consumidor Final sin identificar: no lleva cliente y la condición
 * IVA es Consumidor Final. Al editar, {@code true} quita el cliente; para volver a uno se envía su {@code clienteId}.
 */
public record FacturaGuardarDTO(
        String observaciones,
        Long clienteId,
        Long listaPrecioId,
        Long puntoVentaId,
        Long tipoMonedaId,
        List<FacturaDetalleDTO> detalles,
        Boolean consumidorFinal) {

    public FacturaGuardarDTO(String observaciones, Long clienteId, Long listaPrecioId, Long puntoVentaId,
                             Long tipoMonedaId, List<FacturaDetalleDTO> detalles) {
        this(observaciones, clienteId, listaPrecioId, puntoVentaId, tipoMonedaId, detalles, null);
    }

    public boolean esConsumidorFinal() {
        return Boolean.TRUE.equals(consumidorFinal);
    }
}
