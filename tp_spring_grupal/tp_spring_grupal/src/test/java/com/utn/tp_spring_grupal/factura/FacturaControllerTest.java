package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.common.ApiExceptionHandler;
import com.utn.tp_spring_grupal.common.PageResponse;
import com.utn.tp_spring_grupal.common.ReglaNegocioException;
import com.utn.tp_spring_grupal.common.ValidacionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FacturaController.class)
@Import(ApiExceptionHandler.class)
class FacturaControllerTest {

    @Autowired MockMvc mvc;
    @MockBean FacturaService service;

    private FacturaDTO factura(EstadoFactura estado) {
        return new FacturaDTO(1L, null, estado, "A", LocalDateTime.of(2026, 3, 1, 10, 30), null,
                10L, "Juan", 100L, "Mayorista", 20L, "RI", 30L, "Centro", 40L, new BigDecimal("1174"),
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"),
                null, null, null, null, 1, null);
    }

    @Test
    void postCreaYDevuelve201ConEstadoComoStringNumeroNullYImportesNumericos() throws Exception {
        when(service.crearFactura(any())).thenReturn(factura(EstadoFactura.PENDIENTE));

        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.numeroFactura").doesNotExist())
                .andExpect(jsonPath("$.tipoComprobante").value("A"))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-03-01T10:30:00"))
                .andExpect(jsonPath("$.fechaEmision").doesNotExist())
                .andExpect(jsonPath("$.listaPrecioDenominacion").value("Mayorista"))
                .andExpect(jsonPath("$.cotizacion").value(1174))
                .andExpect(jsonPath("$.importeNeto").value(100.0))
                .andExpect(jsonPath("$.importeIva").value(21.0))
                .andExpect(jsonPath("$.importeTotal").value(121.0))
                .andExpect(jsonPath("$.importeCobrado").doesNotExist())
                .andExpect(jsonPath("$.importeSaldo").doesNotExist());
    }

