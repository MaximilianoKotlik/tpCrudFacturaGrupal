package com.utn.tp_spring_grupal.articulo;

import com.utn.tp_spring_grupal.common.PageResponse;
import com.utn.tp_spring_grupal.marca.Marca;
import com.utn.tp_spring_grupal.rubro.Rubro;
import com.utn.tp_spring_grupal.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@Import(ArticuloService.class)
class ArticuloServiceTest {

    @Autowired TestEntityManager em;
    @Autowired ArticuloService service;
    Rubro elec, ofi;
    Marca logi, sam;

    @BeforeEach
    void datos() {
        TestData d = new TestData(em);
        elec = d.rubro("Electronica");
        ofi = d.rubro("Oficina");
        logi = d.marca("Logitech");
        sam = d.marca("Samsung");
        d.articulo("A-003", "Monitor 24", elec, sam);
        d.articulo("A-001", "Mouse inalambrico", elec, logi);
        d.articulo("A-002", "Resma A4", ofi, null);
        d.articulo("A-004", "Sin rubro", null, null, d.alicuota(4, "10.5"));
        em.flush();
    }

    private List<String> codigos(PageResponse<ArticuloDTO> l) {
        return l.contenido().stream().map(ArticuloDTO::codigo).toList();
    }

    @Test
    void sinFiltrosOrdenaPorCodigo() {
        assertEquals(List.of("A-001", "A-002", "A-003", "A-004"), codigos(service.buscar(null, null, null, null, null, 0, 10)));
    }

    @Test
    void busquedaPorCodigoODenominacionContains() {
        assertEquals(List.of("A-001"), codigos(service.buscar("MOUSE", null, null, null, null, 0, 10)));
        assertEquals(List.of("A-002"), codigos(service.buscar("a-002", null, null, null, null, 0, 10)));
    }

    @Test
    void filtroPorRubro() {
        assertEquals(List.of("A-001", "A-003"), codigos(service.buscar(null, elec.getId(), null, null, null, 0, 10)));
    }

    @Test
    void filtroPorMarcaYRubroCombinados() {
        assertEquals(List.of("A-003"), codigos(service.buscar(null, elec.getId(), sam.getId(), null, null, 0, 10)));
    }

    @Test
    void ordenPorDenominacionDescendente() {
        assertEquals(List.of("A-004", "A-002", "A-001", "A-003"),
                codigos(service.buscar(null, null, null, "denominacion", "desc", 0, 10)));
    }

    @Test
    void rubroYMarcaSeExponenComoNombreONull() {
        ArticuloDTO conTodo = service.buscar("A-001", null, null, null, null, 0, 10).contenido().get(0);
        assertEquals("Electronica", conTodo.rubro());
        assertEquals("Logitech", conTodo.marca());
        ArticuloDTO sinNada = service.buscar("A-004", null, null, null, null, 0, 10).contenido().get(0);
        assertNull(sinNada.rubro());
        assertNull(sinNada.marca());
    }

    @Test
    void exponeLaAlicuotaDelArticulo() {
        assertEquals(0, new java.math.BigDecimal("21").compareTo(
                service.buscar("A-001", null, null, null, null, 0, 10).contenido().get(0).alicuotaPorcentaje()));
        assertEquals(0, new java.math.BigDecimal("10.5").compareTo(
                service.buscar("A-004", null, null, null, null, 0, 10).contenido().get(0).alicuotaPorcentaje()));
    }

    @Test
    void paginaYCalculaTotales() {
        PageResponse<ArticuloDTO> p0 = service.buscar(null, null, null, null, null, 0, 3);
        assertEquals(List.of("A-001", "A-002", "A-003"), codigos(p0));
        assertEquals(4, p0.totalElementos());
        assertEquals(2, p0.totalPaginas());
        assertEquals(List.of("A-004"), codigos(service.buscar(null, null, null, null, null, 1, 3)));
    }
}
