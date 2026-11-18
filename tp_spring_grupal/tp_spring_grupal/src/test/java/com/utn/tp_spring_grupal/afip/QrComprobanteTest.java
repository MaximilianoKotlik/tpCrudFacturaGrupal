package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** QR de la factura electrónica según "Especificaciones del QR" de ARCA (versión 1). */
class QrComprobanteTest {

    /** Ejemplo oficial de la especificación: Factura A 00010-00000094 en dólares a un CUIT. */
    private static final QrComprobante.Datos EJEMPLO_OFICIAL = new QrComprobante.Datos(
            LocalDate.of(2020, 10, 13), 30000000007L, 10, 1, 94, new BigDecimal("12100.00"), "DOL",
            new BigDecimal("65.000000"), new DocumentoReceptor(80, 20000000001L), "70417054367476");

    private static String json(String url) {
        String p = url.substring(url.indexOf("?p=") + 3);
        return new String(Base64.getDecoder().decode(p), StandardCharsets.UTF_8);
    }

    @Test
    void codificaElEjemploOficialDeArcaExactamente() {
        String url = QrComprobante.url(EJEMPLO_OFICIAL);

        assertEquals("https://www.arca.gob.ar/fe/qr/?p="
                + "eyJ2ZXIiOjEsImZlY2hhIjoiMjAyMC0xMC0xMyIsImN1aXQiOjMwMDAwMDAwMDA3LCJwdG9WdGEiOjEwLCJ0aXBvQ21wIjoxLCJucm9DbXAiO"
                + "jk0LCJpbXBvcnRlIjoxMjEwMCwibW9uZWRhIjoiRE9MIiwiY3R6Ijo2NSwidGlwb0RvY1JlYyI6ODAsIm5yb0RvY1JlYyI6MjAwMDAwMDAwMD"
                + "EsInRpcG9Db2RBdXQiOiJFIiwiY29kQXV0Ijo3MDQxNzA1NDM2NzQ3Nn0=", url);
    }

    @Test
    void conservaLosDecimalesDelImporteYDeLaCotizacion() {
        String json = json(QrComprobante.url(new QrComprobante.Datos(LocalDate.of(2026, 11, 18), 20111111112L, 2, 11,
                8, new BigDecimal("1234.50"), "DOL", new BigDecimal("1050.250000"),
                new DocumentoReceptor(96, 30123456L), "86461089472534")));

        assertTrue(json.contains("\"importe\":1234.5,"), json);
        assertTrue(json.contains("\"ctz\":1050.25,"), json);
        assertTrue(json.contains("\"tipoDocRec\":96,\"nroDocRec\":30123456,"), json);
        assertTrue(json.contains("\"fecha\":\"2026-11-18\""), json);
    }

    @Test
    void consumidorFinalSinIdentificarNoInformaDocumentoDelReceptor() {
        String json = json(QrComprobante.url(new QrComprobante.Datos(LocalDate.of(2026, 11, 18), 20111111112L, 2, 6,
                8, new BigDecimal("100"), "PES", BigDecimal.ONE, DocumentoReceptor.CONSUMIDOR_FINAL,
                "86461089472534")));

        assertFalse(json.contains("tipoDocRec"), json);
        assertFalse(json.contains("nroDocRec"), json);
        assertTrue(json.contains("\"moneda\":\"PES\",\"ctz\":1,\"tipoCodAut\":\"E\""), json);
    }
}
