package com.utn.tp_spring_grupal.afip;

import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos de un comprobante para FECAESolicitar. {@code desgloseIva} va vacío para comprobantes C (el emisor no
 * discrimina IVA); para A/B trae una fila por alícuota.
 */
public record SolicitudCae(int puntoVenta, int tipoComprobante, long numero, LocalDate fechaComprobante,
                           BigDecimal importeTotal, BigDecimal importeNeto, BigDecimal importeIva,
                           DocumentoReceptor documento, String monId, BigDecimal monCotiz,
                           int condicionIvaReceptorId, List<ResumenAlicuota> desgloseIva) {
}
