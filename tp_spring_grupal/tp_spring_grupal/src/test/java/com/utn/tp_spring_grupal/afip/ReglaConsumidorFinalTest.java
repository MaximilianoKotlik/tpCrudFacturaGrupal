package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReglaConsumidorFinalTest {

    private static final BigDecimal LIMITE = new BigDecimal("10000000");

    @Test
    void consumidorFinalSinIdentificarPorEncimaDelLimiteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                ReglaConsumidorFinal.validar(DocumentoReceptor.CONSUMIDOR_FINAL, new BigDecimal("10000000.00"), LIMITE));
        assertThrows(IllegalArgumentException.class, () ->
                ReglaConsumidorFinal.validar(DocumentoReceptor.CONSUMIDOR_FINAL, new BigDecimal("25000000"), LIMITE));
    }

    @Test
    void consumidorFinalPorDebajoDelLimiteSePermite() {
        assertDoesNotThrow(() ->
                ReglaConsumidorFinal.validar(DocumentoReceptor.CONSUMIDOR_FINAL, new BigDecimal("9999999.99"), LIMITE));
    }

    @Test
    void conDocumentoIdentificadoNoHayTopeParaElMonto() {
        assertDoesNotThrow(() ->
                ReglaConsumidorFinal.validar(new DocumentoReceptor(96, 30123456L), new BigDecimal("50000000"), LIMITE));
        assertDoesNotThrow(() ->
                ReglaConsumidorFinal.validar(new DocumentoReceptor(80, 20123456786L), new BigDecimal("50000000"), LIMITE));
    }
}
