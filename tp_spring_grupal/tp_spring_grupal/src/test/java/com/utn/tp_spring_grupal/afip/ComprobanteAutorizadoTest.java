package com.utn.tp_spring_grupal.afip;

import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprobanteAutorizadoTest {

    private static String resultGet(String resultado, String cae, String total, int docTipo, long docNro) {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + "<FECompConsultarResponse xmlns=\"http://ar.gov.afip.dif.FEV1/\"><FECompConsultarResult><ResultGet>"
                + "<Concepto>1</Concepto><DocTipo>" + docTipo + "</DocTipo><DocNro>" + docNro + "</DocNro>"
                + "<CbteDesde>11</CbteDesde><CbteHasta>11</CbteHasta><CbteFch>20261028</CbteFch>"
                + "<ImpTotal>" + total + "</ImpTotal><MonId>PES</MonId><Resultado>" + resultado + "</Resultado>"
                + (cae != null ? "<CodAutorizacion>" + cae + "</CodAutorizacion>" : "")
                + "<FchVto>20261107</FchVto></ResultGet></FECompConsultarResult></FECompConsultarResponse>"
                + "</soap:Body></soap:Envelope>";
    }

    private static SolicitudCae solicitud(String total, DocumentoReceptor doc) {
        return new SolicitudCae(2, 11, 11, LocalDate.of(2026, 10, 28), new BigDecimal(total), new BigDecimal(total),
                new BigDecimal("0.00"), doc, "PES", BigDecimal.ONE, 5, List.<ResumenAlicuota>of());
    }

    @Test
    void comprobanteAutorizadoTraeCaeVencimientoImporteYReceptor() {
        Optional<ComprobanteAutorizado> r = ComprobanteAutorizado.parsear(
                resultGet("A", "76433412345678", "121.00", 80, 20123456786L));

        assertTrue(r.isPresent());
        assertEquals("76433412345678", r.get().cae());
        assertEquals("20261107", r.get().caeFchVto());
        assertEquals(0, new BigDecimal("121.00").compareTo(r.get().importeTotal()));
        assertEquals(new DocumentoReceptor(80, 20123456786L), r.get().documento());
    }

    @Test
    void comprobanteInexistenteError602DevuelveVacio() {
        String xml = "<FECompConsultarResult><Errors><Err><Code>602</Code><Msg>Comprobante no existe</Msg></Err>"
                + "</Errors></FECompConsultarResult>";

        assertTrue(ComprobanteAutorizado.parsear(xml).isEmpty());
    }

    @Test
    void comprobanteNoAprobadoSeConsideraNoAutorizado() {
        assertTrue(ComprobanteAutorizado.parsear(resultGet("R", null, "10.00", 99, 0)).isEmpty());
    }

    @Test
    void otroErrorDeArcaSePropagaEnVezDeConfundirseConInexistente() {
        String xml = "<FECompConsultarResult><Errors><Err><Code>600</Code><Msg>Token vencido</Msg></Err>"
                + "</Errors></FECompConsultarResult>";

        AfipException e = assertThrows(AfipException.class, () -> ComprobanteAutorizado.parsear(xml));
        assertTrue(e.getMessage().contains("600"));
    }

    @Test
    void coincideSoloSiImporteYDocumentoSonLosMismos() {
        ComprobanteAutorizado c = ComprobanteAutorizado.parsear(
                resultGet("A", "76433412345678", "121.00", 99, 0)).orElseThrow();

        assertTrue(c.coincideCon(solicitud("121", DocumentoReceptor.CONSUMIDOR_FINAL)));
        assertFalse(c.coincideCon(solicitud("122.00", DocumentoReceptor.CONSUMIDOR_FINAL)));
        assertFalse(c.coincideCon(solicitud("121.00", new DocumentoReceptor(96, 30123456L))));
    }
}
