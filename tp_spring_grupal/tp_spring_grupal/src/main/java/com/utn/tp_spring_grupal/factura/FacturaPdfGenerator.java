package com.utn.tp_spring_grupal.factura;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.barcodes.qrcode.EncodeHintType;
import com.itextpdf.barcodes.qrcode.ErrorCorrectionLevel;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDF de un comprobante autorizado, con el formato de las facturas electrónicas de ARCA (RG 1415 / RG 4291):
 * original, duplicado y triplicado; cabecera con la letra y el código del comprobante; datos del emisor y del
 * receptor; detalle; totales según la letra (A discrimina IVA por alícuota, B informa el IVA contenido de la
 * Ley 27.743, C no discrimina); y al pie el QR, el CAE y su vencimiento en cada página.
 */
@Component
public class FacturaPdfGenerator {

    private static final String[] COPIAS = { "ORIGINAL", "DUPLICADO", "TRIPLICADO" };
    /** Alícuotas que ARCA muestra siempre en una factura A, aunque sean 0. */
    private static final String[] ALICUOTAS_A = { "27", "21", "10.5", "5", "2.5", "0" };
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private static final float MARGEN = 28f;
    /** Alto reservado al pie (QR, CAE, leyendas): el contenido nunca lo invade. */
    private static final float ALTO_PIE = 120f;
    private static final float TAMANIO_QR = 86f;
    private static final Color GRIS = new DeviceRgb(0xE6, 0xE6, 0xE6);
    private static final Color GRIS_TEXTO = new DeviceRgb(0x55, 0x55, 0x55);
    private static final Color ROJO = new DeviceRgb(0xC6, 0x28, 0x28);
    private static final SolidBorder BORDE = new SolidBorder(ColorConstants.BLACK, 0.8f);

    private record RangoCopia(int desde, int hasta) {
    }

