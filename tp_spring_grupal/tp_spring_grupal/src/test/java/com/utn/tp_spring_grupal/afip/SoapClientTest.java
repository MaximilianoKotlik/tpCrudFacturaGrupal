package com.utn.tp_spring_grupal.afip;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contra un servidor HTTP local: estos tests no llaman a ARCA. */
class SoapClientTest {

    private HttpServer server;

    @BeforeEach
    void levantar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void bajar() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/ws";
    }

    @Test
    void enviaElCuerpoYElSoapActionYDevuelveLaRespuesta() throws Exception {
        AtomicReference<String> accion = new AtomicReference<>();
        AtomicReference<String> cuerpo = new AtomicReference<>();
        server.createContext("/ws", ex -> {
            accion.set(ex.getRequestHeaders().getFirst("SOAPAction"));
            cuerpo.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] r = "<ok/>".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, r.length);
            ex.getResponseBody().write(r);
            ex.close();
        });

        String resp = new SoapClient(url(), 1000, 1000).post("urn:op", "<x>ñ</x>");

        assertEquals("<ok/>", resp);
        assertEquals("urn:op", accion.get());
        assertEquals("<x>ñ</x>", cuerpo.get());
    }

    @Test
    void decodificaLaRespuestaSegunElCharsetDelServidor() throws Exception {
        server.createContext("/ws", ex -> {
            byte[] r = "<m>Número inválido</m>".getBytes(StandardCharsets.ISO_8859_1);
            ex.getResponseHeaders().add("Content-Type", "text/xml; charset=ISO-8859-1");
            ex.sendResponseHeaders(200, r.length);
            ex.getResponseBody().write(r);
            ex.close();
        });

        assertEquals("<m>Número inválido</m>", new SoapClient(url(), 1000, 1000).post("a", "<x/>"));
    }

    @Test
    void siElServidorNoRespondeAntesDelTimeoutDeLecturaLanzaComunicacion() {
        server.createContext("/ws", ex -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {
            }
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });

        long ini = System.currentTimeMillis();
        assertThrows(AfipComunicacionException.class, () -> new SoapClient(url(), 1000, 150).post("a", "<x/>"));
        assertTrue(System.currentTimeMillis() - ini < 700, "debe cortar por timeout de lectura");
    }

    @Test
    void errorHttpLanzaConElEstadoYElCuerpoTruncado() {
        server.createContext("/ws", ex -> {
            byte[] r = "e".repeat(5000).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(500, r.length);
            ex.getResponseBody().write(r);
            ex.close();
        });

        AfipComunicacionException e = assertThrows(AfipComunicacionException.class,
                () -> new SoapClient(url(), 1000, 1000).post("a", "<x/>"));
        assertEquals(500, e.getEstadoHttp());
        assertTrue(e.getMessage().contains("no está disponible"), e.getMessage());
        assertFalse(e.getMessage().contains("eee"), "no debe mostrar el cuerpo de la respuesta");
    }

    @Test
    void paginaHtmlDeErrorNuncaLlegaAlMensajeParaElUsuario() {
        server.createContext("/ws", ex -> {
            byte[] r = "<!DOCTYPE html><html><head><title>The resource cannot be found.</title></head></html>"
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, r.length);
            ex.getResponseBody().write(r);
            ex.close();
        });

        AfipComunicacionException e = assertThrows(AfipComunicacionException.class,
                () -> new SoapClient(url(), 1000, 1000).post("a", "<x/>"));
        assertFalse(e.getMessage().contains("<"), e.getMessage());
        assertFalse(e.getMessage().contains("resource cannot be found"), e.getMessage());
        assertTrue(e.getCuerpo().contains("resource cannot be found"), "el cuerpo queda aparte para el log");
    }

    @Test
    void elTimeoutSeExplicaEnLenguajeNormal() {
        server.createContext("/ws", ex -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
            }
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });

        AfipComunicacionException e = assertThrows(AfipComunicacionException.class,
                () -> new SoapClient(url(), 1000, 100).post("a", "<x/>"));
        assertTrue(e.getMessage().contains("no respondió a tiempo"), e.getMessage());
        assertFalse(e.getMessage().contains("Exception"), e.getMessage());
    }

    @Test
    void conexionRechazadaLanzaComunicacion() {
        String urlMuerta = url();
        server.stop(0);
        AfipComunicacionException e = assertThrows(AfipComunicacionException.class,
                () -> new SoapClient(urlMuerta, 500, 500).post("a", "<x/>"));
        assertTrue(e.getMessage().contains("No se pudo conectar con ARCA"), e.getMessage());
        assertFalse(e.getMessage().contains("Exception"), e.getMessage());
    }
}
