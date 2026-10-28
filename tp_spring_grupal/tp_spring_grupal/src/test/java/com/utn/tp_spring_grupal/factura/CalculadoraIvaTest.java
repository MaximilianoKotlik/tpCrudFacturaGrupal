package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.factura.CalculadoraIva.ImportesRenglon;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.LineaIva;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.Totales;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraIvaTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static void assertImporte(String esperado, BigDecimal real) {
        assertEquals(2, real.scale(), "los importes tienen escala 2");
        assertEquals(0, bd(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    // ---------- renglón ----------

    @Test
    void renglonConBonificacionYAlicuota21() {
        ImportesRenglon r = CalculadoraIva.calcularRenglon(bd("100"), 2, bd("10"), bd("21"));
        assertImporte("180.00", r.neto());
        assertImporte("37.80", r.iva());
        assertImporte("217.80", r.subtotal());
    }

    @Test
    void renglonSinBonificacionConAlicuotaReducida() {
        ImportesRenglon r = CalculadoraIva.calcularRenglon(bd("200.00"), 3, BigDecimal.ZERO, bd("10.5"));
        assertImporte("600.00", r.neto());
        assertImporte("63.00", r.iva());
        assertImporte("663.00", r.subtotal());
    }

    @Test
    void alicuotaCeroNoGeneraIva() {
        ImportesRenglon r = CalculadoraIva.calcularRenglon(bd("99.99"), 1, BigDecimal.ZERO, BigDecimal.ZERO);
        assertImporte("99.99", r.neto());
        assertImporte("0.00", r.iva());
        assertImporte("99.99", r.subtotal());
    }

    @Test
    void redondeaHalfUpElIvaPorRenglon() {
        // 0,50 * 21 % = 0,105 -> 0,11 (HALF_UP); un redondeo bancario daría 0,10
        ImportesRenglon a = CalculadoraIva.calcularRenglon(bd("0.50"), 1, BigDecimal.ZERO, bd("21"));
        assertImporte("0.11", a.iva());
        // 10,05 * 10,5 % = 1,05525 -> 1,06
        ImportesRenglon b = CalculadoraIva.calcularRenglon(bd("10.05"), 1, BigDecimal.ZERO, bd("10.5"));
        assertImporte("1.06", b.iva());
        // 0,05 * 3 * 21 % = 0,0315 -> 0,03
        ImportesRenglon c = CalculadoraIva.calcularRenglon(bd("0.05"), 3, BigDecimal.ZERO, bd("21"));
        assertImporte("0.15", c.neto());
        assertImporte("0.03", c.iva());
    }

    @Test
    void redondeaHalfUpElNetoConBonificacionDecimal() {
        // 33,33 * 3 = 99,99; con 7,5 % de bonificación = 92,49075 -> 92,49
        ImportesRenglon r = CalculadoraIva.calcularRenglon(bd("33.33"), 3, bd("7.5"), bd("21"));
        assertImporte("92.49", r.neto());
        assertImporte("19.42", r.iva()); // 92,49 * 0,21 = 19,4229
        assertImporte("111.91", r.subtotal());
    }

    @Test
    void netoMasIvaSiempreEsElSubtotal() {
        ImportesRenglon r = CalculadoraIva.calcularRenglon(bd("7.77"), 7, bd("3.3"), bd("27"));
        assertEquals(0, r.neto().add(r.iva()).compareTo(r.subtotal()));
    }

    // ---------- alícuota aplicable (emisor que no discrimina) ----------

    @Test
    void siElEmisorNoDiscriminaIvaLaAlicuotaAplicableEsCero() {
        var discrimina = CalculadoraIva.alicuotaAplicable(true, 5, bd("21"));
        assertEquals(5, discrimina.codigoAfip());
        assertEquals(0, bd("21").compareTo(discrimina.porcentaje()));

        var noDiscrimina = CalculadoraIva.alicuotaAplicable(false, 5, bd("21"));
        assertEquals(3, noDiscrimina.codigoAfip());
        assertEquals(0, BigDecimal.ZERO.compareTo(noDiscrimina.porcentaje()));
    }

    // ---------- totales y resumen ----------

    @Test
    void totalizaLosRenglones() {
        Totales t = CalculadoraIva.totalizar(List.of(
                new ImportesRenglon(bd("100.00"), bd("21.00"), bd("121.00")),
                new ImportesRenglon(bd("50.50"), bd("5.30"), bd("55.80"))));
        assertImporte("150.50", t.neto());
        assertImporte("26.30", t.iva());
        assertImporte("176.80", t.total());
    }

    @Test
    void totalizarListaVaciaDaCeros() {
        Totales t = CalculadoraIva.totalizar(List.of());
        assertImporte("0.00", t.neto());
        assertImporte("0.00", t.iva());
        assertImporte("0.00", t.total());
    }

    @Test
    void resumenAgrupaPorAlicuotaYOrdenaAscendente() {
        List<ResumenAlicuota> r = CalculadoraIva.resumirPorAlicuota(List.of(
                new LineaIva(5, bd("21"), bd("100.00"), bd("21.00")),
                new LineaIva(4, bd("10.5"), bd("200.00"), bd("21.00")),
                new LineaIva(5, bd("21.00"), bd("50.00"), bd("10.50")),
                new LineaIva(3, bd("0"), bd("30.00"), bd("0.00"))));

        assertEquals(3, r.size());
        assertEquals(3, r.get(0).codigoAfip());
        assertImporte("30.00", r.get(0).baseImponible());
        assertEquals(4, r.get(1).codigoAfip());
        assertImporte("200.00", r.get(1).baseImponible());
        assertImporte("21.00", r.get(1).importe());
        assertEquals(5, r.get(2).codigoAfip());
        assertImporte("150.00", r.get(2).baseImponible());
        assertImporte("31.50", r.get(2).importe());
    }

    @Test
    void resumenDeListaVaciaEsVacio() {
        assertTrue(CalculadoraIva.resumirPorAlicuota(List.of()).isEmpty());
    }

    @Test
    void sumaDelResumenCoincideConLosTotales() {
        List<LineaIva> lineas = List.of(
                new LineaIva(5, bd("21"), bd("92.49"), bd("19.42")),
                new LineaIva(4, bd("10.5"), bd("10.05"), bd("1.06")),
                new LineaIva(5, bd("21"), bd("0.50"), bd("0.11")));
        List<ResumenAlicuota> resumen = CalculadoraIva.resumirPorAlicuota(lineas);
        BigDecimal base = resumen.stream().map(ResumenAlicuota::baseImponible).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva = resumen.stream().map(ResumenAlicuota::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertImporte("103.04", base);
        assertImporte("20.59", iva);
    }
}