    public byte[] generar(ComprobantePdf c) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(salida));
            pdf.getDocumentInfo().setTitle("Factura " + c.tipo().getLetra() + " " + numeroCompleto(c));
            Document doc = new Document(pdf, PageSize.A4, false);
            doc.setMargins(MARGEN, MARGEN, MARGEN + ALTO_PIE, MARGEN);
            Fuentes f = new Fuentes(PdfFontFactory.createFont(StandardFonts.HELVETICA),
                    PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD));
            doc.setFont(f.normal()).setFontSize(8);

            List<RangoCopia> rangos = new ArrayList<>();
            for (int i = 0; i < COPIAS.length; i++) {
                if (i > 0) {
                    doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
                doc.add(copia(COPIAS[i], f));
                int desde = pdf.getNumberOfPages();
                doc.add(cabecera(c, f));
                doc.add(receptor(c, f));
                doc.add(detalle(c, f));
                doc.add(totales(c, f));
                rangos.add(new RangoCopia(desde, pdf.getNumberOfPages()));
            }

            PdfFormXObject qr = qr(c.qrUrl(), pdf);
            for (RangoCopia r : rangos) {
                int total = r.hasta() - r.desde() + 1;
                for (int pagina = r.desde(); pagina <= r.hasta(); pagina++) {
                    pie(doc, c, f, qr, pagina, pagina - r.desde() + 1, total);
                    if (c.anulada()) {
                        marcaAnulada(doc, f, pagina);
                    }
                }
            }
            doc.close();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF de la factura", e);
        }
    }

    private record Fuentes(PdfFont normal, PdfFont negrita) {
    }

    // ------------------------------------------------------------------ bloques

    private Table copia(String nombre, Fuentes f) {
        Table t = new Table(1).useAllAvailableWidth();
        t.addCell(new Cell().add(new Paragraph(nombre).setFont(f.negrita()).setFontSize(11))
                .setTextAlignment(TextAlignment.CENTER).setBorder(BORDE).setPadding(3));
        return t;
    }

    private Table cabecera(ComprobantePdf c, Fuentes f) {
        ComprobantePdf.Emisor e = c.emisor();
        Table t = new Table(UnitValue.createPercentArray(new float[] { 46, 8, 46 })).useAllAvailableWidth();

        Cell izquierda = new Cell().setBorder(BORDE).setBorderRight(Border.NO_BORDER).setPadding(8)
                .add(new Paragraph(e.razonSocial()).setFont(f.negrita()).setFontSize(15)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10))
                .add(dato("Razón Social: ", e.razonSocial(), f))
                .add(dato("Domicilio Comercial: ", e.domicilioComercial(), f))
                .add(dato("Condición frente al IVA: ", e.condicionIva(), f));

        Cell letra = new Cell().setBorder(BORDE).setPadding(2).setVerticalAlignment(VerticalAlignment.TOP)
                .add(new Paragraph(c.tipo().getLetra()).setFont(f.negrita()).setFontSize(28)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(-4))
                .add(new Paragraph(String.format("COD. %02d", c.tipo().getCodigoAfip())).setFont(f.negrita())
                        .setFontSize(7).setTextAlignment(TextAlignment.CENTER));

        Cell derecha = new Cell().setBorder(BORDE).setBorderLeft(Border.NO_BORDER).setPadding(8).setPaddingLeft(18)
                .add(new Paragraph("FACTURA").setFont(f.negrita()).setFontSize(17).setMarginBottom(6))
                .add(new Paragraph()
                        .add(new Text("Punto de Venta: ").setFont(f.negrita()))
                        .add(String.format("%05d", c.puntoVenta()))
                        .add(new Text("   Comp. Nro: ").setFont(f.negrita()))
                        .add(String.format("%08d", c.numero())).setFontSize(9))
                .add(dato("Fecha de Emisión: ", c.fechaEmision().format(FECHA), f).setFontSize(9).setMarginBottom(6))
                .add(dato("CUIT: ", e.cuit(), f))
                .add(dato("Ingresos Brutos: ", e.ingresosBrutos(), f))
                .add(dato("Fecha de Inicio de Actividades: ", e.inicioActividades().format(FECHA), f));

        return t.addCell(izquierda).addCell(letra).addCell(derecha);
    }

    private Table receptor(ComprobantePdf c, Fuentes f) {
        ComprobantePdf.Receptor r = c.receptor();
        Table t = new Table(UnitValue.createPercentArray(new float[] { 40, 60 })).useAllAvailableWidth()
                .setMarginTop(4);
        String documento = switch (r.documento().tipo()) {
            case 80 -> "CUIT: ";
            case 96 -> "DNI: ";
            default -> null;
        };
        Paragraph doc = documento != null
                ? dato(documento, String.valueOf(r.documento().numero()), f)
                : dato("Documento: ", "Sin identificar", f);
        Cell izquierda = new Cell().setBorder(BORDE).setBorderRight(Border.NO_BORDER).setPadding(6)
                .add(doc)
                .add(dato("Condición frente al IVA: ", r.condicionIva(), f))
                .add(dato("Condición de venta: ", "Contado", f));
        Cell derecha = new Cell().setBorder(BORDE).setBorderLeft(Border.NO_BORDER).setPadding(6)
                .add(dato("Apellido y Nombre / Razón Social: ", r.denominacion(), f))
                .add(dato("Domicilio: ", r.domicilio() != null ? r.domicilio() : "-", f));
        if (!c.enPesos()) {
            derecha.add(dato("Moneda: ", c.monedaCodigo() + " - " + c.monedaDenominacion(), f));
        }
        return t.addCell(izquierda).addCell(derecha);
    }

    private Table detalle(ComprobantePdf c, Fuentes f) {
        boolean esA = c.tipo() == TipoComprobante.A;
        String[] titulos = esA
                ? new String[] { "Código", "Producto / Servicio", "Cantidad", "U. medida", "Precio Unit.", "% Bonif",
                        "Subtotal", "Alícuota IVA", "Subtotal c/IVA" }
                : new String[] { "Código", "Producto / Servicio", "Cantidad", "U. medida", "Precio Unit.", "% Bonif",
                        "Imp. Bonif.", "Subtotal" };
        float[] anchos = esA
                ? new float[] { 9, 27, 8, 8, 11, 7, 11, 8, 11 }
                : new float[] { 9, 31, 8, 8, 12, 7, 11, 14 };
        Table t = new Table(UnitValue.createPercentArray(anchos)).useAllAvailableWidth().setMarginTop(8);
        for (int i = 0; i < titulos.length; i++) {
            t.addHeaderCell(new Cell().add(new Paragraph(titulos[i]).setFont(f.negrita()))
                    .setBackgroundColor(GRIS).setBorder(BORDE).setPadding(3)
                    .setTextAlignment(i < 2 ? TextAlignment.LEFT : TextAlignment.RIGHT));
        }
        for (ComprobantePdf.Renglon r : c.renglones()) {
            BigDecimal cantidad = BigDecimal.valueOf(r.cantidad());
            List<String> valores = new ArrayList<>(List.of(r.codigo(), r.descripcion(), importe(cantidad), "unidades"));
            if (esA) {
                valores.addAll(List.of(importe(r.precioUnitario()), importe(r.porcentajeBonificacion()),
                        importe(r.importeNeto()), porcentaje(r.alicuotaPorcentaje()) + "%",
                        importe(r.importeSubtotal())));
            } else {
                // B: los precios se muestran con el IVA incluido. C: el emisor no discrimina (alícuota 0).
                BigDecimal precio = r.precioUnitario()
                        .multiply(BigDecimal.ONE.add(r.alicuotaPorcentaje().divide(CIEN)))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal bonificado = precio.multiply(cantidad).multiply(r.porcentajeBonificacion())
                        .divide(CIEN, 2, RoundingMode.HALF_UP);
                valores.addAll(List.of(importe(precio), importe(r.porcentajeBonificacion()), importe(bonificado),
                        importe(r.importeSubtotal())));
            }
            for (int i = 0; i < valores.size(); i++) {
                t.addCell(new Cell().add(new Paragraph(valores.get(i)))
                        .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(GRIS, 0.5f)).setPadding(3)
                        .setTextAlignment(i < 2 ? TextAlignment.LEFT : TextAlignment.RIGHT));
            }
        }
        return t;
    }

    private Div totales(ComprobantePdf c, Fuentes f) {
        String moneda = c.enPesos() ? "$" : c.monedaCodigo();
        Table t = new Table(UnitValue.createPercentArray(new float[] { 55, 45 })).useAllAvailableWidth()
                .setMarginTop(10);

        Cell izquierda = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(10);
        Table importes = new Table(UnitValue.createPercentArray(new float[] { 65, 35 })).useAllAvailableWidth();
        switch (c.tipo()) {
            case A -> {
                izquierda.add(otrosTributos(moneda, f));
                Map<String, BigDecimal> ivaPorAlicuota = ivaPorAlicuota(c);
                filaTotal(importes, "Importe Neto Gravado: " + moneda, c.importeNeto(), f, false);
                for (String alicuota : ALICUOTAS_A) {
                    filaTotal(importes, "IVA " + alicuota + "%: " + moneda,
                            ivaPorAlicuota.getOrDefault(alicuota, BigDecimal.ZERO), f, false);
                }
            }
            case B -> {
                izquierda.add(transparenciaFiscal(c, f));
                filaTotal(importes, "Subtotal: " + moneda, c.importeTotal(), f, false);
            }
            case C -> filaTotal(importes, "Subtotal: " + moneda, c.importeTotal(), f, false);
        }
        filaTotal(importes, "Importe Otros Tributos: " + moneda, BigDecimal.ZERO, f, false);
        filaTotal(importes, "Importe Total: " + moneda, c.importeTotal(), f, true);
        Cell derecha = new Cell().setBorder(BORDE).setPadding(6).add(importes);

        Div bloque = new Div().setKeepTogether(true).add(t.addCell(izquierda).addCell(derecha));
        if (!c.enPesos()) {
            BigDecimal enPesos = c.importeTotal().multiply(c.cotizacion()).setScale(2, RoundingMode.HALF_UP);
            bloque.add(new Paragraph("El total de este comprobante expresado en moneda de curso legal - Pesos "
                    + "Argentinos - considerándose un tipo de cambio consignado de "
                    + c.cotizacion().setScale(6, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
                    + " asciende a: $ " + importe(enPesos)).setMarginTop(6));
        }
        if (c.observaciones() != null && !c.observaciones().isBlank()) {
            bloque.add(dato("Observaciones: ", c.observaciones(), f).setMarginTop(6));
        }
        return bloque;
    }

    private Table otrosTributos(String moneda, Fuentes f) {
        Table t = new Table(UnitValue.createPercentArray(new float[] { 52, 16, 12, 20 })).useAllAvailableWidth();
        t.addCell(new Cell(1, 4).add(new Paragraph("Otros Tributos").setFont(f.negrita())).setBorder(Border.NO_BORDER)
                .setPadding(2));
        for (String titulo : new String[] { "Descripción", "Detalle", "Alíc. %", "Importe" }) {
            t.addCell(new Cell().add(new Paragraph(titulo).setFont(f.negrita())).setBackgroundColor(GRIS)
                    .setBorder(Border.NO_BORDER).setPadding(2)
                    .setTextAlignment("Importe".equals(titulo) ? TextAlignment.RIGHT : TextAlignment.LEFT));
        }
        for (String tributo : new String[] { "Per./Ret. de Impuesto a las Ganancias", "Per./Ret. de IVA",
                "Per./Ret. Ingresos Brutos", "Impuestos Internos", "Impuestos Municipales" }) {
            t.addCell(celdaSimple(tributo, TextAlignment.LEFT));
            t.addCell(celdaSimple("", TextAlignment.LEFT));
            t.addCell(celdaSimple("", TextAlignment.LEFT));
            t.addCell(celdaSimple(importe(BigDecimal.ZERO), TextAlignment.RIGHT));
        }
        t.addCell(new Cell(1, 3).add(new Paragraph("Importe Otros Tributos: " + moneda).setFont(f.negrita()))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2));
        t.addCell(new Cell().add(new Paragraph(importe(BigDecimal.ZERO)).setFont(f.negrita()))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(2));
        return t;
    }

    private Table transparenciaFiscal(ComprobantePdf c, Fuentes f) {
        String moneda = c.enPesos() ? "$" : c.monedaCodigo();
        Table t = new Table(UnitValue.createPercentArray(new float[] { 65, 35 })).useAllAvailableWidth();
        t.addCell(new Cell(1, 2).add(new Paragraph("Régimen de Transparencia Fiscal al Consumidor (Ley 27.743)")
                .setFont(f.negrita())).setBorder(Border.NO_BORDER).setBorderBottom(BORDE).setPadding(3));
        t.addCell(celdaSimple("IVA Contenido: " + moneda, TextAlignment.LEFT));
        t.addCell(celdaSimple(importe(c.importeIva()), TextAlignment.RIGHT));
        t.addCell(celdaSimple("Otros Impuestos Nacionales Indirectos: " + moneda, TextAlignment.LEFT));
        t.addCell(celdaSimple(importe(BigDecimal.ZERO), TextAlignment.RIGHT));
        return t;
    }

    // --------------------------------------------------------------------- pie

    private void pie(Document doc, ComprobantePdf c, Fuentes f, PdfFormXObject qr, int pagina, int nro, int total) {
        float ancho = PageSize.A4.getWidth() - 2 * MARGEN;
        float base = MARGEN;

        doc.add(new Image(qr).scaleAbsolute(TAMANIO_QR, TAMANIO_QR).setFixedPosition(pagina, MARGEN - 6, base - 6));

        Div leyendas = new Div().setFixedPosition(pagina, MARGEN + TAMANIO_QR + 4, base, 330)
                .add(new Paragraph("Comprobante Autorizado").setFont(f.negrita()).setFontSize(11)
                        .setFontColor(GRIS_TEXTO).setMarginBottom(2))
                .add(new Paragraph("Esta Administración Federal no se responsabiliza por los datos ingresados en "
                        + "el detalle de la operación").setFontSize(6.5f).setFontColor(GRIS_TEXTO));
        doc.add(leyendas);

        doc.add(new Paragraph("Pág. " + nro + "/" + total).setFont(f.negrita())
                .setFixedPosition(pagina, MARGEN + ancho / 2 - 20, base + TAMANIO_QR - 24, 60));

        float anchoCae = 190;
        doc.add(new Div().setFixedPosition(pagina, MARGEN + ancho - anchoCae, base + TAMANIO_QR - 42, anchoCae)
                .add(new Paragraph().add(new Text("CAE N°: ").setFont(f.negrita())).add(c.cae())
                        .setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(2))
                .add(new Paragraph().add(new Text("Fecha de Vto. de CAE: ").setFont(f.negrita()))
                        .add(c.caeVencimiento().format(FECHA)).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
    }

    private void marcaAnulada(Document doc, Fuentes f, int pagina) {
        PageSize a4 = PageSize.A4;
        doc.showTextAligned(new Paragraph("ANULADA").setFont(f.negrita()).setFontSize(110).setFontColor(ROJO)
                        .setOpacity(0.18f),
                a4.getWidth() / 2, a4.getHeight() / 2, pagina, TextAlignment.CENTER, VerticalAlignment.MIDDLE,
                (float) Math.toRadians(45));
    }

    private PdfFormXObject qr(String texto, PdfDocument pdf) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        return new BarcodeQRCode(texto, hints).createFormXObject(ColorConstants.BLACK, pdf);
    }

    // ----------------------------------------------------------------- helpers

    private static Paragraph dato(String etiqueta, String valor, Fuentes f) {
        return new Paragraph().add(new Text(etiqueta).setFont(f.negrita())).add(valor != null ? valor : "-")
                .setMarginBottom(2);
    }

    private static Cell celdaSimple(String texto, TextAlignment alineacion) {
        return new Cell().add(new Paragraph(texto)).setBorder(Border.NO_BORDER).setPadding(2)
                .setTextAlignment(alineacion);
    }

    private static void filaTotal(Table t, String etiqueta, BigDecimal valor, Fuentes f, boolean destacado) {
        PdfFont fuente = f.negrita();
        float tamanio = destacado ? 10 : 8.5f;
        t.addCell(new Cell().add(new Paragraph(etiqueta).setFont(fuente).setFontSize(tamanio))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(1.5f));
        t.addCell(new Cell().add(new Paragraph(importe(valor)).setFont(fuente).setFontSize(tamanio))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(1.5f));
    }

    /** IVA agrupado por alícuota con la etiqueta de ARCA ("21", "10.5"...). */
    private static Map<String, BigDecimal> ivaPorAlicuota(ComprobantePdf c) {
        Map<String, BigDecimal> iva = new HashMap<>();
        for (ComprobantePdf.Renglon r : c.renglones()) {
            iva.merge(porcentaje(r.alicuotaPorcentaje()), r.importeIva(), BigDecimal::add);
        }
        return iva;
    }

    private static String porcentaje(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return (limpio.scale() < 0 ? limpio.setScale(0) : limpio).toPlainString();
    }

    private static String importe(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("es", "AR")));
        return formato.format(valor);
    }

    private static String numeroCompleto(ComprobantePdf c) {
        return String.format("%05d-%08d", c.puntoVenta(), c.numero());
    }
}
