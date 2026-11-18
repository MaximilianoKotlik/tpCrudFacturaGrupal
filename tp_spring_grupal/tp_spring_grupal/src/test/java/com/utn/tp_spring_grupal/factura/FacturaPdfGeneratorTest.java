package com.utn.tp_spring_grupal.factura;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.utn.tp_spring_grupal.afip.DocumentoReceptor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PDF del comprobante con el formato de las facturas electrónicas de ARCA (original, duplicado y triplicado). */
class FacturaPdfGeneratorTest {

    private final FacturaPdfGenerator generador = new FacturaPdfGenerator();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static ComprobantePdf comprobante(TipoComprobante tipo, String moneda, String cotizacion, boolean anulada,
                                              DocumentoReceptor documento) {
        boolean discrimina = tipo != TipoComprobante.C;
        List<ComprobantePdf.Renglon> renglones = List.of(
                new ComprobantePdf.Renglon("ART-1", "Mouse inalámbrico", 2, bd("100.00"), bd("10"),
                        discrimina ? bd("21") : BigDecimal.ZERO, bd("180.00"), discrimina ? bd("37.80") : bd("0.00"),
                        discrimina ? bd("217.80") : bd("180.00")),
                new ComprobantePdf.Renglon("ART-2", "Yerba mate", 1, bd("200.00"), BigDecimal.ZERO,
                        discrimina ? bd("10.5") : BigDecimal.ZERO, bd("200.00"), discrimina ? bd("21.00") : bd("0.00"),
                        discrimina ? bd("221.00") : bd("200.00")));
        BigDecimal iva = discrimina ? bd("58.80") : bd("0.00");
        return new ComprobantePdf(
                new ComprobantePdf.Emisor("Comercial Ejemplo SRL", "Av. Siempreviva 742 - CABA",
                        discrimina ? "IVA Responsable Inscripto" : "Responsable Monotributo", "20111111112",
                        "901-123456-7", LocalDate.of(2020, 3, 1)),
                tipo, 2, 8, LocalDate.of(2026, 11, 18),
                new ComprobantePdf.Receptor(documento, documento.tipo() == 99 ? "Consumidor Final" : "Juan Perez",
                        documento.tipo() == 99 ? "Consumidor Final" : "IVA Responsable Inscripto",
                        documento.tipo() == 99 ? null : "San Martín 123"),
                moneda, moneda.equals("PES") ? "Pesos" : "Dólar estadounidense", bd(cotizacion),
                renglones, bd("380.00"), iva, bd("380.00").add(iva),
                "86461089472534", LocalDate.of(2026, 11, 28), "Entrega en sucursal", anulada,
                "https://www.arca.gob.ar/fe/qr/?p=eyJ2ZXIiOjF9");
    }

    private static ComprobantePdf facturaA() {
        return comprobante(TipoComprobante.A, "PES", "1", false, new DocumentoReceptor(80, 20281234567L));
    }

    private record Leido(int paginas, List<String> textos, boolean primeraPaginaTieneImagen) {
        String todo() {
            return String.join("\n", textos);
        }
    }

