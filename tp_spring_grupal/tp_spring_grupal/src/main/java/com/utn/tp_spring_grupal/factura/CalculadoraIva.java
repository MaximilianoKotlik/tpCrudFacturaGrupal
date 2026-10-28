package com.utn.tp_spring_grupal.factura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cálculo de neto / IVA / subtotal. Clase pura (sin Spring ni JPA): todos los importes son BigDecimal
 * con escala 2 y redondeo HALF_UP aplicado por renglón, como exige AFIP (ImpNeto = suma de BaseImp,
 * ImpIVA = suma de Importe por alícuota, coherentes al centavo).
 */
public final class CalculadoraIva {

    /** Código AFIP de la alícuota 0 % (FEParamGetTiposIva). */
    public static final int CODIGO_AFIP_ALICUOTA_CERO = 3;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private CalculadoraIva() {
    }

    public record ImportesRenglon(BigDecimal neto, BigDecimal iva, BigDecimal subtotal) {
    }

    /** Alícuota que se aplica efectivamente (y se guarda como snapshot en el renglón). */
    public record Alicuota(int codigoAfip, BigDecimal porcentaje) {
    }

    /** Neto e IVA de un renglón junto a la alícuota con la que se calculó. */
    public record LineaIva(int codigoAfip, BigDecimal porcentaje, BigDecimal neto, BigDecimal iva) {
    }

    /** Una fila del desglose por alícuota (AlicIva de AFIP: Id, BaseImp, Importe). */
    public record ResumenAlicuota(int codigoAfip, BigDecimal porcentaje, BigDecimal baseImponible, BigDecimal importe) {
    }

    public record Totales(BigDecimal neto, BigDecimal iva, BigDecimal total) {
    }

    /**
     * neto = precio * cantidad * (1 - bonificación/100); iva = neto * alícuota/100; subtotal = neto + iva.
     * Cada importe se redondea a 2 decimales HALF_UP antes de usarse en el siguiente.
     */
    public static ImportesRenglon calcularRenglon(BigDecimal precioUnitario, int cantidad,
                                                  BigDecimal porcentajeBonificacion, BigDecimal alicuotaPorcentaje) {
        BigDecimal neto = precioUnitario
                .multiply(BigDecimal.valueOf(cantidad))
                .multiply(CIEN.subtract(porcentajeBonificacion))
                .divide(CIEN, ESCALA, REDONDEO);
        BigDecimal iva = neto.multiply(alicuotaPorcentaje).divide(CIEN, ESCALA, REDONDEO);
        return new ImportesRenglon(neto, iva, neto.add(iva));
    }

    /**
     * Si el emisor no discrimina IVA (Monotributo / Exento → comprobante C) el IVA es 0 y la alícuota
     * efectiva es 0 %; en otro caso se usa la del artículo.
     */
    public static Alicuota alicuotaAplicable(boolean emisorDiscriminaIva, int codigoAfipArticulo,
                                             BigDecimal porcentajeArticulo) {
        if (emisorDiscriminaIva) {
            return new Alicuota(codigoAfipArticulo, porcentajeArticulo);
        }
        return new Alicuota(CODIGO_AFIP_ALICUOTA_CERO, BigDecimal.ZERO);
    }

    public static Totales totalizar(Collection<ImportesRenglon> renglones) {
        BigDecimal neto = cero();
        BigDecimal iva = cero();
        BigDecimal total = cero();
        for (ImportesRenglon r : renglones) {
            neto = neto.add(r.neto());
            iva = iva.add(r.iva());
            total = total.add(r.subtotal());
        }
        return new Totales(neto, iva, total);
    }

    /** Agrupa por alícuota (porcentaje) sumando base e IVA; ordenado por porcentaje ascendente. */
    public static List<ResumenAlicuota> resumirPorAlicuota(Collection<LineaIva> lineas) {
        // TreeMap con compareTo: 21 y 21.00 son la misma alícuota.
        Map<BigDecimal, ResumenAlicuota> porAlicuota = new TreeMap<>();
        for (LineaIva l : lineas) {
            ResumenAlicuota previo = porAlicuota.get(l.porcentaje());
            if (previo == null) {
                porAlicuota.put(l.porcentaje(), new ResumenAlicuota(l.codigoAfip(), l.porcentaje(),
                        l.neto().setScale(ESCALA, REDONDEO), l.iva().setScale(ESCALA, REDONDEO)));
            } else {
                porAlicuota.put(l.porcentaje(), new ResumenAlicuota(previo.codigoAfip(), previo.porcentaje(),
                        previo.baseImponible().add(l.neto()), previo.importe().add(l.iva())));
            }
        }
        return List.copyOf(porAlicuota.values());
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA);
    }
}
