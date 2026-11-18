package com.utn.tp_spring_grupal.config;

import com.utn.tp_spring_grupal.afip.AfipWsfeService;
import com.utn.tp_spring_grupal.afip.Cuit;
import com.utn.tp_spring_grupal.articulo.Articulo;
import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.factura.EstadoFactura;
import com.utn.tp_spring_grupal.factura.FacturaVenta;
import com.utn.tp_spring_grupal.factura.FacturaVentaDetalle;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

/** Arranca el contexto completo sobre H2: valida el mapeo JPA y los datos que carga el DataLoader. */
@SpringBootTest
@Transactional
class DataLoaderTest {

    @Autowired EntityManager em;
    @MockBean AfipWsfeService afip;

    private long contar(String entidad) {
        return em.createQuery("select count(e) from " + entidad + " e", Long.class).getSingleResult();
    }

    private List<FacturaVenta> facturas() {
        return em.createQuery("select f from FacturaVenta f", FacturaVenta.class).getResultList();
    }

    @Test
    void cargaCatalogosAbundantes() {
        assertTrue(contar("Cliente") >= 25);
        assertEquals(5, contar("Rubro"));
        assertEquals(6, contar("Marca"));
        assertTrue(contar("Articulo") >= 40);
        assertEquals(3, contar("ListaPrecio"));
        assertEquals(6, contar("AlicuotaIva"));
        assertEquals(1, contar("PuntoVenta"));
        assertTrue(contar("TipoMoneda") >= 3);
    }

    @Test
    void unicoPuntoVentaEsElConfiguradoEnArca() {
        List<Integer> numeros = em.createQuery("select p.numero from PuntoVenta p", Integer.class).getResultList();
        assertEquals(List.of(2), numeros);
    }

    @Test
    void losCuitDeLosClientesTienenDigitoVerificadorValido() {
        List<Cliente> clientes = em.createQuery("select c from Cliente c", Cliente.class).getResultList();
        for (Cliente c : clientes) {
            assertTrue(Cuit.esValido(c.getCuitCuil()), "CUIT invalido: " + c.getCuitCuil());
        }
        assertEquals(clientes.size(), clientes.stream().map(Cliente::getCuitCuil).distinct().count());
    }

    @Test
    void lasMonedasUsanLosCodigosDeArca() {
        List<String> codigos = em.createQuery("select m.codigoAfip from TipoMoneda m", String.class).getResultList();
        // Codigos de FEParamGetTiposMonedas: PES, DOL y 060 (euro). "EUR" no existe en ARCA.
        assertEquals(Set.of("PES", "DOL", "060"), Set.copyOf(codigos));
        assertEquals(codigos.size(), Set.copyOf(codigos).size());
    }

    @Test
    void laBaseNoLlamaAAfipAlArrancar() {
        verifyNoInteractions(afip);
    }

    @Test
    void condicionesDeIvaIncluyenLosCodigosAfipDelPlan() {
        Set<Integer> codigos = em.createQuery("select c.codigoAfip from CondicionIva c", Integer.class)
                .getResultList().stream().collect(Collectors.toSet());
        assertEquals(Set.of(1, 4, 5, 6, 15), codigos);
    }

    @Test
    void alicuotasSonLasDeAfipConSuCodigo() {
        List<Object[]> filas = em.createQuery("select a.codigoAfip, a.porcentaje from AlicuotaIva a", Object[].class).getResultList();
        Set<String> codigoPorcentaje = filas.stream()
                .map(f -> f[0] + ":" + ((BigDecimal) f[1]).stripTrailingZeros().toPlainString())
                .collect(Collectors.toSet());
        assertEquals(Set.of("3:0", "9:2.5", "8:5", "4:10.5", "5:21", "6:27"), codigoPorcentaje);
    }

    @Test
    void clientesTienenCondicionesDeIvaVariadas() {
        List<Cliente> clientes = em.createQuery("select c from Cliente c", Cliente.class).getResultList();
        Set<Integer> condiciones = clientes.stream().map(c -> c.getCondicionIva().getCodigoAfip()).collect(Collectors.toSet());
        assertTrue(condiciones.containsAll(Set.of(1, 4, 5, 6)), "RI, Exento, Consumidor Final y Monotributo presentes: " + condiciones);
    }

    @Test
    void articulosTienenAlicuotasVariadasConMayoriaAl21() {
        List<Articulo> articulos = em.createQuery("select a from Articulo a", Articulo.class).getResultList();
        List<BigDecimal> porcentajes = articulos.stream().map(a -> a.getAlicuotaIva().getPorcentaje()).toList();
        Set<String> distintos = porcentajes.stream().map(p -> p.stripTrailingZeros().toPlainString()).collect(Collectors.toSet());
        assertTrue(distintos.containsAll(Set.of("21", "10.5", "27", "0", "5")), "alicuotas: " + distintos);
        long al21 = porcentajes.stream().filter(p -> p.compareTo(new BigDecimal("21")) == 0).count();
        assertTrue(al21 * 2 > porcentajes.size(), "el 21 % es la alicuota mayoritaria");
    }

    @Test
    void listasDePrecioTienenArticulosDistintosYPreciosDistintos() {
        List<Long> porLista = em.createQuery(
                "select count(p) from ListaPrecioArticulo p group by p.listaPrecio.id", Long.class).getResultList();
        assertEquals(3, porLista.size());
        assertEquals(3, porLista.stream().distinct().count(), "cada lista debe tener distinta cantidad de articulos");
        Long conPrecioDistinto = em.createQuery(
                "select count(a) from ListaPrecioArticulo a join ListaPrecioArticulo b on a.articulo.id = b.articulo.id "
                        + "and a.listaPrecio.id < b.listaPrecio.id where a.precioVenta <> b.precioVenta", Long.class).getSingleResult();
        assertTrue(conPrecioDistinto > 0);
    }

