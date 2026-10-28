package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecio;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticulo;
import com.utn.tp_spring_grupal.puntoventa.PuntoVenta;
import com.utn.tp_spring_grupal.support.TestData;
import com.utn.tp_spring_grupal.tipomoneda.TipoMoneda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Filtros por Specification (fecha de CREACIÓN, estado, monto), paginación y conteo de renglones. */
@DataJpaTest
class FacturaVentaRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired FacturaVentaRepository repo;

    private static final Sort ORDEN = Sort.by(Sort.Direction.DESC, "fechaAlta", "id");

    @BeforeEach
    void datos() {
        TestData d = new TestData(em);
        Cliente cliente = d.cliente("Juan Perez", "20-1-1");
        ListaPrecio lista = d.lista("MAY", "Mayorista");
        ListaPrecioArticulo lpa = d.precio(lista, d.articulo("A-1", "Mouse", null, null), 100);
        PuntoVenta pv = d.puntoVenta();
        TipoMoneda moneda = d.moneda();
        d.factura(EstadoFactura.PENDIENTE, LocalDate.of(2026, 1, 10), 100, null, cliente, lista, pv, moneda, lpa);
        d.factura(EstadoFactura.EMITIDA, LocalDate.of(2026, 2, 10), 500, null, cliente, lista, pv, moneda, lpa);
        d.factura(EstadoFactura.ERROR_AFIP, LocalDate.of(2026, 3, 10), 900, "CUIT invalido", cliente, lista, pv, moneda, lpa);
        d.factura(EstadoFactura.ANULADA, LocalDate.of(2026, 4, 10), 50, null, cliente, lista, pv, moneda, lpa);
        em.flush();
        em.clear();
    }

    private List<FacturaVenta> buscar(LocalDate desde, LocalDate hasta, EstadoFactura estado, String monto) {
        Specification<FacturaVenta> spec = FacturaSpecs.filtrar(desde, hasta, estado, monto != null ? new BigDecimal(monto) : null);
        return repo.findAll(spec, ORDEN);
    }

    @Test
    void sinFiltrosDevuelveTodasLasMasRecientesPrimero() {
        List<FacturaVenta> r = buscar(null, null, null, null);
        assertEquals(4, r.size());
        assertEquals(EstadoFactura.ANULADA, r.get(0).getEstado());
        assertEquals(EstadoFactura.PENDIENTE, r.get(3).getEstado());
    }

    @Test
    void filtraPorEstado() {
        List<FacturaVenta> r = buscar(null, null, EstadoFactura.ERROR_AFIP, null);
        assertEquals(1, r.size());
        assertEquals("CUIT invalido", r.get(0).getMotivoRechazo());
        assertEquals("Mayorista", r.get(0).getListaPrecio().getDenominacion());
    }

    @Test
    void filtraPorMontoMinimo() {
        assertEquals(2, buscar(null, null, null, "500").size());
    }

    @Test
    void filtraPorFechaDeCreacionConLimitesInclusivos() {
        // 10/02 y 10/03 entran: ambos extremos son inclusivos (el "hasta" cubre todo el día)
        assertEquals(2, buscar(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10), null, null).size());
        assertEquals(1, buscar(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10), null, null).size());
        assertEquals(0, buscar(LocalDate.of(2026, 5, 1), null, null, null).size());
        assertEquals(3, buscar(null, LocalDate.of(2026, 3, 31), null, null).size());
    }

    @Test
    void combinaFiltros() {
        assertEquals(1, buscar(null, null, EstadoFactura.EMITIDA, "100").size());
        assertEquals(0, buscar(null, null, EstadoFactura.PENDIENTE, "101").size());
    }

    @Test
    void paginaConTotalesCorrectos() {
        Specification<FacturaVenta> todas = FacturaSpecs.filtrar(null, null, null, null);

        Page<FacturaVenta> p0 = repo.findAll(todas, PageRequest.of(0, 3, ORDEN));
        assertEquals(3, p0.getContent().size());
        assertEquals(4, p0.getTotalElements());
        assertEquals(2, p0.getTotalPages());

        Page<FacturaVenta> p1 = repo.findAll(todas, PageRequest.of(1, 3, ORDEN));
        assertEquals(1, p1.getContent().size());
        assertEquals(EstadoFactura.PENDIENTE, p1.getContent().get(0).getEstado());
    }

    @Test
    void laPaginaTraeLasRelacionesToOneSinConsultasAdicionales() {
        Page<FacturaVenta> p = repo.findAll(FacturaSpecs.filtrar(null, null, null, null), PageRequest.of(0, 10, ORDEN));
        for (FacturaVenta f : p.getContent()) {
            var util = em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
            assertTrue(util.isLoaded(f, "cliente"));
            assertTrue(util.isLoaded(f, "listaPrecio"));
            assertTrue(util.isLoaded(f, "condicionIva"));
            assertTrue(util.isLoaded(f, "puntoVenta"));
            assertTrue(util.isLoaded(f, "tipoMoneda"));
        }
    }

    @Test
    void cuentaRenglonesPorFacturaEnUnaSolaConsulta() {
        List<FacturaVenta> todas = buscar(null, null, null, null);
        List<Long> ids = todas.stream().map(FacturaVenta::getId).toList();
        List<Object[]> filas = repo.contarRenglones(ids);
        assertEquals(4, filas.size());
        assertTrue(filas.stream().allMatch(f -> ((Long) f[1]) == 1L));
    }
}
