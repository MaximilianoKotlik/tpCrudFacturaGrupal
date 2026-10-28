package com.utn.tp_spring_grupal.afip;

import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Arma el bloque {@code FeCAEReq} de FECAESolicitar. Clase pura: no toca la red ni el token.
 * Respeta el orden de elementos del esquema WSFE v1 (CondicionIVAReceptorId después de MonCotiz, Iva al final).
 */
final class FeCaeSolicitarXml {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int CONCEPTO_PRODUCTOS = 1;

    private FeCaeSolicitarXml() {
    }

    static String feCaeReq(SolicitudCae s) {
        validarImportes(s);

        StringBuilder x = new StringBuilder();
        x.append("<ar:FeCAEReq><ar:FeCabReq><ar:CantReg>1</ar:CantReg>")
                .append("<ar:PtoVta>").append(s.puntoVenta()).append("</ar:PtoVta>")
                .append("<ar:CbteTipo>").append(s.tipoComprobante()).append("</ar:CbteTipo>")
                .append("</ar:FeCabReq><ar:FeDetReq><ar:FECAEDetRequest>")
                .append("<ar:Concepto>").append(CONCEPTO_PRODUCTOS).append("</ar:Concepto>")
                .append("<ar:DocTipo>").append(s.documento().tipo()).append("</ar:DocTipo>")
                .append("<ar:DocNro>").append(s.documento().numero()).append("</ar:DocNro>")
                .append("<ar:CbteDesde>").append(s.numero()).append("</ar:CbteDesde>")
                .append("<ar:CbteHasta>").append(s.numero()).append("</ar:CbteHasta>")
                .append("<ar:CbteFch>").append(FECHA.format(s.fechaComprobante())).append("</ar:CbteFch>")
                .append("<ar:ImpTotal>").append(importe(s.importeTotal())).append("</ar:ImpTotal>")
                .append("<ar:ImpTotConc>0.00</ar:ImpTotConc>")
                .append("<ar:ImpNeto>").append(importe(s.importeNeto())).append("</ar:ImpNeto>")
                .append("<ar:ImpOpEx>0.00</ar:ImpOpEx>")
                .append("<ar:ImpTrib>0.00</ar:ImpTrib>")
                .append("<ar:ImpIVA>").append(importe(s.importeIva())).append("</ar:ImpIVA>")
                .append(xmlMoneda(s.monId(), s.monCotiz()))
                .append("<ar:CondicionIVAReceptorId>").append(s.condicionIvaReceptorId())
                .append("</ar:CondicionIVAReceptorId>");

        if (!s.desgloseIva().isEmpty()) {
            x.append("<ar:Iva>");
            for (ResumenAlicuota a : s.desgloseIva()) {
                x.append("<ar:AlicIva><ar:Id>").append(a.codigoAfip()).append("</ar:Id>")
                        .append("<ar:BaseImp>").append(importe(a.baseImponible())).append("</ar:BaseImp>")
                        .append("<ar:Importe>").append(importe(a.importe())).append("</ar:Importe></ar:AlicIva>");
            }
            x.append("</ar:Iva>");
        }
        return x.append("</ar:FECAEDetRequest></ar:FeDetReq></ar:FeCAEReq>").toString();
    }

    /** Fragmento XML de moneda y cotización de FECAEDetRequest. */
    static String xmlMoneda(String monId, BigDecimal monCotiz) {
        return "<ar:MonId>" + XmlUtil.escapar(monId) + "</ar:MonId><ar:MonCotiz>" + monCotiz.toPlainString()
                + "</ar:MonCotiz>";
    }

    /** ARCA rechaza el comprobante si los importes no cierran; mejor detectarlo antes de enviar. */
    private static void validarImportes(SolicitudCae s) {
        if (s.importeTotal().compareTo(s.importeNeto().add(s.importeIva())) != 0) {
            throw new IllegalArgumentException("El total (" + s.importeTotal() + ") debe ser neto ("
                    + s.importeNeto() + ") + IVA (" + s.importeIva() + ").");
        }
        if (s.desgloseIva().isEmpty()) {
            if (s.importeIva().signum() != 0) {
                throw new IllegalArgumentException("Hay IVA (" + s.importeIva() + ") pero no el desglose por alícuota.");
            }
            return;
        }
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        for (ResumenAlicuota a : s.desgloseIva()) {
            base = base.add(a.baseImponible());
            iva = iva.add(a.importe());
        }
        if (base.compareTo(s.importeNeto()) != 0) {
            throw new IllegalArgumentException("ImpNeto (" + s.importeNeto() + ") debe ser la suma de BaseImp (" + base + ").");
        }
        if (iva.compareTo(s.importeIva()) != 0) {
            throw new IllegalArgumentException("ImpIVA (" + s.importeIva() + ") debe ser la suma de los Importe (" + iva + ").");
        }
    }

    private static String importe(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
