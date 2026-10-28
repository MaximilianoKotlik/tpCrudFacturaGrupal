package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsfeRespuestaTest {

    private static final String CAB = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><FECAESolicitarResponse xmlns=\"http://ar.gov.afip.dif.FEV1/\"><FECAESolicitarResult>";
    private static final String PIE = "</FECAESolicitarResult></FECAESolicitarResponse></soap:Body></soap:Envelope>";

    private static String cabecera(String resultado) {
        return "<FeCabResp><Cuit>20123456786</Cuit><PtoVta>2</PtoVta><CbteTipo>11</CbteTipo>"
                + "<FchProceso>20261028150000</FchProceso><CantReg>1</CantReg><Resultado>" + resultado
                + "</Resultado><Reproceso>N</Reproceso></FeCabResp>";
    }

    private static String detalle(String resultado, String extra) {
        return "<FeDetResp><FECAEDetResponse><Concepto>1</Concepto><DocTipo>99</DocTipo><DocNro>0</DocNro>"
                + "<CbteDesde>1</CbteDesde><CbteHasta>1</CbteHasta><CbteFch>20261028</CbteFch><Resultado>"
                + resultado + "</Resultado>" + extra + "</FECAEDetResponse></FeDetResp>";
    }

    private static String obs(String code, String msg) {
        return "<Obs><Code>" + code + "</Code><Msg>" + msg + "</Msg></Obs>";
    }

    @Test
    void respuestaAprobadaTraeCaeYVencimiento() {
        String xml = CAB + cabecera("A") + detalle("A", "<CAE>76433412345678</CAE><CAEFchVto>20261107</CAEFchVto>") + PIE;

        WsfeRespuesta r = WsfeRespuesta.parsear(xml);

        assertTrue(r.aprobada());
        assertEquals("A", r.resultado());
        assertEquals("76433412345678", r.cae());
        assertEquals("20261107", r.caeFchVto());
        assertTrue(r.errores().isEmpty());
    }

    @Test
    void aprobadaConObservacionesLasConservaSinConvertirlaEnRechazo() {
        String xml = CAB + cabecera("A") + detalle("A", "<Observaciones>" + obs("10063", "Aviso")
                + "</Observaciones><CAE>76433412345678</CAE><CAEFchVto>20261107</CAEFchVto>") + PIE;

        WsfeRespuesta r = WsfeRespuesta.parsear(xml);

        assertTrue(r.aprobada());
        assertEquals(1, r.observaciones().size());
        assertEquals("10063", r.observaciones().get(0).codigo());
    }

    @Test
    void rechazoConVariasObservacionesLasListaConCodigoYMensaje() {
        String xml = CAB + cabecera("R") + detalle("R", "<Observaciones>" + obs("10016", "Numero invalido")
                + obs("10015", "DocNro invalido") + "</Observaciones>") + PIE;

        WsfeRespuesta r = WsfeRespuesta.parsear(xml);

        assertFalse(r.aprobada());
        assertEquals("R", r.resultado());
        assertNull(r.cae());
        assertEquals(2, r.observaciones().size());
        assertEquals("[10016] Numero invalido\n[10015] DocNro invalido", r.motivoRechazo());
    }

    @Test
    void errorGlobalDeArcaSeInformaConSuCodigo() {
        String xml = CAB + "<Errors><Err><Code>600</Code><Msg>ValidacionDeToken: No validaron las fechas</Msg></Err>"
                + "<Err><Code>501</Code><Msg>Otro</Msg></Err></Errors>" + PIE;

        WsfeRespuesta r = WsfeRespuesta.parsear(xml);

        assertFalse(r.aprobada());
        assertNull(r.resultado());
        assertEquals(2, r.errores().size());
        assertEquals("[600] ValidacionDeToken: No validaron las fechas\n[501] Otro", r.motivoRechazo());
    }

    @Test
    void erroresYObservacionesSeCombinanEnElMotivo() {
        String xml = CAB + cabecera("R") + detalle("R", "<Observaciones>" + obs("10002", "Pto de venta")
                + "</Observaciones>") + "<Errors><Err><Code>7</Code><Msg>Global</Msg></Err></Errors>" + PIE;

        assertEquals("[7] Global\n[10002] Pto de venta", WsfeRespuesta.parsear(xml).motivoRechazo());
    }

    @Test
    void rechazoSinDetalleTieneUnMotivoGenerico() {
        String xml = CAB + cabecera("R") + detalle("R", "") + PIE;

        assertTrue(WsfeRespuesta.parsear(xml).motivoRechazo().contains("R"));
    }

    @Test
    void soapFaultSeTrataComoError() {
        String xml = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault>"
                + "<faultcode>soap:Server</faultcode><faultstring>Server was unable to process</faultstring>"
                + "</soap:Fault></soap:Body></soap:Envelope>";

        WsfeRespuesta r = WsfeRespuesta.parsear(xml);

        assertFalse(r.aprobada());
        assertTrue(r.motivoRechazo().contains("Server was unable to process"));
    }

    @Test
    void respuestaQueNoEsXmlLanzaUnErrorDeComunicacionSinDetallesTecnicos() {
        AfipComunicacionException e = assertThrows(AfipComunicacionException.class,
                () -> WsfeRespuesta.parsear("<html><body>502 Bad Gateway"));
        assertTrue(e.getMessage().contains("no se pudo interpretar"), e.getMessage());
        assertFalse(e.getMessage().contains("502"), e.getMessage());
        assertFalse(e.getMessage().contains("<"), e.getMessage());
    }

    @Test
    void respuestaConDoctypeSeRechazaPorSeguridad() {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>";
        assertThrows(RuntimeException.class, () -> WsfeRespuesta.parsear(xml));
    }

    @Test
    void decodificaIso88591DeclaradoEnElXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + CAB + cabecera("R")
                + detalle("R", "<Observaciones>" + obs("10015", "Número de documento inválido")
                + "</Observaciones>") + PIE;
        byte[] bytes = xml.getBytes(StandardCharsets.ISO_8859_1);

        WsfeRespuesta r = WsfeRespuesta.parsear(XmlUtil.decodificar(bytes, "text/xml"));

        assertEquals("[10015] Número de documento inválido", r.motivoRechazo());
    }

    @Test
    void decodificaSegunElCharsetDelContentType() {
        byte[] bytes = "<a>ñ</a>".getBytes(StandardCharsets.ISO_8859_1);
        assertEquals("<a>ñ</a>", XmlUtil.decodificar(bytes, "text/xml; charset=ISO-8859-1"));
    }

    @Test
    void sinCharsetNiDeclaracionAsumeUtf8() {
        byte[] bytes = "<a>ñ</a>".getBytes(StandardCharsets.UTF_8);
        assertEquals("<a>ñ</a>", XmlUtil.decodificar(bytes, null));
    }
}