    @Test
    void noHayFacturasEmitidasNiConNumeroCaeOFechaDeEmision() {
        List<FacturaVenta> facturas = facturas();
        assertTrue(facturas.stream().noneMatch(f -> f.getEstado() == EstadoFactura.EMITIDA), "no debe haber EMITIDA");
        for (FacturaVenta f : facturas) {
            assertNull(f.getNumero());
            assertNull(f.getCae());
            assertNull(f.getCaeFechaVencimiento());
            assertNull(f.getFechaEmision());
        }
    }

    @Test
    void hayAlMenos30FacturasConElMixDeEstadosPrevisto() {
        List<FacturaVenta> facturas = facturas();
        assertTrue(facturas.size() >= 30, "facturas: " + facturas.size());

        long pendientes = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PENDIENTE).count();
        long conError = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.ERROR_AFIP).count();
        long eliminadas = facturas.stream().filter(f -> f.getFechaBaja() != null).count();
        assertTrue(pendientes >= 20, "pendientes: " + pendientes);
        assertTrue(conError >= 4, "con error: " + conError);
        assertTrue(eliminadas >= 3, "eliminadas: " + eliminadas);
        assertEquals(facturas.size(), pendientes + conError);
    }

    @Test
    void noHayAnuladasPorqueSoloSeAnulaUnaFacturaEmitida() {
        assertTrue(facturas().stream().noneMatch(f -> f.getEstado() == EstadoFactura.ANULADA));
    }

    @Test
    void lasEliminadasSonNoEmitidasConUsuarioDeBaja() {
        List<FacturaVenta> eliminadas = facturas().stream().filter(f -> f.getFechaBaja() != null).toList();
        assertFalse(eliminadas.isEmpty());
        for (FacturaVenta f : eliminadas) {
            assertTrue(f.getEstado().esEditable(), "solo se eliminan PENDIENTE / ERROR_AFIP");
            assertNotNull(f.getUsuarioBaja());
            assertNull(f.getFechaAnulacion());
        }
    }

    @Test
    void fechasDeCreacionEstanRepartidasEnVariosDias() {
        long dias = facturas().stream()
                .map(f -> f.getFechaAlta().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .distinct().count();
        assertTrue(dias >= 7, "dias distintos: " + dias);
    }

    @Test
    void facturasSonCoherentesEnImportesRenglonesYEstados() {
        for (FacturaVenta f : facturas()) {
            assertNotNull(f.getListaPrecio(), "toda factura tiene lista de precios");
            assertNotNull(f.getCliente());
            assertEquals(f.getCliente().getCondicionIva().getId(), f.getCondicionIva().getId(),
                    "la condicion IVA de la factura es la del cliente");
            assertFalse(f.getDetalles().isEmpty());

            BigDecimal sumaNeto = BigDecimal.ZERO;
            BigDecimal sumaIva = BigDecimal.ZERO;
            BigDecimal sumaSubtotal = BigDecimal.ZERO;
            for (FacturaVentaDetalle d : f.getDetalles()) {
                assertEquals(0, d.getImporteNeto().add(d.getImporteIva()).compareTo(d.getImporteSubtotal()),
                        "neto + IVA = subtotal en cada renglon");
                assertEquals(f.getListaPrecio().getId(), d.getListaPrecioArticulo().getListaPrecio().getId(),
                        "los renglones usan precios de la lista de la factura");
                assertEquals(0, d.getAlicuotaPorcentaje().compareTo(d.getListaPrecioArticulo().getArticulo().getAlicuotaIva().getPorcentaje()),
                        "snapshot de la alicuota del articulo");
                assertTrue(d.getCantidad() >= 1);
                sumaNeto = sumaNeto.add(d.getImporteNeto());
                sumaIva = sumaIva.add(d.getImporteIva());
                sumaSubtotal = sumaSubtotal.add(d.getImporteSubtotal());
            }
            assertEquals(0, f.getImporteNeto().compareTo(sumaNeto));
            assertEquals(0, f.getImporteIva().compareTo(sumaIva));
            assertEquals(0, f.getImporteTotal().compareTo(sumaSubtotal), "suma de subtotales = total");
            assertEquals(0, f.getImporteTotal().compareTo(f.getImporteNeto().add(f.getImporteIva())));
            assertEquals(0, f.getImporteTotal().subtract(f.getImporteCobrado()).compareTo(f.getImporteSaldo()));

            switch (f.getEstado()) {
                case ERROR_AFIP -> assertNotNull(f.getMotivoRechazo());
                case PENDIENTE -> assertNull(f.getMotivoRechazo());
                case ANULADA -> assertNotNull(f.getFechaAnulacion());
                case EMITIDA -> throw new AssertionError("no debe haber EMITIDA");
            }
        }
    }

    @Test
    void hayFacturasConMasDeUnaAlicuotaEnSusRenglones() {
        long conVarias = facturas().stream()
                .filter(f -> f.getDetalles().stream().map(d -> d.getAlicuotaPorcentaje().stripTrailingZeros()).distinct().count() > 1)
                .count();
        assertTrue(conVarias > 0, "al menos una factura con desglose de varias alicuotas");
    }
}
