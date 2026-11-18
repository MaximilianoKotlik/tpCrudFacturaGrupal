package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.afip.AfipComunicacionException;
import com.utn.tp_spring_grupal.afip.AfipException;
import com.utn.tp_spring_grupal.afip.AfipWsfeService;
import com.utn.tp_spring_grupal.afip.ComprobanteAutorizado;
import com.utn.tp_spring_grupal.afip.DocumentoReceptor;
import com.utn.tp_spring_grupal.afip.SolicitudCae;
import com.utn.tp_spring_grupal.afip.WsfeRespuesta;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.ZoneId;
import java.util.Optional;
import com.utn.tp_spring_grupal.alicuotaiva.AlicuotaIva;
import com.utn.tp_spring_grupal.articulo.Articulo;
import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.cliente.ClienteRepository;
import com.utn.tp_spring_grupal.common.PageResponse;
import com.utn.tp_spring_grupal.common.ReglaNegocioException;
import com.utn.tp_spring_grupal.common.ValidacionException;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;
import com.utn.tp_spring_grupal.condicioniva.CondicionIvaRepository;
import com.utn.tp_spring_grupal.emisor.CondicionIvaEmisor;
import com.utn.tp_spring_grupal.emisor.EmisorProperties;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecio;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticulo;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticuloRepository;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioRepository;
import com.utn.tp_spring_grupal.listaprecio.PrecioListaDTO;
import com.utn.tp_spring_grupal.puntoventa.PuntoVenta;
import com.utn.tp_spring_grupal.puntoventa.PuntoVentaRepository;
import com.utn.tp_spring_grupal.tipomoneda.TipoMoneda;
import com.utn.tp_spring_grupal.tipomoneda.TipoMonedaRepository;
import com.utn.tp_spring_grupal.usuario.Usuario;
import com.utn.tp_spring_grupal.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FacturaServiceTest {

    @Mock FacturaVentaRepository facturaRepo;
    @Mock ClienteRepository clienteRepo;
    @Mock ListaPrecioRepository listaRepo;
    @Mock ListaPrecioArticuloRepository lpaRepo;
    @Mock PuntoVentaRepository pvRepo;
    @Mock TipoMonedaRepository monedaRepo;
    @Mock UsuarioRepository usuarioRepo;
    @Mock CondicionIvaRepository condicionIvaRepo;
    @Mock AfipWsfeService afip;
    @Mock PlatformTransactionManager tm;

    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    EmisorProperties emisor;
    FacturaService service;

    Usuario usuario;
    Cliente cliente, clienteConsumidorFinal;
    ListaPrecio listaA, listaB;
    CondicionIva ivaRi, ivaConsumidorFinal;
    AlicuotaIva alicuota21, alicuota105;
    PuntoVenta pv;
    TipoMoneda moneda;
    Articulo articulo, articuloReducido;
    ListaPrecioArticulo precioListaA, precioListaB, precioReducidoListaA;

    @BeforeEach
    void setUp() {
        emisor = new EmisorProperties();
        emisor.setCuit("20-11111111-2");
        emisor.setRazonSocial("Comercial Ejemplo SRL");
        emisor.setDomicilioComercial("Av. Siempreviva 742 - CABA");
        emisor.setIngresosBrutos("901-123456-7");
        emisor.setInicioActividades(LocalDate.of(2020, 3, 1));
        service = new FacturaService(facturaRepo, clienteRepo, listaRepo, lpaRepo, pvRepo, monedaRepo, usuarioRepo,
                condicionIvaRepo, afip, emisor, tm);

        usuario = Usuario.builder().id(1L).usuario("u").clave("c").nombre("n").apellido("a").build();
        ivaRi = CondicionIva.builder().id(20L).codigoAfip(1).denominacion("Resp. Inscripto").build();
        ivaConsumidorFinal = CondicionIva.builder().id(21L).codigoAfip(5).denominacion("Consumidor Final").build();
        cliente = Cliente.builder().id(10L).denominacion("Juan Perez").cuitCuil("20-28123456-7").condicionIva(ivaRi).build();
        clienteConsumidorFinal = Cliente.builder().id(11L).denominacion("Ana CF").cuitCuil("27-30456789-4")
                .condicionIva(ivaConsumidorFinal).build();
        listaA = ListaPrecio.builder().id(100L).codigo("A").denominacion("Mayorista").build();
        listaB = ListaPrecio.builder().id(200L).codigo("B").denominacion("Minorista").build();
        pv = PuntoVenta.builder().id(30L).numero(1).descripcion("Centro").build();
        moneda = TipoMoneda.builder().id(40L).codigoAfip("PES").denominacion("Pesos").simbolo("$").build();
        alicuota21 = AlicuotaIva.builder().id(1L).codigoAfip(5).porcentaje(new BigDecimal("21.00")).denominacion("IVA 21 %").build();
        alicuota105 = AlicuotaIva.builder().id(2L).codigoAfip(4).porcentaje(new BigDecimal("10.50")).denominacion("IVA 10,5 %").build();
        articulo = Articulo.builder().id(50L).codigo("ART-1").denominacion("Mouse").alicuotaIva(alicuota21).build();
        articuloReducido = Articulo.builder().id(51L).codigo("ART-2").denominacion("Yerba").alicuotaIva(alicuota105).build();
        precioListaA = ListaPrecioArticulo.builder().id(1L).listaPrecio(listaA).articulo(articulo).precioVenta(bd("100.00")).build();
        precioListaB = ListaPrecioArticulo.builder().id(2L).listaPrecio(listaB).articulo(articulo).precioVenta(bd("250.00")).build();
        precioReducidoListaA = ListaPrecioArticulo.builder().id(3L).listaPrecio(listaA).articulo(articuloReducido).precioVenta(bd("200.00")).build();

        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepo.findById(10L)).thenReturn(Optional.of(cliente));
        when(clienteRepo.findById(11L)).thenReturn(Optional.of(clienteConsumidorFinal));
        when(condicionIvaRepo.findFirstByCodigoAfip(5)).thenReturn(Optional.of(ivaConsumidorFinal));
        when(listaRepo.findById(100L)).thenReturn(Optional.of(listaA));
        when(listaRepo.findById(200L)).thenReturn(Optional.of(listaB));
        when(pvRepo.findById(30L)).thenReturn(Optional.of(pv));
        when(monedaRepo.findById(40L)).thenReturn(Optional.of(moneda));
        when(lpaRepo.findByListaPrecioIdAndArticuloId(100L, 50L)).thenReturn(Optional.of(precioListaA));
        when(lpaRepo.findByListaPrecioIdAndArticuloId(200L, 50L)).thenReturn(Optional.of(precioListaB));
        when(lpaRepo.findByListaPrecioIdAndArticuloId(100L, 51L)).thenReturn(Optional.of(precioReducidoListaA));
        when(facturaRepo.save(any(FacturaVenta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- helpers ----------

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static void assertImporte(String esperado, BigDecimal real) {
        assertEquals(0, bd(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    private FacturaDetalleDTO renglon(Integer cantidad, String bonif) {
        return renglon(50L, cantidad, bonif);
    }

    private FacturaDetalleDTO renglon(Long articuloId, Integer cantidad, String bonif) {
        return new FacturaDetalleDTO(articuloId, "Mouse", cantidad, bd(bonif), null, null, null, null, null, null);
    }

    private FacturaGuardarDTO guardar(Long listaId, List<FacturaDetalleDTO> detalles) {
        return new FacturaGuardarDTO("obs", 10L, listaId, 30L, 40L, detalles);
    }

    private FacturaVenta facturaExistente(EstadoFactura estado) {
        return facturaExistente(7L, estado);
    }

    private FacturaVenta facturaExistente(long id, EstadoFactura estado) {
        FacturaVenta f = FacturaVenta.builder()
                .id(id).estado(estado).numero(null).fechaEmision(null)
                .cliente(cliente).listaPrecio(listaA).condicionIva(ivaRi).puntoVenta(pv).tipoMoneda(moneda)
                .importeNeto(bd("100.00")).importeIva(bd("21.00")).importeTotal(bd("121.00"))
                .importeCobrado(BigDecimal.ZERO).importeSaldo(bd("121.00"))
                .fechaAlta(new Date())
                .detalles(new ArrayList<>()).build();
        f.getDetalles().add(FacturaVentaDetalle.builder().factura(f).listaPrecioArticulo(precioListaA)
                .descripcion("Mouse").cantidad(1).precioUnitario(bd("100.00")).porcentajeBonificacion(bd("0"))
                .alicuotaPorcentaje(bd("21.00")).alicuotaCodigoAfip(5)
                .importeNeto(bd("100.00")).importeIva(bd("21.00")).importeSubtotal(bd("121.00")).build());
        when(facturaRepo.findById(id)).thenReturn(Optional.of(f));
        return f;
    }

    private void afipResponde() throws Exception {
        when(afip.obtenerUltimoComprobante(anyInt(), anyInt())).thenReturn(10L);
        when(afip.obtenerCotizacion(any())).thenReturn(BigDecimal.ONE);
        when(afip.solicitarCae(any(SolicitudCae.class)))
                .thenReturn(new WsfeRespuesta("A", "CAE-123", "20261231", List.of(), List.of()));
    }

    private SolicitudCae solicitudEnviada() {
        ArgumentCaptor<SolicitudCae> cap = ArgumentCaptor.forClass(SolicitudCae.class);
        verify(afip).solicitarCae(cap.capture());
        return cap.getValue();
    }

    private FacturaVenta guardada() {
        ArgumentCaptor<FacturaVenta> cap = ArgumentCaptor.forClass(FacturaVenta.class);
        verify(facturaRepo).save(cap.capture());
        return cap.getValue();
    }

    // ---------- crear ----------

    @Test
    void crearDejaLaFacturaPendienteSinNumeroCaeNiFechaDeEmisionYSinLlamarAAfip() {
        FacturaDTO r = service.crearFactura(guardar(100L, List.of(renglon(2, "10"))));

        assertEquals(EstadoFactura.PENDIENTE, r.estado());
        assertNull(r.numeroFactura());
        assertNull(r.cae());
        assertNull(r.fechaEmision());
        assertNotNull(r.fechaCreacion());
        assertEquals("Mayorista", r.listaPrecioDenominacion());
        assertEquals(1, r.detalles().size());
        verifyNoInteractions(afip);
    }

    @Test
    void crearTomaLaCondicionIvaDelCliente() {
        FacturaDTO r = service.crearFactura(new FacturaGuardarDTO(null, 11L, 100L, 30L, 40L, List.of(renglon(1, "0"))));

        assertEquals(21L, r.condicionIvaId());
        assertEquals("Consumidor Final", r.condicionIvaDenominacion());
        assertEquals(ivaConsumidorFinal, guardada().getCondicionIva());
    }

    @Test
    void crearCalculaImportesConBonificacionEIva21() {
        // 100 * 2 con 10 % de bonificación = 180 neto; IVA 37,80; subtotal 217,80
        service.crearFactura(guardar(100L, List.of(renglon(2, "10"))));

        FacturaVenta f = guardada();
        FacturaVentaDetalle d = f.getDetalles().get(0);
        assertImporte("180.00", d.getImporteNeto());
        assertImporte("37.80", d.getImporteIva());
        assertImporte("217.80", d.getImporteSubtotal());
        assertImporte("180.00", f.getImporteNeto());
        assertImporte("37.80", f.getImporteIva());
        assertImporte("217.80", f.getImporteTotal());
        assertImporte("0", f.getImporteCobrado());
        assertImporte("217.80", f.getImporteSaldo());
        assertEquals(listaA, f.getListaPrecio());
    }

    @Test
    void crearGuardaElSnapshotDeLaAlicuotaDelArticuloEnCadaRenglon() {
        service.crearFactura(guardar(100L, List.of(renglon(50L, 1, "0"), renglon(51L, 1, "0"))));

        FacturaVenta f = guardada();
        assertImporte("21", f.getDetalles().get(0).getAlicuotaPorcentaje());
        assertEquals(5, f.getDetalles().get(0).getAlicuotaCodigoAfip());
        assertImporte("10.5", f.getDetalles().get(1).getAlicuotaPorcentaje());
        assertEquals(4, f.getDetalles().get(1).getAlicuotaCodigoAfip());

        // el snapshot no cambia si después cambia la alícuota del artículo
        alicuota21.setPorcentaje(bd("27.00"));
        assertImporte("21", f.getDetalles().get(0).getAlicuotaPorcentaje());
    }

    @Test
    void crearConAlicuotasMixtasSumaNetoIvaYTotalPorRenglon() {
        // 100 al 21 % -> IVA 21,00 | 200 * 3 = 600 al 10,5 % -> IVA 63,00
        service.crearFactura(guardar(100L, List.of(renglon(50L, 1, "0"), renglon(51L, 3, "0"))));

        FacturaVenta f = guardada();
        assertImporte("700.00", f.getImporteNeto());
        assertImporte("84.00", f.getImporteIva());
        assertImporte("784.00", f.getImporteTotal());
    }

    @Test
    void crearConEmisorMonotributoNoDiscriminaIva() {
        emisor.setCondicionIvaEmisor(CondicionIvaEmisor.MONOTRIBUTO);

        FacturaDTO r = service.crearFactura(guardar(100L, List.of(renglon(2, "0"))));

        FacturaVenta f = guardada();
        assertImporte("200.00", f.getImporteNeto());
        assertImporte("0.00", f.getImporteIva());
        assertImporte("200.00", f.getImporteTotal());
        assertImporte("0", f.getDetalles().get(0).getAlicuotaPorcentaje());
        assertEquals(3, f.getDetalles().get(0).getAlicuotaCodigoAfip());
        assertEquals("C", r.tipoComprobante());
    }

    @Test
    void crearExponeLaLetraDelComprobanteSegunEmisorYReceptor() {
        assertEquals("A", service.crearFactura(guardar(100L, List.of(renglon(1, "0")))).tipoComprobante());
        assertEquals("B", service.crearFactura(new FacturaGuardarDTO(null, 11L, 100L, 30L, 40L, List.of(renglon(1, "0"))))
                .tipoComprobante());
    }

    @Test
    void crearTomaElPrecioDeLaListaElegidaYNoDeOtra() {
        service.crearFactura(guardar(200L, List.of(renglon(1, "0"))));

        assertImporte("250.00", guardada().getDetalles().get(0).getPrecioUnitario());
        verify(lpaRepo).findByListaPrecioIdAndArticuloId(200L, 50L);
        verify(lpaRepo, never()).findByListaPrecioIdAndArticuloId(100L, 50L);
    }

    @Test
    void crearConArticuloQueNoEstaEnLaListaFallaConNoSeEncontro() {
        when(lpaRepo.findByListaPrecioIdAndArticuloId(200L, 99L)).thenReturn(Optional.empty());

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> service.crearFactura(guardar(200L, List.of(renglon(99L, 1, "0")))));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().startsWith("No se encontr"));
    }

    @Test
    void crearSinRenglonesEsInvalido() {
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, List.of())));
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, null)));
    }

    @Test
    void crearSinClienteOSinListaEsInvalido() {
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(null, List.of(renglon(1, "0")))));
        assertThrows(ValidacionException.class,
                () -> service.crearFactura(new FacturaGuardarDTO(null, null, 100L, 30L, 40L, List.of(renglon(1, "0")))));
    }

    // ---------- consumidor final sin cliente ----------

    private FacturaGuardarDTO consumidorFinal(Long clienteId) {
        return new FacturaGuardarDTO("obs", clienteId, 100L, 30L, 40L, List.of(renglon(1, "0")), true);
    }

    @Test
    void crearAConsumidorFinalSinClienteUsaLaCondicionConsumidorFinal() {
        FacturaDTO r = service.crearFactura(consumidorFinal(null));

        FacturaVenta f = guardada();
        assertNull(f.getCliente());
        assertEquals(ivaConsumidorFinal, f.getCondicionIva());
        assertNull(r.clienteId());
        assertEquals("Consumidor Final", r.clienteDenominacion());
        assertEquals("B", r.tipoComprobante());
    }

    @Test
    void crearAConsumidorFinalConClienteEsInvalido() {
        assertThrows(ValidacionException.class, () -> service.crearFactura(consumidorFinal(10L)));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    void crearAConsumidorFinalSinLaCondicionCargadaEsInvalido() {
        when(condicionIvaRepo.findFirstByCodigoAfip(5)).thenReturn(Optional.empty());
        assertThrows(ValidacionException.class, () -> service.crearFactura(consumidorFinal(null)));
    }

    @Test
    void actualizarAConsumidorFinalQuitaElClienteYCambiaLaCondicion() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        service.actualizarFactura(7L, new FacturaGuardarDTO(null, null, null, null, null, null, true));

        assertNull(f.getCliente());
        assertEquals(ivaConsumidorFinal, f.getCondicionIva());
    }

    @Test
    void actualizarDeConsumidorFinalAUnClienteLoAsignaConSuCondicion() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setCliente(null);
        f.setCondicionIva(ivaConsumidorFinal);

        service.actualizarFactura(7L, new FacturaGuardarDTO(null, 10L, null, null, null, null, false));

        assertEquals(cliente, f.getCliente());
        assertEquals(ivaRi, f.getCondicionIva());
    }

    @Test
    void crearConCantidadCeroNegativaONulaEsInvalido() {
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, List.of(renglon(0, "0")))));
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, List.of(renglon(-3, "0")))));
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, List.of(renglon(null, "0")))));
    }

    @Test
    void crearConBonificacionFueraDeRangoEsInvalido() {
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, List.of(renglon(1, "-1")))));
        assertThrows(ValidacionException.class, () -> service.crearFactura(guardar(100L, List.of(renglon(1, "100.5")))));
    }

    @Test
    void crearAceptaBonificacionEnLosLimites() {
        service.crearFactura(guardar(100L, List.of(renglon(1, "0"), renglon(1, "100"))));
        verify(facturaRepo).save(any(FacturaVenta.class));
    }

    // ---------- actualizar ----------

    @Test
    void actualizarFacturaEmitidaOAnuladaDa409() {
        facturaExistente(EstadoFactura.EMITIDA);
        FacturaGuardarDTO dto = new FacturaGuardarDTO("nuevo", null, null, null, null, null);
        assertThrows(ReglaNegocioException.class, () -> service.actualizarFactura(7L, dto));

        facturaExistente(EstadoFactura.ANULADA);
        assertThrows(ReglaNegocioException.class, () -> service.actualizarFactura(7L, dto));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    void actualizarPendienteAplicaSoloLosCamposRecibidos() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.actualizarFactura(7L, new FacturaGuardarDTO("cambio", null, null, null, null, null));

        assertEquals("cambio", f.getObservaciones());
        assertImporte("121.00", f.getImporteTotal());
        assertEquals(EstadoFactura.PENDIENTE, r.estado());
        assertEquals(ivaRi, f.getCondicionIva());
    }

    @Test
    void actualizarErrorAfipEstaPermitidoYRecalculaConLaNuevaLista() {
        FacturaVenta f = facturaExistente(EstadoFactura.ERROR_AFIP);

        service.actualizarFactura(7L, new FacturaGuardarDTO(null, null, 200L, null, null, List.of(renglon(2, "0"))));

        assertEquals(listaB, f.getListaPrecio());
        assertEquals(1, f.getDetalles().size());
        assertImporte("250.00", f.getDetalles().get(0).getPrecioUnitario());
        assertImporte("500.00", f.getImporteNeto());
        assertImporte("105.00", f.getImporteIva());
        assertImporte("605.00", f.getImporteTotal()); // 2 * 250 * 1,21
        assertImporte("605.00", f.getImporteSaldo());
    }

    @Test
    void actualizarElClienteCopiaSuCondicionIva() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        service.actualizarFactura(7L, new FacturaGuardarDTO(null, 11L, null, null, null, null));

        assertEquals(clienteConsumidorFinal, f.getCliente());
        assertEquals(ivaConsumidorFinal, f.getCondicionIva());
    }

    @Test
    void actualizarCambiandoLaListaSinEnviarRenglonesEsInvalido() {
        facturaExistente(EstadoFactura.PENDIENTE);
        assertThrows(ValidacionException.class,
                () -> service.actualizarFactura(7L, new FacturaGuardarDTO(null, null, 200L, null, null, null)));
    }

    @Test
    void actualizarConRenglonesInvalidosEsInvalido() {
        facturaExistente(EstadoFactura.PENDIENTE);
        assertThrows(ValidacionException.class,
                () -> service.actualizarFactura(7L, new FacturaGuardarDTO(null, null, null, null, null, List.of())));
        assertThrows(ValidacionException.class,
                () -> service.actualizarFactura(7L, new FacturaGuardarDTO(null, null, null, null, null, List.of(renglon(0, "0")))));
    }

    @Test
    void actualizarFacturaInexistenteDa404() {
        when(facturaRepo.findById(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.actualizarFactura(999L, new FacturaGuardarDTO(null, null, null, null, null, null)));
        assertEquals("No se encontró la factura. Es posible que ya no exista.", ex.getMessage());
    }

    // ---------- eliminar (baja lógica) ----------

    @Test
    void eliminarPendienteLaDaDeBajaSinCambiarElEstado() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        service.eliminarFactura(7L);

        assertNotNull(f.getFechaBaja());
        assertEquals(usuario, f.getUsuarioBaja());
        assertEquals(EstadoFactura.PENDIENTE, f.getEstado());
        verify(facturaRepo).save(f);
    }

    @Test
    void eliminarErrorAfipEstaPermitido() {
        FacturaVenta f = facturaExistente(EstadoFactura.ERROR_AFIP);
        service.eliminarFactura(7L);
        assertNotNull(f.getFechaBaja());
    }

    @Test
    void eliminarErrorAfipConNumeroReservadoDa409ParaNoPerderUnCaeQuizasAutorizado() {
        FacturaVenta f = facturaExistente(EstadoFactura.ERROR_AFIP);
        f.setNumero(11L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.eliminarFactura(7L));

        assertTrue(ex.getMessage().contains("emitir"));
        assertNull(f.getFechaBaja());
        verify(facturaRepo, never()).save(any());
    }

    @Test
    void eliminarEmitidaOAnuladaDa409() {
        FacturaVenta emitida = facturaExistente(EstadoFactura.EMITIDA);
        assertThrows(ReglaNegocioException.class, () -> service.eliminarFactura(7L));
        assertNull(emitida.getFechaBaja());

        facturaExistente(EstadoFactura.ANULADA);
        assertThrows(ReglaNegocioException.class, () -> service.eliminarFactura(7L));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    void unaFacturaDadaDeBajaYaNoSeEncuentra() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setFechaBaja(new Date());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.obtenerFactura(7L));
        assertEquals("No se encontró la factura. Es posible que ya no exista.", ex.getMessage());
        assertThrows(RuntimeException.class, () -> service.eliminarFactura(7L));
    }

    // ---------- anular (emitidas) ----------

    @Test
    void anularEmitidaLaDejaAnuladaConFechaYConservaElCae() {
        FacturaVenta f = facturaExistente(EstadoFactura.EMITIDA);
        f.setCae("CAE-1");
        f.setNumero(11L);

        FacturaDTO r = service.anularFactura(7L);

        assertEquals(EstadoFactura.ANULADA, f.getEstado());
        assertNotNull(f.getFechaAnulacion());
        assertNull(f.getFechaBaja());
        assertEquals("CAE-1", f.getCae());
        assertEquals(EstadoFactura.ANULADA, r.estado());
        verifyNoInteractions(afip);
    }

    @Test
    void anularPendienteOErrorAfipDa409() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.anularFactura(7L));
        assertTrue(ex.getMessage().contains("emitidas"));
        assertEquals(EstadoFactura.PENDIENTE, f.getEstado());

        facturaExistente(EstadoFactura.ERROR_AFIP);
        assertThrows(ReglaNegocioException.class, () -> service.anularFactura(7L));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    void anularDosVecesDa409() {
        facturaExistente(EstadoFactura.EMITIDA);
        service.anularFactura(7L);
        assertThrows(ReglaNegocioException.class, () -> service.anularFactura(7L));
    }

    // ---------- emitir ----------

    @Test
    void emitirConAfipOkDejaEmitidaConNumeroCaeYFechaDeEmisionHoy() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.EMITIDA, r.estado());
        assertEquals(11L, r.numeroFactura());
        assertEquals("CAE-123", r.cae());
        assertEquals(LocalDate.now(ZONA), r.fechaEmision());
        assertEquals(LocalDate.of(2026, 12, 31), r.caeFechaVencimiento());
        assertEquals("CAE-123", f.getCae());
        assertEquals(LocalDate.now(ZONA), f.getFechaEmision());
        assertNull(f.getMotivoRechazo());
        assertEquals(1, f.getTipoComprobante());
    }

    @Test
    void emitirUsaElTipoDeComprobanteDelResolverYLosImportesPersistidos() throws Exception {
        afipResponde();
        facturaExistente(EstadoFactura.PENDIENTE); // emisor RI, receptor RI -> A (1); neto 100, IVA 21, total 121

        service.emitirFactura(7L);

        verify(afip).obtenerUltimoComprobante(1, 1);
        SolicitudCae sol = solicitudEnviada();
        assertEquals(1, sol.puntoVenta());
        assertEquals(1, sol.tipoComprobante());
        assertEquals(11L, sol.numero());
        assertEquals(new DocumentoReceptor(80, 20281234567L), sol.documento());
        assertEquals("PES", sol.monId());
        assertEquals(0, BigDecimal.ONE.compareTo(sol.monCotiz()));
        assertEquals(1, sol.condicionIvaReceptorId());
        assertEquals(LocalDate.now(ZONA), sol.fechaComprobante());
        assertImporte("121.00", sol.importeTotal());
        assertImporte("100.00", sol.importeNeto());
        assertImporte("21.00", sol.importeIva());
        assertEquals(1, sol.desgloseIva().size());
        assertEquals(5, sol.desgloseIva().get(0).codigoAfip());
        assertImporte("100.00", sol.desgloseIva().get(0).baseImponible());
        assertImporte("21.00", sol.desgloseIva().get(0).importe());
    }

    @Test
    void emitirAConsumidorFinalUsaFacturaB() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setCliente(clienteConsumidorFinal);
        f.setCondicionIva(ivaConsumidorFinal);

        FacturaDTO r = service.emitirFactura(7L);

        verify(afip).obtenerUltimoComprobante(1, 6);
        assertEquals("B", r.tipoComprobante());
        assertEquals(6, f.getTipoComprobante());
    }

    @Test
    void emitirConEmisorMonotributoUsaFacturaCSinDesgloseDeIva() throws Exception {
        afipResponde();
        emisor.setCondicionIvaEmisor(CondicionIvaEmisor.MONOTRIBUTO);
        facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.emitirFactura(7L);

        verify(afip).obtenerUltimoComprobante(1, 11);
        assertTrue(solicitudEnviada().desgloseIva().isEmpty());
        assertEquals(11, solicitudEnviada().tipoComprobante());
        assertEquals("C", r.tipoComprobante());
    }

    @Test
    void emitirConAfipCaidaDejaErrorAfipConMotivoSinPisarObservacionesNiFechaDeEmision() throws Exception {
        when(afip.obtenerCotizacion(any())).thenReturn(BigDecimal.ONE);
        when(afip.obtenerUltimoComprobante(anyInt(), anyInt())).thenThrow(new AfipComunicacionException("ARCA no respondió a tiempo.", new RuntimeException()));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setObservaciones("obs original");

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.ERROR_AFIP, r.estado());
        assertEquals("ARCA no respondió a tiempo.", r.motivoRechazo());
        assertEquals("obs original", f.getObservaciones());
        assertNull(f.getNumero());
        assertNull(f.getCae());
        assertNull(f.getFechaEmision());
        assertNull(f.getTipoComprobante());
    }

    @Test
    void emitirDesdeErrorAfipReintentaYLimpiaElMotivo() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.ERROR_AFIP);
        f.setMotivoRechazo("fallo anterior");

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.EMITIDA, r.estado());
        assertNull(f.getMotivoRechazo());
    }

    @Test
    void emitirEmitidaOAnuladaDa409SinTocarAfip() {
        facturaExistente(EstadoFactura.EMITIDA);
        assertThrows(ReglaNegocioException.class, () -> service.emitirFactura(7L));
        facturaExistente(EstadoFactura.ANULADA);
        assertThrows(ReglaNegocioException.class, () -> service.emitirFactura(7L));
        verifyNoInteractions(afip);
    }

    @Test
    void rechazoDeArcaGuardaCodigosYMensajesYLiberaElNumero() throws Exception {
        afipResponde();
        when(afip.solicitarCae(any(SolicitudCae.class))).thenReturn(new WsfeRespuesta("R", null, null, List.of(),
                List.of(new WsfeRespuesta.Mensaje("10016", "Numero invalido"), new WsfeRespuesta.Mensaje("10015", "DocNro"))));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.ERROR_AFIP, r.estado());
        assertEquals("[10016] Numero invalido\n[10015] DocNro", f.getMotivoRechazo());
        assertNull(f.getNumero());
        assertNull(f.getCae());
    }

    @Test
    void siSeCortaLaComunicacionElNumeroQuedaReservadoParaRecuperarlo() throws Exception {
        afipResponde();
        when(afip.solicitarCae(any(SolicitudCae.class))).thenThrow(new AfipComunicacionException("timeout", new RuntimeException()));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.ERROR_AFIP, r.estado());
        assertEquals(11L, f.getNumero());
        assertTrue(f.getMotivoRechazo().contains("timeout"));
        assertTrue(f.getMotivoRechazo().contains("no se sabe si el comprobante fue autorizado"), f.getMotivoRechazo());
        assertNull(f.getCae());
    }

    @Test
    void alReintentarRecuperaElCaeSiArcaYaAutorizoElNumeroReservado() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.ERROR_AFIP);
        f.setNumero(11L);
        when(afip.consultarComprobante(1, 1, 11L)).thenReturn(Optional.of(new ComprobanteAutorizado(
                "CAE-RECUPERADO", "20261231", "20261028", bd("121.00"), new DocumentoReceptor(80, 20281234567L))));

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.EMITIDA, r.estado());
        assertEquals("CAE-RECUPERADO", f.getCae());
        assertEquals(11L, f.getNumero());
        assertEquals(LocalDate.of(2026, 10, 28), f.getFechaEmision());
        verify(afip, never()).solicitarCae(any(SolicitudCae.class));
        verify(afip, never()).obtenerUltimoComprobante(anyInt(), anyInt());
    }

    @Test
    void siElComprobanteDeArcaNoCoincideSeEmiteUnoNuevo() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.ERROR_AFIP);
        f.setNumero(11L);
        when(afip.consultarComprobante(1, 1, 11L)).thenReturn(Optional.of(new ComprobanteAutorizado(
                "CAE-OTRO", "20261231", "20261028", bd("999.00"), new DocumentoReceptor(80, 20281234567L))));

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.EMITIDA, r.estado());
        assertEquals("CAE-123", f.getCae());
        verify(afip).solicitarCae(any(SolicitudCae.class));
    }

    @Test
    void clienteConDniSeEnviaComoDocTipo96() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setCliente(Cliente.builder().id(12L).denominacion("Pepe").cuitCuil("30123456").condicionIva(ivaConsumidorFinal).build());
        f.setCondicionIva(ivaConsumidorFinal);

        service.emitirFactura(7L);

        assertEquals(new DocumentoReceptor(96, 30123456L), solicitudEnviada().documento());
        assertEquals(5, solicitudEnviada().condicionIvaReceptorId());
    }

    @Test
    void facturaSinClienteSeEnviaComoConsumidorFinalSinDocumento() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setCliente(null);
        f.setCondicionIva(ivaConsumidorFinal);

        service.emitirFactura(7L);

        assertEquals(DocumentoReceptor.CONSUMIDOR_FINAL, solicitudEnviada().documento());
    }

    @Test
    void consumidorFinalSinIdentificarPorEncimaDelTopeNoLlegaAArca() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setCliente(null);
        f.setCondicionIva(ivaConsumidorFinal);
        f.setImporteTotal(bd("10000000.00"));

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.ERROR_AFIP, r.estado());
        assertTrue(r.motivoRechazo().contains("Consumidor Final"));
        verify(afip, never()).solicitarCae(any(SolicitudCae.class));
    }

    @Test
    void unErrorInesperadoNuncaMuestraDetallesTecnicosAlUsuario() throws Exception {
        afipResponde();
        when(afip.obtenerUltimoComprobante(anyInt(), anyInt()))
                .thenThrow(new NullPointerException("Cannot invoke \"String.length()\" because \"x\" is null"));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.ERROR_AFIP, r.estado());
        assertEquals("No se pudo emitir la factura por un problema inesperado. Intentá de nuevo; "
                + "si el problema continúa, avisá al administrador.", f.getMotivoRechazo());
    }

    @Test
    void lasReglasDeEstadoHablanEnPalabrasDelUsuario() {
        facturaExistente(EstadoFactura.EMITIDA);
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> service.emitirFactura(7L));
        assertEquals("La factura ya está emitida y no se puede emitir de nuevo.", e.getMessage());
        facturaExistente(EstadoFactura.ANULADA);
        e = assertThrows(ReglaNegocioException.class, () -> service.emitirFactura(7L));
        assertEquals("La factura está anulada y no se puede emitir.", e.getMessage());
    }

    // ---------- obtener ----------

    @Test
    void obtenerDevuelveDetallesConElSnapshotDelRenglon() {
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        // el precio actual de la lista cambió después de crear la factura
        precioListaA.setPrecioVenta(bd("999.00"));
        f.getDetalles().get(0).setPrecioUnitario(bd("100.00"));

        FacturaDTO r = service.obtenerFactura(7L);

        assertEquals(100L, r.listaPrecioId());
        assertEquals(20L, r.condicionIvaId());
        assertEquals(EstadoFactura.PENDIENTE, r.estado());
        assertEquals(1, r.cantidadItems());
        FacturaDetalleDTO d = r.detalles().get(0);
        assertEquals(50L, d.articuloId());
        assertEquals(1, d.cantidad());
        assertImporte("100.00", d.precioUnitario());
        assertImporte("21", d.alicuotaPorcentaje());
        assertImporte("121.00", d.importeSubtotal());
    }

    @Test
    void obtenerDevuelveElPrecioDelArticuloEnCadaListaParaRecalcularAlCambiarDeLista() {
        facturaExistente(EstadoFactura.PENDIENTE);
        when(lpaRepo.findByArticuloIdIn(any())).thenReturn(List.of(precioListaA, precioListaB));

        FacturaDetalleDTO d = service.obtenerFactura(7L).detalles().get(0);

        assertEquals(2, d.precios().size());
        assertTrue(d.precios().contains(new PrecioListaDTO(100L, bd("100.00"))));
        assertTrue(d.precios().contains(new PrecioListaDTO(200L, bd("250.00"))));
    }

    // ---------- imprimir ----------

    private FacturaVenta emitida() {
        FacturaVenta f = facturaExistente(EstadoFactura.EMITIDA);
        f.setNumero(8L);
        f.setTipoComprobante(1);
        f.setFechaEmision(LocalDate.of(2026, 11, 18));
        f.setCae("86461089472534");
        f.setCaeFechaVencimiento(LocalDate.of(2026, 11, 28));
        f.setCotizacion(BigDecimal.ONE);
        f.setObservaciones("Entrega en sucursal");
        cliente.setDomicilio(com.utn.tp_spring_grupal.cliente.Domicilio.builder().nombreCalle("San Martín")
                .numeroCalle("123").build());
        return f;
    }

    @Test
    void datosDelComprobanteTraenEmisorReceptorRenglonesYCae() {
        emitida();

        ComprobantePdf c = service.datosComprobante(7L);

        assertEquals("Comercial Ejemplo SRL", c.emisor().razonSocial());
        assertEquals("20111111112", c.emisor().cuit());
        assertEquals("IVA Responsable Inscripto", c.emisor().condicionIva());
        assertEquals(TipoComprobante.A, c.tipo());
        assertEquals(1, c.puntoVenta());
        assertEquals(8L, c.numero());
        assertEquals(new DocumentoReceptor(80, 20281234567L), c.receptor().documento());
        assertEquals("Juan Perez", c.receptor().denominacion());
        assertEquals("Resp. Inscripto", c.receptor().condicionIva());
        assertEquals("San Martín 123", c.receptor().domicilio());
        assertEquals("PES", c.monedaCodigo());
        assertEquals(1, c.renglones().size());
        assertEquals("ART-1", c.renglones().get(0).codigo());
        assertImporte("121.00", c.importeTotal());
        assertEquals("86461089472534", c.cae());
        assertEquals(LocalDate.of(2026, 11, 28), c.caeVencimiento());
        assertEquals(false, c.anulada());
    }

    @Test
    void elQrDelComprobanteCodificaSusDatosSegunArca() {
        emitida();

        String url = service.datosComprobante(7L).qrUrl();

        assertTrue(url.startsWith("https://www.arca.gob.ar/fe/qr/?p="));
        String json = new String(java.util.Base64.getDecoder().decode(url.substring(url.indexOf("?p=") + 3)),
                java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("{\"ver\":1,\"fecha\":\"2026-11-18\",\"cuit\":20111111112,\"ptoVta\":1,\"tipoCmp\":1,"
                + "\"nroCmp\":8,\"importe\":121,\"moneda\":\"PES\",\"ctz\":1,\"tipoDocRec\":80,"
                + "\"nroDocRec\":20281234567,\"tipoCodAut\":\"E\",\"codAut\":86461089472534}", json);
    }

    @Test
    void facturaSinClienteSeImprimeComoConsumidorFinalSinDocumento() {
        FacturaVenta f = emitida();
        f.setCliente(null);
        f.setCondicionIva(ivaConsumidorFinal);
        f.setTipoComprobante(6);

        ComprobantePdf c = service.datosComprobante(7L);

        assertEquals(DocumentoReceptor.CONSUMIDOR_FINAL, c.receptor().documento());
        assertEquals("Consumidor Final", c.receptor().denominacion());
        assertNull(c.receptor().domicilio());
        assertEquals(TipoComprobante.B, c.tipo());
    }

    @Test
    void unaFacturaAnuladaSeImprimeConLaMarcaDeAnulada() {
        emitida().setEstado(EstadoFactura.ANULADA);
        assertTrue(service.datosComprobante(7L).anulada());
    }

    @Test
    void soloSeImprimeUnaFacturaConCae() {
        facturaExistente(EstadoFactura.PENDIENTE);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.datosComprobante(7L));
        assertTrue(ex.getMessage().contains("emitida"));

        facturaExistente(EstadoFactura.ERROR_AFIP);
        assertThrows(ReglaNegocioException.class, () -> service.datosComprobante(7L));
    }

    // ---------- listado paginado ----------

    @Test
    @SuppressWarnings("unchecked")
    void listadoPaginaConTotalesYSinDetalles() {
        FacturaVenta f1 = facturaExistente(EstadoFactura.PENDIENTE);
        FacturaVenta f2 = facturaExistente(8L, EstadoFactura.ERROR_AFIP);
        when(facturaRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(f1, f2), inv.getArgument(1), 25));
        when(facturaRepo.contarRenglones(any())).thenReturn(List.of(new Object[] { 7L, 3L }, new Object[] { 8L, 1L }));

        PageResponse<FacturaDTO> r = service.buscarFacturas(FacturaFiltro.VACIO, 1, 2);

        assertEquals(2, r.contenido().size());
        assertEquals(1, r.pagina());
        assertEquals(2, r.tamanio());
        assertEquals(25, r.totalElementos());
        assertEquals(13, r.totalPaginas());
        assertNull(r.contenido().get(0).detalles());
        assertEquals(3, r.contenido().get(0).cantidadItems());
        assertEquals(1, r.contenido().get(1).cantidadItems());
        assertEquals("Juan Perez", r.contenido().get(0).clienteDenominacion());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listadoOrdenaPorFechaDeCreacionDescendenteYLimitaElTamanio() {
        when(facturaRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<FacturaVenta>(List.of(), inv.getArgument(1), 0));

        service.buscarFacturas(FacturaFiltro.VACIO, -3, 1000);

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(facturaRepo).findAll(any(Specification.class), cap.capture());
        assertEquals(0, cap.getValue().getPageNumber());
        assertEquals(100, cap.getValue().getPageSize());
        assertEquals("DESC", cap.getValue().getSort().getOrderFor("fechaAlta").getDirection().name());
    }

    // ---------- cotizacion ----------

    @Test
    void emitirEnPesosGuardaCotizacionUnoYEnviaPesos() throws Exception {
        afipResponde();
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(0, BigDecimal.ONE.compareTo(f.getCotizacion()));
        assertEquals(0, BigDecimal.ONE.compareTo(r.cotizacion()));
        verify(afip).obtenerCotizacion("PES");
    }

    @Test
    void emitirEnDolaresEnviaMonIdYCotizacionRealesYLaGuarda() throws Exception {
        afipResponde();
        when(afip.obtenerCotizacion("DOL")).thenReturn(new BigDecimal("1174"));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setTipoMoneda(TipoMoneda.builder().id(41L).codigoAfip("DOL").denominacion("Dolar").simbolo("US$").build());

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals("DOL", solicitudEnviada().monId());
        assertEquals(new BigDecimal("1174"), solicitudEnviada().monCotiz());
        assertEquals(new BigDecimal("1174"), f.getCotizacion());
        assertEquals(new BigDecimal("1174"), r.cotizacion());
        assertEquals(EstadoFactura.EMITIDA, r.estado());
    }

    @Test
    void siFallaLaCotizacionLaFacturaQuedaEnErrorAfipSinCotizacion() throws Exception {
        afipResponde();
        when(afip.obtenerCotizacion("DOL")).thenThrow(new AfipException("No se pudo obtener la cotización en ARCA: [602] Sin Resultados"));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);
        f.setTipoMoneda(TipoMoneda.builder().id(41L).codigoAfip("DOL").denominacion("Dolar").simbolo("US$").build());
        f.setCotizacion(new BigDecimal("999"));

        FacturaDTO r = service.emitirFactura(7L);

        assertEquals(EstadoFactura.ERROR_AFIP, r.estado());
        assertNull(f.getCotizacion());
        assertNull(r.cotizacion());
        assertEquals("No se pudo obtener la cotización en ARCA: [602] Sin Resultados", r.motivoRechazo());
    }

    @Test
    void siFallaAfipTrasObtenerLaCotizacionSeLimpia() throws Exception {
        afipResponde();
        when(afip.solicitarCae(any(SolicitudCae.class)))
                .thenReturn(new WsfeRespuesta("R", null, null, List.of(), List.of(new WsfeRespuesta.Mensaje("10016", "rechazo"))));
        FacturaVenta f = facturaExistente(EstadoFactura.PENDIENTE);

        service.emitirFactura(7L);

        assertNull(f.getCotizacion());
    }
}
