package com.utn.tp_spring_grupal.factura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoFacturaTest {

    @Test
    void pendienteEsEditable() {
        assertTrue(EstadoFactura.PENDIENTE.esEditable());
    }

    @Test
    void errorAfipEsEditableParaPoderReintentar() {
        assertTrue(EstadoFactura.ERROR_AFIP.esEditable());
    }

    @Test
    void emitidaNoEsEditable() {
        assertFalse(EstadoFactura.EMITIDA.esEditable());
    }

    @Test
    void anuladaNoEsEditable() {
        assertFalse(EstadoFactura.ANULADA.esEditable());
    }

    @Test
    void tieneExactamenteLosCuatroEstadosDelContrato() {
        assertEquals(4, EstadoFactura.values().length);
        assertEquals(EstadoFactura.ERROR_AFIP, EstadoFactura.valueOf("ERROR_AFIP"));
    }
}
