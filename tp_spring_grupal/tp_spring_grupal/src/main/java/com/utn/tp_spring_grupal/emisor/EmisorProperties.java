package com.utn.tp_spring_grupal.emisor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Configuración del emisor: afip.condicion-iva-emisor (RESPONSABLE_INSCRIPTO por defecto). */
@Component
@ConfigurationProperties(prefix = "afip")
public class EmisorProperties {

    private CondicionIvaEmisor condicionIvaEmisor = CondicionIvaEmisor.RESPONSABLE_INSCRIPTO;

    public CondicionIvaEmisor getCondicionIvaEmisor() {
        return condicionIvaEmisor;
    }

    public void setCondicionIvaEmisor(CondicionIvaEmisor condicionIvaEmisor) {
        this.condicionIvaEmisor = condicionIvaEmisor;
    }

    /** afip.punto-venta: punto de venta de Web Services dado de alta en ARCA. */
    private int puntoVenta = 2;

    /** afip.consumidor-final-monto-maximo: tope por comprobante a un Consumidor Final sin identificar (RG 5700/2025). */
    private BigDecimal consumidorFinalMontoMaximo = new BigDecimal("10000000");

    public int getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(int puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public BigDecimal getConsumidorFinalMontoMaximo() {
        return consumidorFinalMontoMaximo;
    }

    public void setConsumidorFinalMontoMaximo(BigDecimal consumidorFinalMontoMaximo) {
        this.consumidorFinalMontoMaximo = consumidorFinalMontoMaximo;
    }

    public boolean discriminaIva() {
        return condicionIvaEmisor.discriminaIva();
    }
}
