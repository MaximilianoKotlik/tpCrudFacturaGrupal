package com.utn.tp_spring_grupal.afip;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

/**
 * Texto del código QR de un comprobante electrónico, según "Especificaciones del QR incluido en las facturas
 * electrónicas" de ARCA (RG 4291, versión 1 del JSON): {@code https://www.arca.gob.ar/fe/qr/?p={JSON en Base64}}.
 * Clase pura.
 */
public final class QrComprobante {

    public static final String URL = "https://www.arca.gob.ar/fe/qr/";

    /** Código del tipo de autorización: "E" = comprobante autorizado por CAE. */
    private static final String AUTORIZADO_POR_CAE = "E";

    private QrComprobante() {
    }

    /**
     * @param importe    importe total en la moneda del comprobante
     * @param moneda     código ARCA de la moneda (PES, DOL...)
     * @param cotizacion cotización en pesos (1 para pesos)
     * @param receptor   documento del receptor; Consumidor Final sin identificar (99) no se informa
     * @param cae        código de autorización (14 dígitos)
     */
    public record Datos(LocalDate fecha, long cuitEmisor, int puntoVenta, int tipoComprobante, long numero,
                        BigDecimal importe, String moneda, BigDecimal cotizacion, DocumentoReceptor receptor,
                        String cae) {
    }

    public static String url(Datos d) {
        StringBuilder json = new StringBuilder()
                .append("{\"ver\":1")
                .append(",\"fecha\":\"").append(d.fecha()).append('"')
                .append(",\"cuit\":").append(d.cuitEmisor())
                .append(",\"ptoVta\":").append(d.puntoVenta())
                .append(",\"tipoCmp\":").append(d.tipoComprobante())
                .append(",\"nroCmp\":").append(d.numero())
                .append(",\"importe\":").append(numero(d.importe()))
                .append(",\"moneda\":\"").append(d.moneda()).append('"')
                .append(",\"ctz\":").append(numero(d.cotizacion()));
        DocumentoReceptor receptor = d.receptor();
        if (receptor != null && receptor.tipo() != DocumentoReceptor.TIPO_CONSUMIDOR_FINAL) {
            json.append(",\"tipoDocRec\":").append(receptor.tipo())
                    .append(",\"nroDocRec\":").append(receptor.numero());
        }
        json.append(",\"tipoCodAut\":\"").append(AUTORIZADO_POR_CAE).append('"')
                .append(",\"codAut\":").append(d.cae().trim())
                .append('}');
        return URL + "?p=" + Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Número JSON sin ceros decimales sobrantes: 12100.00 → 12100, 1050.250000 → 1050.25. */
    private static String numero(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return (limpio.scale() < 0 ? limpio.setScale(0) : limpio).toPlainString();
    }
}
