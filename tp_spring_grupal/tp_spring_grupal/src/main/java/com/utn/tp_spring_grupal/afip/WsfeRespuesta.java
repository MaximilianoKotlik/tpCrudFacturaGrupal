package com.utn.tp_spring_grupal.afip;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de FECAESolicitar ya parseado: resultado (A/R), CAE, errores (Errors/Err) y observaciones
 * (Observaciones/Obs). Clase pura sobre el XML de respuesta.
 */
public record WsfeRespuesta(String resultado, String cae, String caeFchVto,
                            List<Mensaje> errores, List<Mensaje> observaciones) {

    public record Mensaje(String codigo, String mensaje) {
        @Override
        public String toString() {
            return codigo != null ? "[" + codigo + "] " + mensaje : mensaje;
        }
    }

    public boolean aprobada() {
        return "A".equals(resultado) && cae != null;
    }

    /** Código y mensaje de ARCA, uno por línea: primero los errores y después las observaciones. */
    public String motivoRechazo() {
        List<String> lineas = new ArrayList<>();
        errores.forEach(m -> lineas.add(m.toString()));
        observaciones.forEach(m -> lineas.add(m.toString()));
        if (!lineas.isEmpty()) {
            return String.join("\n", lineas);
        }
        return resultado != null
                ? "ARCA rechazó el comprobante (Resultado=" + resultado + ") sin informar el detalle."
                : "ARCA no informó el resultado de la autorización.";
    }

    public static WsfeRespuesta parsear(String xml) {
        Element raiz = XmlUtil.parsear(xml).getDocumentElement();

        List<Mensaje> errores = new ArrayList<>();
        for (Element err : XmlUtil.todos(raiz, "Err")) {
            errores.add(new Mensaje(XmlUtil.texto(err, "Code"), XmlUtil.texto(err, "Msg")));
        }
        Element fault = XmlUtil.primero(raiz, "Fault");
        if (fault != null) {
            errores.add(new Mensaje(XmlUtil.texto(fault, "faultcode"), XmlUtil.texto(fault, "faultstring")));
        }
        List<Mensaje> observaciones = new ArrayList<>();
        for (Element obs : XmlUtil.todos(raiz, "Obs")) {
            observaciones.add(new Mensaje(XmlUtil.texto(obs, "Code"), XmlUtil.texto(obs, "Msg")));
        }

        // El Resultado del comprobante (FECAEDetResponse) prevalece sobre el de la cabecera.
        Element detalle = XmlUtil.primero(raiz, "FECAEDetResponse");
        String resultado = detalle != null ? XmlUtil.texto(detalle, "Resultado") : null;
        if (resultado == null) {
            resultado = XmlUtil.texto(raiz, "Resultado");
        }
        return new WsfeRespuesta(resultado, XmlUtil.texto(raiz, "CAE"), XmlUtil.texto(raiz, "CAEFchVto"),
                List.copyOf(errores), List.copyOf(observaciones));
    }
}
