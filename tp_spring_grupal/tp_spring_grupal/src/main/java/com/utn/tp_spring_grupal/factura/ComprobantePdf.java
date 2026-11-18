package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.afip.DocumentoReceptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Todo lo que se imprime en el PDF de un comprobante autorizado por ARCA. Los importes están en la moneda del
 * comprobante; los de cada renglón son netos (sin IVA) salvo {@code importeSubtotal}.
 *
 * @param qrUrl texto del código QR (ver {@link com.utn.tp_spring_grupal.afip.QrComprobante})
 */
public record ComprobantePdf(
        Emisor emisor,
        TipoComprobante tipo,
        int puntoVenta,
        long numero,
        LocalDate fechaEmision,
        Receptor receptor,
        String monedaCodigo,
        String monedaDenominacion,
        BigDecimal cotizacion,
        List<Renglon> renglones,
        BigDecimal importeNeto,
        BigDecimal importeIva,
        BigDecimal importeTotal,
        String cae,
        LocalDate caeVencimiento,
        String observaciones,
        boolean anulada,
        String qrUrl) {

    public record Emisor(String razonSocial, String domicilioComercial, String condicionIva, String cuit,
                         String ingresosBrutos, LocalDate inicioActividades) {
    }

    /** @param domicilio null si no se conoce (Consumidor Final sin identificar) */
    public record Receptor(DocumentoReceptor documento, String denominacion, String condicionIva, String domicilio) {
    }

    /** @param precioUnitario neto (sin IVA) */
    public record Renglon(String codigo, String descripcion, int cantidad, BigDecimal precioUnitario,
                          BigDecimal porcentajeBonificacion, BigDecimal alicuotaPorcentaje, BigDecimal importeNeto,
                          BigDecimal importeIva, BigDecimal importeSubtotal) {
    }

    public boolean enPesos() {
        return "PES".equals(monedaCodigo);
    }
}
