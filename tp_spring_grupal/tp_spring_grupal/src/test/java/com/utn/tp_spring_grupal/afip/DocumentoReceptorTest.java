package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoReceptorTest {

    @Test
    void cuitConGuionesEsTipo80ConSoloLosDigitos() {
        DocumentoReceptor d = DocumentoReceptor.deCuitCuil("20-12345678-6");
        assertEquals(80, d.tipo());
        assertEquals(20123456786L, d.numero());
    }

    @Test
    void cuitSinGuionesEsTipo80() {
        assertEquals(new DocumentoReceptor(80, 30500010912L), DocumentoReceptor.deCuitCuil("30500010912"));
    }

    @Test
    void dniDeOchoDigitosEsTipo96() {
        assertEquals(new DocumentoReceptor(96, 30123456L), DocumentoReceptor.deCuitCuil("30.123.456"));
    }

    @Test
    void dniDeSieteDigitosEsTipo96() {
        assertEquals(new DocumentoReceptor(96, 7654321L), DocumentoReceptor.deCuitCuil("7654321"));
    }

    @Test
    void sinDocumentoEsConsumidorFinal() {
        assertEquals(DocumentoReceptor.CONSUMIDOR_FINAL, DocumentoReceptor.deCuitCuil(null));
        assertEquals(DocumentoReceptor.CONSUMIDOR_FINAL, DocumentoReceptor.deCuitCuil("   "));
        assertEquals(new DocumentoReceptor(99, 0), DocumentoReceptor.CONSUMIDOR_FINAL);
    }

    @Test
    void longitudIrreconocibleSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> DocumentoReceptor.deCuitCuil("12345"));
        assertThrows(IllegalArgumentException.class, () -> DocumentoReceptor.deCuitCuil("1234567890"));
    }
}
