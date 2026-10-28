package com.utn.tp_spring_grupal.afip;

import org.w3c.dom.Element;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/** Ticket de acceso de WSAA (token + sign + vencimiento real). Solo vive en memoria: nunca se persiste ni se loguea. */
record LoginTicket(String token, String sign, ZonedDateTime vencimiento) {

    private static final long MARGEN_MINUTOS = 5;

    boolean vigente(ZonedDateTime ahora) {
        return ahora.isBefore(vencimiento.minusMinutes(MARGEN_MINUTOS));
    }

    @Override
    public String toString() {
        return "LoginTicket[vencimiento=" + vencimiento + "]";
    }

    /** Interpreta la respuesta SOAP de loginCms; un Fault se traduce a un mensaje claro. */
    static LoginTicket parsear(String soapResponse) {
        Element raiz = XmlUtil.parsear(soapResponse).getDocumentElement();

        Element fault = XmlUtil.primero(raiz, "Fault");
        if (fault != null) {
            String code = XmlUtil.texto(fault, "faultcode");
            String msg = XmlUtil.texto(fault, "faultstring");
            if (code != null && code.contains("alreadyAuthenticated")) {
                throw new AfipAutenticacionException("ARCA ya emitió un ticket de acceso vigente para este "
                        + "certificado y servicio (probablemente lo tomó otro proceso o una ejecución anterior de "
                        + "la app) y no permite pedir otro hasta que venza (hasta ~12 h). Esperá a que venza.");
            }
            throw new AfipAutenticacionException("ARCA rechazó la autenticación (" + code + "): " + msg);
        }

        String interno = XmlUtil.texto(raiz, "loginCmsReturn");
        if (interno == null) {
            throw new AfipAutenticacionException("La respuesta de ARCA no trae un ticket de acceso.");
        }
        Element ticket = XmlUtil.parsear(interno).getDocumentElement();
        String token = XmlUtil.texto(ticket, "token");
        String sign = XmlUtil.texto(ticket, "sign");
        String expiracion = XmlUtil.texto(ticket, "expirationTime");
        if (token == null || sign == null || expiracion == null) {
            throw new AfipAutenticacionException("El ticket de acceso de ARCA está incompleto.");
        }
        return new LoginTicket(token, sign, OffsetDateTime.parse(expiracion).toZonedDateTime());
    }
}
