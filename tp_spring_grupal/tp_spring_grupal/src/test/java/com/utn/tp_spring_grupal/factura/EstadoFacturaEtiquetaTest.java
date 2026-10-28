package com.utn.tp_spring_grupal.factura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoFacturaEtiquetaTest {

    @Test
    void cadaEstadoTieneUnaEtiquetaLegibleParaElUsuario() {
        assertEquals("pendiente", EstadoFactura.PENDIENTE.etiqueta());
        assertEquals("emitida", EstadoFactura.EMITIDA.etiqueta());
        assertEquals("con error de ARCA", EstadoFactura.ERROR_AFIP.etiqueta());
        assertEquals("anulada", EstadoFactura.ANULADA.etiqueta());
    }
}