    @Test
    void postConCantidadEnteraSeAceptaYSeDeserializaComoInteger() throws Exception {
        when(service.crearFactura(any())).thenReturn(factura(EstadoFactura.PENDIENTE));

        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"detalles\":[{\"articuloId\":1,\"cantidad\":2,\"porcentajeBonificacion\":7.5}]}"))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<FacturaGuardarDTO> cap = org.mockito.ArgumentCaptor.forClass(FacturaGuardarDTO.class);
        verify(service).crearFactura(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, cap.getValue().detalles().get(0).cantidad());
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("7.5").compareTo(cap.getValue().detalles().get(0).porcentajeBonificacion()));
    }

    @Test
    void postConCantidadDecimalDevuelve400SinLlamarAlService() throws Exception {
        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"detalles\":[{\"articuloId\":1,\"cantidad\":1.5}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());

        verifyNoInteractions(service);
    }

    @Test
    void putConCantidadDecimalDevuelve400() throws Exception {
        mvc.perform(put("/api/facturas/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detalles\":[{\"articuloId\":1,\"cantidad\":2.0}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void postConValidacionFallidaDevuelve400ConMensaje() throws Exception {
        when(service.crearFactura(any())).thenThrow(new ValidacionException("La factura debe tener al menos un renglón."));

        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La factura debe tener al menos un renglón."));
    }

    @Test
    void putSobreFacturaNoEditableDevuelve409() throws Exception {
        when(service.actualizarFactura(eq(5L), any())).thenThrow(new ReglaNegocioException("La factura está EMITIDA y no se puede editar."));

        mvc.perform(put("/api/facturas/5").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La factura está EMITIDA y no se puede editar."));
    }

    @Test
    void emitirDevuelve200ConLaFacturaEmitida() throws Exception {
        when(service.emitirFactura(5L)).thenReturn(factura(EstadoFactura.EMITIDA));

        mvc.perform(post("/api/facturas/5/emitir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMITIDA"));
    }

    @Test
    void emitirNoEmitibleDevuelve409() throws Exception {
        when(service.emitirFactura(5L)).thenThrow(new ReglaNegocioException("no emitible"));

        mvc.perform(post("/api/facturas/5/emitir")).andExpect(status().isConflict());
    }

    @Test
    void deleteEliminaLaFacturaYDevuelve204() throws Exception {
        mvc.perform(delete("/api/facturas/5")).andExpect(status().isNoContent());
        verify(service).eliminarFactura(5L);
    }

    @Test
    void deleteDeUnaFacturaEmitidaDevuelve409ConMensaje() throws Exception {
        doThrow(new ReglaNegocioException("Una factura emitida no se elimina: se anula."))
                .when(service).eliminarFactura(5L);

        mvc.perform(delete("/api/facturas/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Una factura emitida no se elimina: se anula."));
    }

    @Test
    void anularDevuelveLaFacturaAnulada() throws Exception {
        when(service.anularFactura(5L)).thenReturn(factura(EstadoFactura.ANULADA));

        mvc.perform(post("/api/facturas/5/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    @Test
    void anularUnaNoEmitidaDevuelve409() throws Exception {
        when(service.anularFactura(5L)).thenThrow(new ReglaNegocioException("Solo se anulan facturas emitidas."));
        mvc.perform(post("/api/facturas/5/anular")).andExpect(status().isConflict());
    }

    @Test
    void postAConsumidorFinalSinClienteSeDeserializa() throws Exception {
        when(service.crearFactura(any())).thenReturn(factura(EstadoFactura.PENDIENTE));

        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consumidorFinal\":true,\"listaPrecioId\":1,\"detalles\":[{\"articuloId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<FacturaGuardarDTO> cap = org.mockito.ArgumentCaptor.forClass(FacturaGuardarDTO.class);
        verify(service).crearFactura(cap.capture());
        org.junit.jupiter.api.Assertions.assertTrue(cap.getValue().esConsumidorFinal());
        org.junit.jupiter.api.Assertions.assertNull(cap.getValue().clienteId());
    }

    @Test
    void filtroDeEstadoInvalidoDevuelve400() throws Exception {
        mvc.perform(get("/api/facturas").param("estado", "PAGADA")).andExpect(status().isBadRequest());
    }

    @Test
    void listadoDevuelveElContratoDePaginacion() throws Exception {
        when(service.buscarFacturas(any(), eq(0), eq(10)))
                .thenReturn(new PageResponse<>(List.of(factura(EstadoFactura.PENDIENTE)), 0, 10, 23, 3));

        mvc.perform(get("/api/facturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].detalles").doesNotExist())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.totalElementos").value(23))
                .andExpect(jsonPath("$.totalPaginas").value(3));
    }

    @Test
    void listadoPasaFiltrosYParametrosDePaginacionAlService() throws Exception {
        when(service.buscarFacturas(any(), eq(2), eq(5)))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0));

        mvc.perform(get("/api/facturas")
                        .param("estado", "ERROR_AFIP")
                        .param("fechaDesde", "2026-03-01")
                        .param("fechaHasta", "2026-03-31")
                        .param("montoMinimo", "150.50")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2));

        verify(service).buscarFacturas(eq(new FacturaFiltro(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                EstadoFactura.ERROR_AFIP, new BigDecimal("150.50"), null, null, null)), eq(2), eq(5));
    }

    @Test
    void listadoPasaLosFiltrosDeClienteTipoYCae() throws Exception {
        when(service.buscarFacturas(any(), eq(0), eq(10))).thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));

        mvc.perform(get("/api/facturas")
                        .param("clienteId", "12")
                        .param("tipoComprobante", "B")
                        .param("cae", "7512"))
                .andExpect(status().isOk());

        verify(service).buscarFacturas(eq(new FacturaFiltro(null, null, null, null, 12L, TipoComprobante.B, "7512")),
                eq(0), eq(10));
    }

    @Test
    void tipoDeComprobanteInvalidoDevuelve400() throws Exception {
        mvc.perform(get("/api/facturas").param("tipoComprobante", "Z")).andExpect(status().isBadRequest());
    }

    @Test
    void reportesAceptanLosMismosFiltrosSinPaginar() throws Exception {
        when(service.buscarFacturasSinPaginar(any())).thenReturn(List.of());
        when(service.generarReporteExcelTxt(any())).thenReturn("x".getBytes());

        mvc.perform(get("/api/facturas/generar-excel").param("estado", "PENDIENTE"))
                .andExpect(status().isOk());

        verify(service).buscarFacturasSinPaginar(eq(new FacturaFiltro(null, null, EstadoFactura.PENDIENTE, null,
                null, null, null)));
    }
}
