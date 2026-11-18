package com.utn.tp_spring_grupal.emisor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Configuración del emisor: afip.condicion-iva-emisor (RESPONSABLE_INSCRIPTO por defecto) y los datos que se
 * imprimen en la cabecera del comprobante (afip.cuit, afip.razon-social, afip.domicilio-comercial,
 * afip.ingresos-brutos, afip.inicio-actividades).
 */
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

    /** afip.cuit: CUIT del emisor (el mismo del certificado). */
    private String cuit = "";
    private String razonSocial = "Razón social no configurada";
    private String domicilioComercial = "Domicilio no configurado";
    /** afip.ingresos-brutos: número de inscripción en Ingresos Brutos (o "Exento"). */
    private String ingresosBrutos = "-";
    /** afip.inicio-actividades: fecha de inicio de actividades (yyyy-MM-dd). */
    private LocalDate inicioActividades = LocalDate.of(2020, 1, 1);

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getDomicilioComercial() {
        return domicilioComercial;
    }

    public void setDomicilioComercial(String domicilioComercial) {
        this.domicilioComercial = domicilioComercial;
    }

    public String getIngresosBrutos() {
        return ingresosBrutos;
    }

    public void setIngresosBrutos(String ingresosBrutos) {
        this.ingresosBrutos = ingresosBrutos;
    }

    public LocalDate getInicioActividades() {
        return inicioActividades;
    }

    public void setInicioActividades(LocalDate inicioActividades) {
        this.inicioActividades = inicioActividades;
    }

    public boolean discriminaIva() {
        return condicionIvaEmisor.discriminaIva();
    }
}
