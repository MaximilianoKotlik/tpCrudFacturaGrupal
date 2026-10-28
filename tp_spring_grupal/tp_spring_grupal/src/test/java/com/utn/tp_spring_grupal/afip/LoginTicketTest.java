package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginTicketTest {

    private static String respuesta(String vencimiento) {
        String interno = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><loginTicketResponse version=\"1.0\">"
                + "<header><source>CN=wsaahomo</source><destination>SERIALNUMBER=CUIT 20123456786</destination>"
                + "<uniqueId>123</uniqueId><generationTime>2026-10-28T15:00:00.000-03:00</generationTime>"
                + "<expirationTime>" + vencimiento + "</expirationTime></header>"
                + "<credentials><token>TOKEN-X</token><sign>SIGN-Y</sign></credentials></loginTicketResponse>";
        String escapado = interno.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
                + "<loginCmsResponse xmlns=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">"
                + "<loginCmsReturn>" + escapado + "</loginCmsReturn></loginCmsResponse></soapenv:Body></soapenv:Envelope>";
    }

    private static String fault(String code, String msg) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><soapenv:Fault>"
                + "<faultcode xmlns:ns1=\"http://xml.apache.org/axis/\">" + code + "</faultcode><faultstring>" + msg
                + "</faultstring></soapenv:Fault></soapenv:Body></soapenv:Envelope>";
    }

    @Test
    void leeTokenSignYVencimientoRealDeLaRespuesta() {
        LoginTicket t = LoginTicket.parsear(respuesta("2026-10-29T03:00:00.000-03:00"));

        assertEquals("TOKEN-X", t.token());
        assertEquals("SIGN-Y", t.sign());
        assertEquals(ZonedDateTime.parse("2026-10-29T03:00:00-03:00").toInstant(), t.vencimiento().toInstant());
    }

    @Test
    void esVigenteHastaCincoMinutosAntesDelVencimiento() {
        LoginTicket t = LoginTicket.parsear(respuesta("2026-10-29T03:00:00.000-03:00"));

        assertTrue(t.vigente(ZonedDateTime.parse("2026-10-29T02:54:00-03:00")));
        assertFalse(t.vigente(ZonedDateTime.parse("2026-10-29T02:56:00-03:00")));
        assertFalse(t.vigente(ZonedDateTime.parse("2026-10-29T04:00:00-03:00")));
    }

    @Test
    void tokenYaEmitidoSeExplicaSinMostrarElXmlCrudo() {
        AfipAutenticacionException e = assertThrows(AfipAutenticacionException.class, () -> LoginTicket.parsear(
                fault("ns1:coe.alreadyAuthenticated", "El CEE ya posee un TA valido para el acceso al WSN solicitado")));

        assertTrue(e.getMessage().contains("ticket de acceso vigente"));
        assertFalse(e.getMessage().contains("<"));
    }

    @Test
    void otroFaultInformaCodigoYMensaje() {
        AfipAutenticacionException e = assertThrows(AfipAutenticacionException.class, () -> LoginTicket.parsear(
                fault("ns1:cms.cert.expired", "Certificado expirado")));

        assertTrue(e.getMessage().contains("cms.cert.expired"));
        assertTrue(e.getMessage().contains("Certificado expirado"));
    }

    @Test
    void respuestaSinCredencialesLanza() {
        assertThrows(AfipAutenticacionException.class, () -> LoginTicket.parsear("<a><b/></a>"));
    }
}
