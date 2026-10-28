package com.utn.tp_spring_grupal.afip;

import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeCaeSolicitarXmlTest {

    private static final String NS = "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\"";

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static SolicitudCae facturaC(DocumentoReceptor doc, String total) {
        return new SolicitudCae(2, 11, 1, LocalDate.of(2026, 10, 28), d(total), d(total), d("0.00"),
                doc, "PES", BigDecimal.ONE, 5, List.of());
    }

    private static Document parsear(String fragmento) {
        return XmlUtil.parsear("<root " + NS + ">" + fragmento + "</root>");
    }

    private static String texto(Document doc, String tag) {
        return XmlUtil.texto(doc.getDocumentElement(), tag);
    }

    private static List<String> hijos(Document doc, String padre) {
        Element e = (Element) doc.getElementsByTagNameNS("*", padre).item(0);
        List<String> nombres = new ArrayList<>();
        NodeList hs = e.getChildNodes();
        for (int i = 0; i < hs.getLength(); i++) {
            if (hs.item(i).getNodeType() == Node.ELEMENT_NODE) {
                nombres.add(hs.item(i).getLocalName());
            }
        }
        return nombres;
    }

    @Test
    void facturaCNoLlevaBloqueIvaYTodosLosImportesAdicionalesVanEnCero() {
        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(facturaC(DocumentoReceptor.CONSUMIDOR_FINAL, "1500.50")));

        assertEquals(0, doc.getElementsByTagNameNS("*", "Iva").getLength());
        assertEquals("1500.50", texto(doc, "ImpTotal"));
        assertEquals("1500.50", texto(doc, "ImpNeto"));
        assertEquals("0.00", texto(doc, "ImpIVA"));
        assertEquals("0.00", texto(doc, "ImpTotConc"));
        assertEquals("0.00", texto(doc, "ImpOpEx"));
        assertEquals("0.00", texto(doc, "ImpTrib"));
        assertEquals("1", texto(doc, "Concepto"));
    }

    @Test
    void cabeceraYCamposDeIdentificacionDelComprobante() {
        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(facturaC(new DocumentoReceptor(80, 20123456786L), "10.00")));

        assertEquals("1", texto(doc, "CantReg"));
        assertEquals("2", texto(doc, "PtoVta"));
        assertEquals("11", texto(doc, "CbteTipo"));
        assertEquals("80", texto(doc, "DocTipo"));
        assertEquals("20123456786", texto(doc, "DocNro"));
        assertEquals("1", texto(doc, "CbteDesde"));
        assertEquals("1", texto(doc, "CbteHasta"));
        assertEquals("20261028", texto(doc, "CbteFch"));
    }

    @Test
    void consumidorFinalSeEnviaComoDocTipo99DocNro0() {
        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(facturaC(DocumentoReceptor.CONSUMIDOR_FINAL, "10.00")));

        assertEquals("99", texto(doc, "DocTipo"));
        assertEquals("0", texto(doc, "DocNro"));
    }

    @Test
    void fechaSeFormateaYYYYMMDDConCerosAlaIzquierda() {
        SolicitudCae s = new SolicitudCae(2, 11, 7, LocalDate.of(2026, 1, 5), d("10.00"), d("10.00"), d("0.00"),
                DocumentoReceptor.CONSUMIDOR_FINAL, "PES", BigDecimal.ONE, 5, List.of());
        assertEquals("20260105", texto(parsear(FeCaeSolicitarXml.feCaeReq(s)), "CbteFch"));
    }

    @Test
    void incluyeLaCondicionIvaDelReceptorDespuesDeLaCotizacion() {
        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(facturaC(DocumentoReceptor.CONSUMIDOR_FINAL, "10.00")));

        assertEquals("5", texto(doc, "CondicionIVAReceptorId"));
        List<String> orden = hijos(doc, "FECAEDetRequest");
        assertTrue(orden.indexOf("CondicionIVAReceptorId") > orden.indexOf("MonCotiz"), orden.toString());
        assertEquals(orden.size() - 1, orden.indexOf("CondicionIVAReceptorId"), orden.toString());
    }

    @Test
    void facturaAConDosAlicuotasArmaUnAlicIvaPorAlicuotaYLosTotalesCierran() {
        List<ResumenAlicuota> desglose = List.of(
                new ResumenAlicuota(4, d("10.50"), d("400.00"), d("42.00")),
                new ResumenAlicuota(5, d("21.00"), d("600.00"), d("126.00")));
        SolicitudCae s = new SolicitudCae(2, 1, 5, LocalDate.of(2026, 10, 28), d("1168.00"), d("1000.00"),
                d("168.00"), new DocumentoReceptor(80, 30500010912L), "PES", BigDecimal.ONE, 1, desglose);

        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(s));

        NodeList alic = doc.getElementsByTagNameNS("*", "AlicIva");
        assertEquals(2, alic.getLength());
        Element primera = (Element) alic.item(0);
        assertEquals("4", XmlUtil.texto(primera, "Id"));
        assertEquals("400.00", XmlUtil.texto(primera, "BaseImp"));
        assertEquals("42.00", XmlUtil.texto(primera, "Importe"));
        Element segunda = (Element) alic.item(1);
        assertEquals("5", XmlUtil.texto(segunda, "Id"));
        assertEquals("600.00", XmlUtil.texto(segunda, "BaseImp"));
        assertEquals("126.00", XmlUtil.texto(segunda, "Importe"));
        assertEquals("1000.00", texto(doc, "ImpNeto"));
        assertEquals("168.00", texto(doc, "ImpIVA"));
        List<String> orden = hijos(doc, "FECAEDetRequest");
        assertTrue(orden.indexOf("Iva") > orden.indexOf("CondicionIVAReceptorId"), orden.toString());
    }

    @Test
    void alicuotaCeroSeInformaIgualEnElDesglose() {
        List<ResumenAlicuota> desglose = List.of(new ResumenAlicuota(3, d("0.00"), d("500.00"), d("0.00")));
        SolicitudCae s = new SolicitudCae(2, 6, 1, LocalDate.of(2026, 10, 28), d("500.00"), d("500.00"),
                d("0.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "PES", BigDecimal.ONE, 5, desglose);

        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(s));

        assertEquals(1, doc.getElementsByTagNameNS("*", "AlicIva").getLength());
        assertEquals("3", texto(doc, "Id"));
    }

    @Test
    void desgloseQueNoSumaElNetoSeRechazaAntesDeEnviar() {
        List<ResumenAlicuota> desglose = List.of(new ResumenAlicuota(5, d("21.00"), d("900.00"), d("189.00")));
        SolicitudCae s = new SolicitudCae(2, 1, 5, LocalDate.of(2026, 10, 28), d("1210.00"), d("1000.00"),
                d("210.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "PES", BigDecimal.ONE, 1, desglose);

        assertThrows(IllegalArgumentException.class, () -> FeCaeSolicitarXml.feCaeReq(s));
    }

    @Test
    void desgloseQueNoSumaElIvaSeRechazaAntesDeEnviar() {
        List<ResumenAlicuota> desglose = List.of(new ResumenAlicuota(5, d("21.00"), d("1000.00"), d("200.00")));
        SolicitudCae s = new SolicitudCae(2, 1, 5, LocalDate.of(2026, 10, 28), d("1210.00"), d("1000.00"),
                d("210.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "PES", BigDecimal.ONE, 1, desglose);

        assertThrows(IllegalArgumentException.class, () -> FeCaeSolicitarXml.feCaeReq(s));
    }

    @Test
    void ivaMayorACeroSinDesgloseSeRechaza() {
        SolicitudCae s = new SolicitudCae(2, 11, 1, LocalDate.of(2026, 10, 28), d("121.00"), d("100.00"),
                d("21.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "PES", BigDecimal.ONE, 5, List.of());

        assertThrows(IllegalArgumentException.class, () -> FeCaeSolicitarXml.feCaeReq(s));
    }

    @Test
    void totalQueNoEsNetoMasIvaSeRechaza() {
        SolicitudCae s = new SolicitudCae(2, 11, 1, LocalDate.of(2026, 10, 28), d("101.00"), d("100.00"),
                d("0.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "PES", BigDecimal.ONE, 5, List.of());

        assertThrows(IllegalArgumentException.class, () -> FeCaeSolicitarXml.feCaeReq(s));
    }

    @Test
    void monedaExtranjeraEnviaLaCotizacionConSusDecimales() {
        SolicitudCae s = new SolicitudCae(2, 11, 1, LocalDate.of(2026, 10, 28), d("10.00"), d("10.00"),
                d("0.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "060", d("1202.3588"), 5, List.of());

        Document doc = parsear(FeCaeSolicitarXml.feCaeReq(s));

        assertEquals("060", texto(doc, "MonId"));
        assertEquals("1202.3588", texto(doc, "MonCotiz"));
    }

    @Test
    void escapaLosCaracteresEspecialesDeLosValoresDeTexto() {
        SolicitudCae s = new SolicitudCae(2, 11, 1, LocalDate.of(2026, 10, 28), d("10.00"), d("10.00"),
                d("0.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "A&<B>", BigDecimal.ONE, 5, List.of());

        assertEquals("A&<B>", texto(parsear(FeCaeSolicitarXml.feCaeReq(s)), "MonId"));
    }
}
