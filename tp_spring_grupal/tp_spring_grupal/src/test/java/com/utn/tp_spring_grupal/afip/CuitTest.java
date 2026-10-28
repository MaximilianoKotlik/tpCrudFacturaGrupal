package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuitTest {

    @Test
    void reconoceCuitsConDigitoVerificadorCorrecto() {
        assertTrue(Cuit.esValido("20123456786"));
        assertTrue(Cuit.esValido("20-12345678-6"));
        assertTrue(Cuit.esValido("30500010912"));
    }

    @Test
    void rechazaDigitoVerificadorIncorrectoOLongitudErronea() {
        assertFalse(Cuit.esValido("20123456787"));
        assertFalse(Cuit.esValido("2012345678"));
        assertFalse(Cuit.esValido(null));
        assertFalse(Cuit.esValido("abc"));
    }

    @Test
    void calculaElDigitoVerificadorDeLosPrimerosDiezDigitos() {
        assertEquals(6, Cuit.digitoVerificador("2012345678"));
        assertEquals(2, Cuit.digitoVerificador("3050001091"));
    }

    @Test
    void completarDevuelveUnCuitValidoAvanzandoElNumeroSiElDigitoNoExiste() {
        for (int base = 20_000_000; base < 20_000_200; base++) {
            assertTrue(Cuit.esValido(Cuit.completar("30", base)), "base " + base);
        }
    }
}
