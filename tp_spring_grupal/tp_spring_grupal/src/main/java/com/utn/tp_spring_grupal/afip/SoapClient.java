package com.utn.tp_spring_grupal.afip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** POST SOAP con timeouts de conexión y lectura. Nunca registra el cuerpo enviado (lleva token y sign). */
final class SoapClient {

    private static final Logger log = LoggerFactory.getLogger(SoapClient.class);
    private static final int MAX_CUERPO_EN_LOG = 300;

    private final String url;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    SoapClient(String url, int connectTimeoutMs, int readTimeoutMs) {
        this.url = url;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    String post(String soapAction, String xml) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", soapAction);
            conn.setDoOutput(true);
            conn.getOutputStream().write(xml.getBytes(StandardCharsets.UTF_8));

            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String cuerpo = is == null ? "" : XmlUtil.decodificar(is.readAllBytes(), conn.getContentType());
            if (status >= 400) {
                // El detalle técnico (página HTML, SOAP Fault) queda en el log y en getCuerpo(), no en el mensaje.
                log.warn("ARCA respondió HTTP {}: {}", status, recortar(cuerpo));
                throw new AfipComunicacionException("ARCA no está disponible en este momento. "
                        + "Intentá de nuevo en unos minutos.", status, cuerpo);
            }
            return cuerpo;
        } catch (IOException e) {
            log.warn("Falla de comunicación con ARCA", e);
            throw new AfipComunicacionException(mensajeDe(e), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String mensajeDe(IOException e) {
        if (e instanceof SocketTimeoutException) {
            return "ARCA no respondió a tiempo. Intentá de nuevo en unos minutos.";
        }
        if (e instanceof ConnectException || e instanceof UnknownHostException || e instanceof NoRouteToHostException) {
            return "No se pudo conectar con ARCA. Verificá la conexión a internet e intentá de nuevo.";
        }
        return "Se interrumpió la comunicación con ARCA. Intentá de nuevo en unos minutos.";
    }

    private static String recortar(String cuerpo) {
        return cuerpo.length() > MAX_CUERPO_EN_LOG ? cuerpo.substring(0, MAX_CUERPO_EN_LOG) + "…" : cuerpo;
    }
}
