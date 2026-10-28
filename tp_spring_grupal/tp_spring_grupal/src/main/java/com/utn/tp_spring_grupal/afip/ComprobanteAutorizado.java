package com.utn.tp_spring_grupal.afip;

import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.Optional;

/** Comprobante ya autorizado según FECompConsultar; sirve para recuperar un CAE cuya respuesta se perdió. */
public record ComprobanteAutorizado(String cae, String caeFchVto, String cbteFch, BigDecimal importeTotal,
                                    DocumentoReceptor documento) {

    private static final String CODIGO_NO_EXISTE = "602";

    /** Vacío si ARCA no tiene el comprobante o no está aprobado; cualquier otro error de ARCA se propaga. */
    public static Optional<ComprobanteAutorizado> parsear(String xml) {
        Element raiz = XmlUtil.parsear(xml).getDocumentElement();
        Element resultGet = XmlUtil.primero(raiz, "ResultGet");
        if (resultGet == null) {
            WsfeRespuesta r = WsfeRespuesta.parsear(xml);
            boolean soloNoExiste = !r.errores().isEmpty()
                    && r.errores().stream().allMatch(m -> CODIGO_NO_EXISTE.equals(m.codigo()));
            if (r.errores().isEmpty() || soloNoExiste) {
                return Optional.empty();
            }
            throw new AfipException("ARCA no pudo informar el comprobante:\n" + r.motivoRechazo());
        }
        String cae = XmlUtil.texto(resultGet, "CodAutorizacion");
        if (!"A".equals(XmlUtil.texto(resultGet, "Resultado")) || cae == null) {
            return Optional.empty();
        }
        return Optional.of(new ComprobanteAutorizado(cae, XmlUtil.texto(resultGet, "FchVto"),
                XmlUtil.texto(resultGet, "CbteFch"),
                new BigDecimal(XmlUtil.texto(resultGet, "ImpTotal")),
                new DocumentoReceptor(Integer.parseInt(XmlUtil.texto(resultGet, "DocTipo")),
                        Long.parseLong(XmlUtil.texto(resultGet, "DocNro")))));
    }

    /** El comprobante de ARCA es el que se iba a emitir: mismo importe total y mismo receptor. */
    public boolean coincideCon(SolicitudCae s) {
        return importeTotal.compareTo(s.importeTotal()) == 0 && documento.equals(s.documento());
    }
}
