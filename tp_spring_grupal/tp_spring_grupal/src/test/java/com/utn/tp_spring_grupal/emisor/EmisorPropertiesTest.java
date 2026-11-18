package com.utn.tp_spring_grupal.emisor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los datos del emisor que se imprimen en el comprobante se leen de application.properties. */
class EmisorPropertiesTest {

    @Test
    void leeLosDatosDelEmisorDesdeLaConfiguracion() {
        Map<String, String> props = Map.of(
                "afip.cuit", "20-11111111-2",
                "afip.razon-social", "Comercial Ejemplo SRL",
                "afip.domicilio-comercial", "Av. Siempreviva 742 - CABA",
                "afip.ingresos-brutos", "901-123456-7",
                "afip.inicio-actividades", "2019-05-01",
                "afip.condicion-iva-emisor", "MONOTRIBUTO");

        EmisorProperties e = new Binder(java.util.List.of(new MapConfigurationPropertySource(props)), null,
                ApplicationConversionService.getSharedInstance()).bind("afip", EmisorProperties.class).get();

        assertEquals("20-11111111-2", e.getCuit());
        assertEquals("Comercial Ejemplo SRL", e.getRazonSocial());
        assertEquals("Av. Siempreviva 742 - CABA", e.getDomicilioComercial());
        assertEquals("901-123456-7", e.getIngresosBrutos());
        assertEquals(LocalDate.of(2019, 5, 1), e.getInicioActividades());
        assertEquals("Responsable Monotributo", e.getCondicionIvaEmisor().denominacion());
    }
}
