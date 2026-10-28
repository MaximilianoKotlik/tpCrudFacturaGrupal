package com.utn.tp_spring_grupal.listaprecio;

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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(ListaPrecioService.class)
class ListaPrecioServiceTest {

    @Autowired TestEntityManager em;
    @Autowired ListaPrecioService service;
    @Autowired ListaPrecioArticuloRepository lpaRepository;
    ListaPrecio mayorista, minorista;
    Rubro elec;
    Marca logi;
    Long idMouse, idMonitor;

    @BeforeEach
    void datos() {
        TestData d = new TestData(em);
        mayorista = d.lista("MAY", "Mayorista");
        minorista = d.lista("MIN", "Minorista");
        elec = d.rubro("Electronica");
        Rubro ofi = d.rubro("Oficina");
        logi = d.marca("Logitech");
        var mouse = d.articulo("A-001", "Mouse", elec, logi);
        var monitor = d.articulo("A-002", "Monitor", elec, null);
        var resma = d.articulo("A-003", "Resma", ofi, null);
        idMouse = mouse.getId();
        idMonitor = monitor.getId();
        d.precio(mayorista, mouse, 100);
        d.precio(mayorista, monitor, 900);
        d.precio(mayorista, resma, 50);
        d.precio(minorista, mouse, 150);   // la minorista no tiene monitor ni resma
        em.flush();
        em.clear();
    }

    private List<String> codigos(PageResponse<ListaPrecioArticuloDTO> l) {
        return l.contenido().stream().map(ListaPrecioArticuloDTO::codigo).toList();
    }

    @Test
    void listarDevuelveLasListasConElContrato() {
        List<ListaPrecioDTO> listas = service.listar();
        assertEquals(2, listas.size());
        assertTrue(listas.stream().anyMatch(l -> l.codigo().equals("MAY") && l.denominacion().equals("Mayorista")));
    }

    @Test
    void soloDevuelveArticulosDeLaListaElegidaConSuPrecio() {
        List<ListaPrecioArticuloDTO> r = service.buscarArticulos(minorista.getId(), null, null, null, null, null, 0, 10).contenido();
        assertEquals(1, r.size());
        assertEquals(idMouse, r.get(0).articuloId());
        assertEquals(0, new BigDecimal("150").compareTo(r.get(0).precioVenta()));
        assertEquals(0, new BigDecimal("21").compareTo(r.get(0).alicuotaPorcentaje()));
        assertEquals("Electronica", r.get(0).rubro());
        assertEquals("Logitech", r.get(0).marca());
    }

    @Test
    void filtraPorRubroYMarca() {
        assertEquals(List.of("A-001", "A-002"), codigos(service.buscarArticulos(mayorista.getId(), null, elec.getId(), null, null, null, 0, 10)));
        assertEquals(List.of("A-001"), codigos(service.buscarArticulos(mayorista.getId(), null, null, logi.getId(), null, null, 0, 10)));
    }

    @Test
    void busquedaTextualPorCodigoODenominacion() {
        assertEquals(List.of("A-003"), codigos(service.buscarArticulos(mayorista.getId(), "resm", null, null, null, null, 0, 10)));
    }

    @Test
    void ordenaPorPrecioAscYDesc() {
        assertEquals(List.of("A-003", "A-001", "A-002"), codigos(service.buscarArticulos(mayorista.getId(), null, null, null, "precio", "asc", 0, 10)));
        assertEquals(List.of("A-002", "A-001", "A-003"), codigos(service.buscarArticulos(mayorista.getId(), null, null, null, "precio", "desc", 0, 10)));
    }

    @Test
    void ordenaPorDenominacion() {
        assertEquals(List.of("A-002", "A-001", "A-003"), codigos(service.buscarArticulos(mayorista.getId(), null, null, null, "denominacion", "asc", 0, 10)));
    }

    @Test
    void marcaNullSeExponeComoNull() {
        ListaPrecioArticuloDTO monitor = service.buscarArticulos(mayorista.getId(), "monitor", null, null, null, null, 0, 10).contenido().get(0);
        assertNull(monitor.marca());
    }

    @Test
    void precioPorListaYArticulo() {
        var may = lpaRepository.findByListaPrecioIdAndArticuloId(mayorista.getId(), idMouse);
        var min = lpaRepository.findByListaPrecioIdAndArticuloId(minorista.getId(), idMouse);
        assertEquals(0, new BigDecimal("100").compareTo(may.orElseThrow().getPrecioVenta()));
        assertEquals(0, new BigDecimal("150").compareTo(min.orElseThrow().getPrecioVenta()));
        assertTrue(lpaRepository.findByListaPrecioIdAndArticuloId(minorista.getId(), idMonitor).isEmpty());
    }

    @Test
    void paginaLosArticulosDeLaListaConTotales() {
        PageResponse<ListaPrecioArticuloDTO> p0 = service.buscarArticulos(mayorista.getId(), null, null, null, "codigo", "asc", 0, 2);
        assertEquals(List.of("A-001", "A-002"), codigos(p0));
        assertEquals(3, p0.totalElementos());
        assertEquals(2, p0.totalPaginas());
        assertEquals(List.of("A-003"), codigos(service.buscarArticulos(mayorista.getId(), null, null, null, "codigo", "asc", 1, 2)));
    }

    @Test
    void ordenarPorPrecioPaginaSobreTodoElConjunto() {
        // el orden se aplica antes de paginar: la primera pagina de "precio desc" trae el mas caro de la lista
        assertEquals(List.of("A-002"), codigos(service.buscarArticulos(mayorista.getId(), null, null, null, "precio", "desc", 0, 1)));
        assertEquals(List.of("A-001"), codigos(service.buscarArticulos(mayorista.getId(), null, null, null, "precio", "desc", 1, 1)));
    }
}
