package com.utn.tp_spring_grupal.afip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Cliente del WebService de Factura Electrónica de ARCA (WSFE v1). El armado del XML y el parseo de las
 * respuestas viven en clases puras ({@link FeCaeSolicitarXml}, {@link WsfeRespuesta}); acá solo se orquesta
 * la llamada. Nunca se registra el token, el sign ni el XML enviado.
 */
@Service
public class AfipWsfeService {

    private static final Logger log = LoggerFactory.getLogger(AfipWsfeService.class);
    private static final String NS = "http://ar.gov.afip.dif.FEV1/";
    private static final String MONEDA_PESOS = "PES";

    private final AfipAuthService authService;

    @Value("${afip.cuit}")
    private String cuit;

    @Value("${afip.wsfe.url}")
    private String wsfeUrl;

    @Value("${afip.timeout.connect-ms:10000}")
    private int connectTimeoutMs = 10_000;

    @Value("${afip.timeout.read-ms:30000}")
    private int readTimeoutMs = 30_000;

    private volatile SoapClient soapClient;

    public AfipWsfeService(AfipAuthService authService) {
        this.authService = authService;
    }

    /** Último número autorizado para un punto de venta y tipo de comprobante (FECompUltimoAutorizado). */
    public long obtenerUltimoComprobante(int puntoVenta, int tipoComprobante) {
        String respuesta = invocar("FECompUltimoAutorizado",
                "<ar:PtoVta>" + puntoVenta + "</ar:PtoVta><ar:CbteTipo>" + tipoComprobante + "</ar:CbteTipo>");
        Element raiz = XmlUtil.parsear(respuesta).getDocumentElement();
        String nro = XmlUtil.texto(raiz, "CbteNro");
        if (nro == null) {
            throw new AfipException("ARCA no informó el último número de comprobante:\n"
                    + WsfeRespuesta.parsear(respuesta).motivoRechazo());
        }
        return Long.parseLong(nro);
    }

    /**
     * Pide el CAE (FECAESolicitar). Devuelve la respuesta parseada, sea aprobada o rechazada. Si no hay
     * respuesta utilizable (timeout, HTTP de error) lanza {@link AfipComunicacionException}: el resultado
     * queda indeterminado y hay que verificarlo con {@link #consultarComprobante}.
     */
    public WsfeRespuesta solicitarCae(SolicitudCae solicitud) {
        String respuesta = invocar("FECAESolicitar", FeCaeSolicitarXml.feCaeReq(solicitud));
        WsfeRespuesta r = WsfeRespuesta.parsear(respuesta);
        log.info("FECAESolicitar ptoVta={} tipo={} nro={} resultado={} errores={} observaciones={}",
                solicitud.puntoVenta(), solicitud.tipoComprobante(), solicitud.numero(), r.resultado(),
                r.errores(), r.observaciones());
        return r;
    }

    /** Comprobante autorizado con ese número (FECompConsultar); vacío si ARCA no lo tiene. */
    public Optional<ComprobanteAutorizado> consultarComprobante(int puntoVenta, int tipoComprobante, long numero) {
        String respuesta = invocar("FECompConsultar",
                "<ar:FeCompConsReq><ar:CbteTipo>" + tipoComprobante + "</ar:CbteTipo><ar:CbteNro>" + numero
                        + "</ar:CbteNro><ar:PtoVta>" + puntoVenta + "</ar:PtoVta></ar:FeCompConsReq>");
        return ComprobanteAutorizado.parsear(respuesta);
    }

    /** Cotización de una moneda según ARCA (FEParamGetCotizacion). PES vale 1 y no consulta a ARCA. */
    public BigDecimal obtenerCotizacion(String monId) {
        if (MONEDA_PESOS.equals(monId)) {
            return BigDecimal.ONE;
        }
        return parsearCotizacion(invocar("FEParamGetCotizacion", "<ar:MonId>" + XmlUtil.escapar(monId) + "</ar:MonId>"));
    }

    /** Extrae MonCotiz de la respuesta de FEParamGetCotizacion; si ARCA devolvió un error, lo informa con su código. */
    static BigDecimal parsearCotizacion(String respuestaXml) {
        Element raiz = XmlUtil.parsear(respuestaXml).getDocumentElement();
        String valor = XmlUtil.texto(raiz, "MonCotiz");
        if (valor == null) {
            String detalle = WsfeRespuesta.parsear(respuestaXml).motivoRechazo();
            throw new AfipException("No se pudo obtener la cotización de la moneda en ARCA:\n" + detalle);
        }
        return new BigDecimal(valor);
    }

    /** Fragmento XML de moneda y cotización de FECAEDetRequest. */
    static String xmlMoneda(String monId, BigDecimal monCotiz) {
        return FeCaeSolicitarXml.xmlMoneda(monId, monCotiz);
    }

    String invocar(String operacion, String cuerpo) {
        String[] credenciales = authService.obtenerTokenYSign();
        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"" + NS + "\">"
                + "<soapenv:Header/><soapenv:Body><ar:" + operacion + "><ar:Auth>"
                + "<ar:Token>" + credenciales[0] + "</ar:Token><ar:Sign>" + credenciales[1] + "</ar:Sign>"
                + "<ar:Cuit>" + cuit + "</ar:Cuit></ar:Auth>" + cuerpo + "</ar:" + operacion + ">"
                + "</soapenv:Body></soapenv:Envelope>";
        log.debug("WSFE {}", operacion);
        return cliente().post(NS + operacion, soap);
    }

    private SoapClient cliente() {
        SoapClient c = soapClient;
        if (c == null) {
            c = new SoapClient(wsfeUrl, connectTimeoutMs, readTimeoutMs);
            soapClient = c;
        }
        return c;
    }
}