    private static Leido leer(byte[] pdf) throws IOException {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            List<String> textos = new ArrayList<>();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                // Se normalizan los espacios: una leyenda larga puede partirse en dos renglones.
                textos.add(PdfTextExtractor.getTextFromPage(doc.getPage(i)).replaceAll("\\s+", " "));
            }
            PdfDictionary xobjects = doc.getPage(1).getResources().getResource(PdfName.XObject);
            return new Leido(doc.getNumberOfPages(), textos, xobjects != null && !xobjects.isEmpty());
        }
    }

    @Test
    void generaOriginalDuplicadoYTriplicadoUnaCopiaPorPagina() throws IOException {
        Leido pdf = leer(generador.generar(facturaA()));

        assertEquals(3, pdf.paginas());
        assertTrue(pdf.textos().get(0).contains("ORIGINAL"));
        assertTrue(pdf.textos().get(1).contains("DUPLICADO"));
        assertTrue(pdf.textos().get(2).contains("TRIPLICADO"));
        for (String pagina : pdf.textos()) {
            assertTrue(pagina.contains("Pág. 1/1"), pagina);
        }
    }

    @Test
    void laCabeceraTieneLosDatosObligatoriosDelEmisorYDelComprobante() throws IOException {
        String texto = leer(generador.generar(facturaA())).textos().get(0);

        assertTrue(texto.contains("FACTURA"), texto);
        assertTrue(texto.contains("COD. 01"), texto);
        assertTrue(texto.contains("Punto de Venta: 00002"), texto);
        assertTrue(texto.contains("Comp. Nro: 00000008"), texto);
        assertTrue(texto.contains("Fecha de Emisión: 18/11/2026"), texto);
        assertTrue(texto.contains("Razón Social: Comercial Ejemplo SRL"), texto);
        assertTrue(texto.contains("Domicilio Comercial: Av. Siempreviva 742 - CABA"), texto);
        assertTrue(texto.contains("CUIT: 20111111112"), texto);
        assertTrue(texto.contains("Ingresos Brutos: 901-123456-7"), texto);
        assertTrue(texto.contains("Fecha de Inicio de Actividades: 01/03/2020"), texto);
        assertTrue(texto.contains("Condición frente al IVA: IVA Responsable Inscripto"), texto);
    }

    @Test
    void muestraLosDatosDelReceptorYLaCondicionDeVenta() throws IOException {
        String texto = leer(generador.generar(facturaA())).textos().get(0);

        assertTrue(texto.contains("CUIT: 20281234567"), texto);
        assertTrue(texto.contains("Apellido y Nombre / Razón Social: Juan Perez"), texto);
        assertTrue(texto.contains("Domicilio: San Martín 123"), texto);
        assertTrue(texto.contains("Condición de venta: Contado"), texto);
    }

    @Test
    void facturaADiscriminaIvaPorAlicuotaYMuestraOtrosTributos() throws IOException {
        String texto = leer(generador.generar(facturaA())).textos().get(0);

        // Encabezados de columna (en dos líneas por el ancho de la columna).
        assertTrue(texto.contains("Alícuota"), texto);
        assertTrue(texto.contains("c/IVA"), texto);
        assertTrue(texto.contains("Importe Neto Gravado: $"), texto);
        assertTrue(texto.contains("IVA 27%: $"), texto);
        assertTrue(texto.contains("IVA 21%: $"), texto);
        assertTrue(texto.contains("37,80"), texto);
        assertTrue(texto.contains("IVA 10.5%: $"), texto);
        assertTrue(texto.contains("21,00"), texto);
        assertTrue(texto.contains("Otros Tributos"), texto);
        assertTrue(texto.contains("Importe Total: $"), texto);
        assertTrue(texto.contains("438,80"), texto);
    }

    @Test
    void elPieTieneCaeVencimientoYLeyendasDeComprobanteAutorizadoConElQr() throws IOException {
        Leido pdf = leer(generador.generar(facturaA()));
        String texto = pdf.textos().get(0);

        assertTrue(texto.contains("CAE N°: 86461089472534"), texto);
        assertTrue(texto.contains("Fecha de Vto. de CAE: 28/11/2026"), texto);
        assertTrue(texto.contains("Comprobante Autorizado"), texto);
        assertTrue(texto.contains("no se responsabiliza por los datos ingresados en el detalle de la operación"), texto);
        assertTrue(pdf.primeraPaginaTieneImagen(), "el QR se dibuja como imagen");
    }

    @Test
    void facturaBMuestraPreciosConIvaYElRegimenDeTransparenciaFiscal() throws IOException {
        String texto = leer(generador.generar(comprobante(TipoComprobante.B, "PES", "1", false,
                DocumentoReceptor.CONSUMIDOR_FINAL))).textos().get(0);

        assertTrue(texto.contains("COD. 06"), texto);
        assertFalse(texto.contains("Importe Neto Gravado"));
        assertFalse(texto.contains("c/IVA"));
        // 100 + 21 % = 121 por unidad
        assertTrue(texto.contains("121,00"), texto);
        assertTrue(texto.contains("Régimen de Transparencia Fiscal al Consumidor (Ley 27.743)"), texto);
        assertTrue(texto.contains("IVA Contenido: $"), texto);
        assertTrue(texto.contains("58,80"), texto);
        assertTrue(texto.contains("Apellido y Nombre / Razón Social: Consumidor Final"), texto);
        assertFalse(texto.contains("CUIT: 0"), "un Consumidor Final sin identificar no muestra documento");
    }

    @Test
    void facturaCNoDiscriminaIva() throws IOException {
        String texto = leer(generador.generar(comprobante(TipoComprobante.C, "PES", "1", false,
                new DocumentoReceptor(96, 30123456L)))).textos().get(0);

        assertTrue(texto.contains("COD. 11"), texto);
        assertTrue(texto.contains("DNI: 30123456"), texto);
        assertTrue(texto.contains("Condición frente al IVA: Responsable Monotributo"), texto);
        assertFalse(texto.contains("IVA 21%"));
        assertFalse(texto.contains("Ley 27.743"));
        assertTrue(texto.contains("Subtotal: $"), texto);
        assertTrue(texto.contains("Importe Total: $"), texto);
    }

    @Test
    void enMonedaExtranjeraInformaLaMonedaYElTotalEnPesos() throws IOException {
        String texto = leer(generador.generar(comprobante(TipoComprobante.A, "DOL", "1000", false,
                new DocumentoReceptor(80, 20281234567L)))).textos().get(0);

        assertTrue(texto.contains("Moneda: DOL - Dólar estadounidense"), texto);
        assertTrue(texto.contains("Importe Total: DOL"), texto);
        assertTrue(texto.contains("Pesos Argentinos"), texto);
        assertTrue(texto.contains("438.800,00"), texto);
    }

    @Test
    void unaFacturaAnuladaLlevaLaMarcaAnulada() throws IOException {
        Leido anulada = leer(generador.generar(comprobante(TipoComprobante.A, "PES", "1", true,
                new DocumentoReceptor(80, 20281234567L))));
        Leido vigente = leer(generador.generar(facturaA()));

        assertTrue(anulada.todo().contains("ANULADA"));
        assertFalse(vigente.todo().contains("ANULADA"));
    }

    @Test
    void muchosRenglonesOcupanVariasPaginasPorCopiaConLaNumeracion() throws IOException {
        ComprobantePdf base = facturaA();
        List<ComprobantePdf.Renglon> muchos = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            muchos.add(base.renglones().get(0));
        }
        ComprobantePdf largo = new ComprobantePdf(base.emisor(), base.tipo(), base.puntoVenta(), base.numero(),
                base.fechaEmision(), base.receptor(), base.monedaCodigo(), base.monedaDenominacion(),
                base.cotizacion(), muchos, base.importeNeto(), base.importeIva(), base.importeTotal(), base.cae(),
                base.caeVencimiento(), base.observaciones(), false, base.qrUrl());

        Leido pdf = leer(generador.generar(largo));

        assertEquals(0, pdf.paginas() % 3, "cada copia ocupa la misma cantidad de páginas");
        int porCopia = pdf.paginas() / 3;
        assertTrue(porCopia >= 2, "páginas por copia: " + porCopia);
        assertTrue(pdf.textos().get(0).contains("Pág. 1/" + porCopia));
        assertTrue(pdf.textos().get(porCopia - 1).contains("Pág. " + porCopia + "/" + porCopia));
        assertTrue(pdf.textos().get(porCopia).contains("DUPLICADO"));
        assertNotNull(pdf.textos().get(pdf.paginas() - 1));
    }
}
