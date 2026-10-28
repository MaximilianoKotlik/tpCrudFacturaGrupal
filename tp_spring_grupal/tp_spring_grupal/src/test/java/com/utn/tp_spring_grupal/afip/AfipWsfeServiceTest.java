package com.utn.tp_spring_grupal.afip;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfipWsfeServiceTest {

    private static final String RESP_DOL = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><FEParamGetCotizacionResponse>"
            + "<FEParamGetCotizacionResult><ResultGet><MonId>DOL</MonId><MonCotiz>1174</MonCotiz>"
            + "<FchCotiz>20261027</FchCotiz></ResultGet></FEParamGetCotizacionResult>"
            + "</FEParamGetCotizacionResponse></soap:Body></soap:Envelope>";
    private static final String RESP_EUR = "<ResultGet><MonId>060</MonId><MonCotiz>1202.3588</MonCotiz>"
            + "<FchCotiz>20261027</FchCotiz></ResultGet>";
    private static final String RESP_ERROR = "<FEParamGetCotizacionResult><Errors><Err><Code>602</Code>"
            + "<Msg>Sin Resultados: - Metodo FEParamGetCotizacion</Msg></Err></Errors></FEParamGetCotizacionResult>";

    @Test
    void parsearCotizacionLeeUnEnteroComoDecimal() {
        assertEquals(0, new BigDecimal("1174").compareTo(AfipWsfeService.parsearCotizacion(RESP_DOL)));
    }

    @Test
    void parsearCotizacionConservaLosDecimalesDeArca() {
        assertEquals(new BigDecimal("1202.3588"), AfipWsfeService.parsearCotizacion(RESP_EUR));
    }

    @Test
    void parsearCotizacionConErrorDeArcaLanzaConCodigoYMensaje() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> AfipWsfeService.parsearCotizacion(RESP_ERROR));
        assertTrue(e.getMessage().contains("602"));
        assertTrue(e.getMessage().contains("Sin Resultados"));
    }

    @Test
    void parsearCotizacionSinMonCotizLanza() {
        assertThrows(RuntimeException.class, () -> AfipWsfeService.parsearCotizacion("<x/>"));
    }

    @Test
    void obtenerCotizacionDePesosEsUnoSinLlamarAArca() throws Exception {
        // authService null: si intentara llamar a ARCA fallaria con NPE
        assertEquals(BigDecimal.ONE, new AfipWsfeService(null).obtenerCotizacion("PES"));
    }

    @Test
    void xmlMonedaEnviaLaMonedaYLaCotizacionReales() {
        assertEquals("<ar:MonId>DOL</ar:MonId><ar:MonCotiz>1174</ar:MonCotiz>",
                AfipWsfeService.xmlMoneda("DOL", new BigDecimal("1174")));
        assertEquals("<ar:MonId>060</ar:MonId><ar:MonCotiz>1202.3588</ar:MonCotiz>",
                AfipWsfeService.xmlMoneda("060", new BigDecimal("1202.3588")));
        assertEquals("<ar:MonId>PES</ar:MonId><ar:MonCotiz>1</ar:MonCotiz>",
                AfipWsfeService.xmlMoneda("PES", BigDecimal.ONE));
    }
}
