package com.utn.tp_spring_grupal.factura;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.utn.tp_spring_grupal.afip.AfipAutenticacionException;
import com.utn.tp_spring_grupal.afip.AfipComunicacionException;
import com.utn.tp_spring_grupal.afip.AfipException;
import com.utn.tp_spring_grupal.afip.AfipWsfeService;
import com.utn.tp_spring_grupal.afip.ComprobanteAutorizado;
import com.utn.tp_spring_grupal.afip.DocumentoReceptor;
import com.utn.tp_spring_grupal.afip.ReglaConsumidorFinal;
import com.utn.tp_spring_grupal.afip.SolicitudCae;
import com.utn.tp_spring_grupal.afip.WsfeRespuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.cliente.ClienteRepository;
import com.utn.tp_spring_grupal.common.Busqueda;
import com.utn.tp_spring_grupal.common.PageResponse;
import com.utn.tp_spring_grupal.common.ReglaNegocioException;
import com.utn.tp_spring_grupal.common.ValidacionException;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;
import com.utn.tp_spring_grupal.emisor.EmisorProperties;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.Alicuota;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.ImportesRenglon;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.LineaIva;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.ResumenAlicuota;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.Totales;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecio;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticulo;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticuloRepository;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioRepository;
import com.utn.tp_spring_grupal.puntoventa.PuntoVenta;
import com.utn.tp_spring_grupal.puntoventa.PuntoVentaRepository;
import com.utn.tp_spring_grupal.tipomoneda.TipoMoneda;
import com.utn.tp_spring_grupal.tipomoneda.TipoMonedaRepository;
import com.utn.tp_spring_grupal.usuario.Usuario;
import com.utn.tp_spring_grupal.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);
    /** Zona en la que ARCA fecha los comprobantes. */
    private static final ZoneId ZONA_AFIP = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String MENSAJE_ERROR_INESPERADO = "No se pudo emitir la factura por un problema inesperado. "
            + "Intentá de nuevo; si el problema continúa, avisá al administrador.";
    private static final DateTimeFormatter FECHA_AFIP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FECHA_REPORTE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final FacturaVentaRepository facturaVentaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AfipWsfeService afipWsfeService;
    private final EmisorProperties emisor;
    private final TransactionTemplate tx;

    public FacturaService(FacturaVentaRepository facturaVentaRepository,
                          ClienteRepository clienteRepository,
                          ListaPrecioRepository listaPrecioRepository,
                          ListaPrecioArticuloRepository listaPrecioArticuloRepository,
                          PuntoVentaRepository puntoVentaRepository,
                          TipoMonedaRepository tipoMonedaRepository,
                          UsuarioRepository usuarioRepository,
                          AfipWsfeService afipWsfeService,
                          EmisorProperties emisor,
                          PlatformTransactionManager transactionManager) {
        this.facturaVentaRepository = facturaVentaRepository;
        this.clienteRepository = clienteRepository;
        this.listaPrecioRepository = listaPrecioRepository;
        this.listaPrecioArticuloRepository = listaPrecioArticuloRepository;
        this.puntoVentaRepository = puntoVentaRepository;
        this.tipoMonedaRepository = tipoMonedaRepository;
        this.usuarioRepository = usuarioRepository;
        this.afipWsfeService = afipWsfeService;
        this.emisor = emisor;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------ crear

    /**
     * Crea la factura en estado PENDIENTE (sin número, CAE ni fecha de emisión). La condición IVA se copia
     * del cliente (snapshot). La emisión en AFIP es una acción aparte.
     */
    public FacturaDTO crearFactura(FacturaGuardarDTO dto) {
        requerido(dto.clienteId(), "El cliente es obligatorio");
        requerido(dto.listaPrecioId(), "La lista de precios es obligatoria");
        requerido(dto.puntoVentaId(), "El punto de venta es obligatorio");
        requerido(dto.tipoMonedaId(), "El tipo de moneda es obligatorio");
        validarRenglones(dto.detalles());

        Usuario usuarioActual = usuarioRepository.findById(1L)
                .orElseThrow(() -> new NullPointerException("No se encontró el usuario del sistema"));
        TipoMoneda tipoMoneda = buscarMoneda(dto.tipoMonedaId());
        Cliente cliente = buscarCliente(dto.clienteId());
        ListaPrecio listaPrecio = buscarLista(dto.listaPrecioId());
        PuntoVenta puntoVenta = buscarPuntoVenta(dto.puntoVentaId());

        Date ahora = new Date();
        FacturaVenta factura = FacturaVenta.builder()
                .numero(null)
                .fechaEmision(null)
                .observaciones(dto.observaciones())
                .estado(EstadoFactura.PENDIENTE)

                // Relaciones
                .cliente(cliente)
                .listaPrecio(listaPrecio)
                .condicionIva(cliente.getCondicionIva())
                .puntoVenta(puntoVenta)
                .tipoMoneda(tipoMoneda)

                // Auditoría (heredada de AuditoriaApp); fechaAlta es la fecha de creación
                .fechaAlta(ahora)
                .fechaModificacion(ahora)
                .usuarioCarga(usuarioActual)
                .usuarioModificacion(usuarioActual)

                .detalles(new ArrayList<>())
                .build();

        recalcular(factura, dto.detalles());

        factura = facturaVentaRepository.save(factura);
        return aDTO(factura, factura.getDetalles().size(), true);
    }

    // ------------------------------------------------------------- actualizar

    /** Edición parcial. Solo permitida en PENDIENTE y ERROR_AFIP; en otro estado responde 409. */
    public FacturaDTO actualizarFactura(Long id, FacturaGuardarDTO dto) {
        FacturaVenta factura = buscarOFallar(id);

        if (!factura.getEstado().esEditable()) {
            throw new ReglaNegocioException("La factura está " + factura.getEstado().etiqueta() + " y no se puede editar.");
        }

        boolean cambiaLista = dto.listaPrecioId() != null
                && !dto.listaPrecioId().equals(factura.getListaPrecio().getId());
        if (cambiaLista && dto.detalles() == null) {
            throw new ValidacionException("Al cambiar la lista de precios hay que enviar los renglones nuevamente.");
        }
        if (dto.detalles() != null) {
            validarRenglones(dto.detalles());
        }

        if (dto.tipoMonedaId() != null) {
            factura.setTipoMoneda(buscarMoneda(dto.tipoMonedaId()));
        }
        if (dto.clienteId() != null) {
            Cliente cliente = buscarCliente(dto.clienteId());
            factura.setCliente(cliente);
            factura.setCondicionIva(cliente.getCondicionIva());
        }
        if (dto.puntoVentaId() != null) {
            factura.setPuntoVenta(buscarPuntoVenta(dto.puntoVentaId()));
        }
        if (cambiaLista) {
            factura.setListaPrecio(buscarLista(dto.listaPrecioId()));
        }
        if (dto.observaciones() != null) {
            factura.setObservaciones(dto.observaciones());
        }

        factura.setFechaModificacion(new Date());

        if (dto.detalles() != null) {
            factura.getDetalles().clear();
            recalcular(factura, dto.detalles());
        }

        facturaVentaRepository.save(factura);
        return aDTO(factura, factura.getDetalles().size(), true);
    }

    // ----------------------------------------------------------------- emitir

    /** Datos de la factura leídos en una transacción corta; con esto se llama a ARCA sin transacción abierta. */
    private record DatosEmision(Long id, int puntoVenta, String monId, int condicionIvaCodigo, String cuitCuil,
                                BigDecimal total, BigDecimal neto, BigDecimal iva, List<ResumenAlicuota> desgloseIva,
                                Long numeroReservado) {
    }

    /**
     * Pide el CAE a AFIP. Éxito → EMITIDA con número, CAE y fecha de emisión (la enviada a ARCA); rechazo de ARCA →
     * ERROR_AFIP con el código y mensaje de ARCA en motivoRechazo (editable y reintentable). Solo PENDIENTE / ERROR_AFIP.
     *
     * <p>La llamada a ARCA corre SIN transacción abierta. Antes de pedir el CAE el número se reserva en la factura
     * (transacción corta): si la respuesta se pierde o falla el guardado local, el reintento consulta ese número con
     * FECompConsultar y recupera el CAE en vez de emitir un comprobante nuevo.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FacturaDTO emitirFactura(Long id) {
        DatosEmision datos = tx.execute(s -> leerDatosDeEmision(id));

        try {
            TipoComprobante tipoComprobante = TipoComprobanteResolver
                    .resolver(emisor.getCondicionIvaEmisor(), datos.condicionIvaCodigo());
            DocumentoReceptor documento = DocumentoReceptor.deCuitCuil(datos.cuitCuil());
            BigDecimal cotizacion = afipWsfeService.obtenerCotizacion(datos.monId());
            ReglaConsumidorFinal.validar(documento, datos.total().multiply(cotizacion),
                    emisor.getConsumidorFinalMontoMaximo());
            LocalDate fechaComprobante = LocalDate.now(ZONA_AFIP);
            int tipo = tipoComprobante.getCodigoAfip();

            if (datos.numeroReservado() != null) {
                SolicitudCae previa = solicitud(datos, tipo, datos.numeroReservado(), fechaComprobante, documento,
                        cotizacion);
                Optional<ComprobanteAutorizado> yaAutorizado =
                        afipWsfeService.consultarComprobante(datos.puntoVenta(), tipo, datos.numeroReservado());
                if (yaAutorizado.isPresent() && yaAutorizado.get().coincideCon(previa)) {
                    log.warn("Factura {}: ARCA ya había autorizado el comprobante {}; se recupera su CAE",
                            id, datos.numeroReservado());
                    guardarEmitida(id, tipo, cotizacion, yaAutorizado.get().cae(),
                            yaAutorizado.get().caeFchVto(), fechaAutorizada(yaAutorizado.get(), fechaComprobante),
                            datos.numeroReservado());
                    return leerDTO(id);
                }
            }

            long nuevoNro = afipWsfeService.obtenerUltimoComprobante(datos.puntoVenta(), tipo) + 1;
            reservarNumero(id, nuevoNro);
            SolicitudCae solicitud = solicitud(datos, tipo, nuevoNro, fechaComprobante, documento, cotizacion);

            WsfeRespuesta respuesta;
            try {
                respuesta = afipWsfeService.solicitarCae(solicitud);
            } catch (AfipComunicacionException e) {
                // Resultado indeterminado: ARCA pudo haber autorizado. Se conserva el número para recuperarlo.
                log.warn("Factura {}: resultado indeterminado del comprobante {}", id, nuevoNro);
                registrarError(id, e.getMessage() + " Por ahora no se sabe si el comprobante fue autorizado: "
                        + "al volver a emitir, el sistema lo verifica con ARCA antes de generar uno nuevo.", false);
                return leerDTO(id);
            }

            if (respuesta.aprobada()) {
                guardarEmitidaConReintento(id, tipo, cotizacion, respuesta.cae(), respuesta.caeFchVto(),
                        fechaComprobante, nuevoNro);
            } else {
                registrarError(id, respuesta.motivoRechazo(), true);
            }
        } catch (GuardadoPostAutorizacionException e) {
            throw e;
        } catch (AfipComunicacionException | AfipAutenticacionException | AfipException
                 | IllegalArgumentException e) {
            // Falla antes de obtener una respuesta de ARCA: el número reservado antes (si hay) no se toca.
            // Estas excepciones traen un mensaje pensado para el usuario.
            log.warn("Factura {}: no se pudo emitir: {}", id, e.getMessage());
            registrarError(id, e.getMessage(), false);
        } catch (RuntimeException e) {
            // Cualquier otra falla es un defecto: el detalle va al log y el usuario ve un mensaje genérico.
            log.error("Factura {}: error inesperado al emitir", id, e);
            registrarError(id, MENSAJE_ERROR_INESPERADO, false);
        }
        return leerDTO(id);
    }

    /** ARCA autorizó el comprobante pero no se pudo guardar el resultado: el número queda reservado para recuperarlo. */
    private static class GuardadoPostAutorizacionException extends RuntimeException {
        GuardadoPostAutorizacionException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    private static LocalDate fechaAutorizada(ComprobanteAutorizado c, LocalDate porDefecto) {
        return c.cbteFch() != null ? LocalDate.parse(c.cbteFch(), FECHA_AFIP) : porDefecto;
    }

    private DatosEmision leerDatosDeEmision(Long id) {
        FacturaVenta factura = buscarOFallar(id);
        if (!factura.getEstado().esEditable()) {
            throw new ReglaNegocioException(factura.getEstado() == EstadoFactura.EMITIDA
                    ? "La factura ya está emitida y no se puede emitir de nuevo."
                    : "La factura está " + factura.getEstado().etiqueta() + " y no se puede emitir.");
        }
        Cliente cliente = factura.getCliente();
        List<ResumenAlicuota> desglose = emisor.discriminaIva()
                ? CalculadoraIva.resumirPorAlicuota(lineasIva(factura))
                : List.of();
        // Los importes salen de los renglones ya persistidos (snapshot), no de dividir el total por 1,21.
        return new DatosEmision(id, factura.getPuntoVenta().getNumero(), factura.getTipoMoneda().getCodigoAfip(),
                factura.getCondicionIva().getCodigoAfip(), cliente != null ? cliente.getCuitCuil() : null,
                factura.getImporteTotal(), factura.getImporteNeto(), factura.getImporteIva(), desglose,
                factura.getNumero());
    }

    private SolicitudCae solicitud(DatosEmision d, int tipo, long numero, LocalDate fecha, DocumentoReceptor documento,
                                   BigDecimal cotizacion) {
        return new SolicitudCae(d.puntoVenta(), tipo, numero, fecha, d.total(), d.neto(), d.iva(), documento,
                d.monId(), cotizacion, d.condicionIvaCodigo(), d.desgloseIva());
    }

    private void reservarNumero(Long id, long numero) {
        tx.executeWithoutResult(s -> {
            FacturaVenta f = buscarOFallar(id);
            f.setNumero(numero);
            facturaVentaRepository.save(f);
        });
    }

    private void guardarEmitidaConReintento(Long id, int tipo, BigDecimal cotizacion, String cae, String vto,
                                            LocalDate fecha, long numero) {
        RuntimeException ultimo = null;
        for (int intento = 1; intento <= 3; intento++) {
            try {
                guardarEmitida(id, tipo, cotizacion, cae, vto, fecha, numero);
                return;
            } catch (RuntimeException e) {
                ultimo = e;
                log.error("Factura {}: ARCA autorizó el comprobante {} (CAE {}) pero falló el guardado (intento {})",
                        id, numero, cae, intento, e);
            }
        }
        throw new GuardadoPostAutorizacionException("ARCA autorizó el comprobante " + numero + " (CAE " + cae
                + ") pero no se pudo guardar. Volvé a emitir la factura para recuperarlo.", ultimo);
    }

    private void guardarEmitida(Long id, int tipo, BigDecimal cotizacion, String cae, String vto, LocalDate fecha,
                                long numero) {
        tx.executeWithoutResult(s -> {
            FacturaVenta f = buscarOFallar(id);
            f.setNumero(numero);
            f.setCotizacion(cotizacion);
            f.setCae(cae);
            f.setCaeFechaVencimiento(LocalDate.parse(vto, FECHA_AFIP));
            f.setResultadoAfip("A");
            f.setMotivoRechazo(null);
            f.setTipoComprobante(tipo);
            f.setEstado(EstadoFactura.EMITIDA);
            f.setFechaEmision(fecha);
            f.setFechaModificacion(new Date());
            facturaVentaRepository.save(f);
        });
    }

    /** La factura queda ERROR_AFIP (editable, reintentable); {@code liberarNumero} borra el número reservado. */
    private void registrarError(Long id, String motivo, boolean liberarNumero) {
        tx.executeWithoutResult(s -> {
            FacturaVenta f = buscarOFallar(id);
            if (liberarNumero) {
                f.setNumero(null);
            }
            f.setCae(null);
            f.setCotizacion(null);
            f.setCaeFechaVencimiento(null);
            f.setFechaEmision(null);
            f.setTipoComprobante(null);
            f.setResultadoAfip("R");
            f.setMotivoRechazo(motivo);
            f.setEstado(EstadoFactura.ERROR_AFIP);
            f.setFechaModificacion(new Date());
            facturaVentaRepository.save(f);
        });
    }

    private FacturaDTO leerDTO(Long id) {
        return tx.execute(s -> {
            FacturaVenta f = buscarOFallar(id);
            return aDTO(f, f.getDetalles().size(), true);
        });
    }

    // ----------------------------------------------------------------- anular

    /**
     * Eliminación lógica (anulación) de una factura PENDIENTE o ERROR_AFIP. Una EMITIDA ya tiene CAE y no se
     * anula (corresponde una Nota de Crédito, fuera de alcance): 409.
     */
    public void anularFactura(Long id) {
        FacturaVenta factura = buscarOFallar(id);

        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ReglaNegocioException("La factura ya está anulada.");
        }
        if (factura.getEstado() == EstadoFactura.EMITIDA) {
            throw new ReglaNegocioException(
                    "Una factura EMITIDA no se puede anular: corresponde emitir una Nota de Crédito.");
        }

        factura.setEstado(EstadoFactura.ANULADA);
        factura.setFechaAnulacion(new Date());
        factura.setFechaModificacion(new Date());

        facturaVentaRepository.save(factura);
    }

    // ---------------------------------------------------------------- lecturas

    /**
     * Listado paginado, ordenado por fecha de creación descendente. Las fechas son inclusivas y filtran por
     * fecha de creación. El DTO del listado no trae renglones (detalles = null).
     */
    @Transactional(readOnly = true)
    public PageResponse<FacturaDTO> buscarFacturas(LocalDate fechaDesde, LocalDate fechaHasta, EstadoFactura estado,
                                                   BigDecimal montoMinimo, int page, int size) {
        Specification<FacturaVenta> spec = FacturaSpecs.filtrar(fechaDesde, fechaHasta, estado, montoMinimo);
        Pageable pageable = Busqueda.pageable(page, size, ORDEN_LISTADO);
        Page<FacturaVenta> pagina = facturaVentaRepository.findAll(spec, pageable);

        Map<Long, Long> items = contarRenglones(pagina.getContent());
        return PageResponse.de(pagina, f -> aDTO(f, items.getOrDefault(f.getId(), 0L), false));
    }

    /** Mismos filtros que el listado, sin paginar (PDF / TXT). */
    @Transactional(readOnly = true)
    public List<FacturaDTO> buscarFacturasSinPaginar(LocalDate fechaDesde, LocalDate fechaHasta,
                                                     EstadoFactura estado, BigDecimal montoMinimo) {
        Specification<FacturaVenta> spec = FacturaSpecs.filtrar(fechaDesde, fechaHasta, estado, montoMinimo);
        List<FacturaVenta> facturas = facturaVentaRepository.findAll(spec, ORDEN_LISTADO);
        Map<Long, Long> items = contarRenglones(facturas);
        return facturas.stream().map(f -> aDTO(f, items.getOrDefault(f.getId(), 0L), false)).toList();
    }

    private static final Sort ORDEN_LISTADO = Sort.by(Sort.Direction.DESC, "fechaAlta", "id");

    // Detalle completo para precargar la edición (cabecera + renglones).
    @Transactional(readOnly = true)
    public FacturaDTO obtenerFactura(Long id) {
        FacturaVenta factura = buscarOFallar(id);
        return aDTO(factura, factura.getDetalles().size(), true);
    }

    // ---------------------------------------------------------------- helpers

    private FacturaVenta buscarOFallar(Long id) {
        return facturaVentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la factura. Es posible que ya no exista."));
    }

    private TipoMoneda buscarMoneda(Long id) {
        return tipoMonedaRepository.findById(id)
                .orElseThrow(() -> new NullPointerException("No se encontró la moneda seleccionada."));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NullPointerException("No se encontró el cliente seleccionado."));
    }

    private ListaPrecio buscarLista(Long id) {
        return listaPrecioRepository.findById(id)
                .orElseThrow(() -> new NullPointerException("No se encontró la lista de precios seleccionada."));
    }

    private PuntoVenta buscarPuntoVenta(Long id) {
        return puntoVentaRepository.findById(id)
                .orElseThrow(() -> new NullPointerException("No se encontró el punto de venta seleccionado."));
    }

    private void requerido(Object valor, String mensaje) {
        if (valor == null) {
            throw new ValidacionException(mensaje);
        }
    }

    /** Al menos un renglón, cantidad entera >= 1 y bonificación entre 0 y 100. */
    private void validarRenglones(List<FacturaDetalleDTO> renglones) {
        if (renglones == null || renglones.isEmpty()) {
            throw new ValidacionException("La factura debe tener al menos un renglón.");
        }
        for (FacturaDetalleDTO r : renglones) {
            if (r.articuloId() == null) {
                throw new ValidacionException("Cada renglón debe indicar un artículo.");
            }
            if (r.cantidad() == null || r.cantidad() < 1) {
                throw new ValidacionException("La cantidad de cada renglón debe ser un entero mayor o igual a 1.");
            }
            BigDecimal bonificacion = bonificacion(r);
            if (bonificacion.signum() < 0 || bonificacion.compareTo(CIEN) > 0) {
                throw new ValidacionException("La bonificación debe estar entre 0 y 100.");
            }
        }
    }

    private static BigDecimal bonificacion(FacturaDetalleDTO r) {
        return r.porcentajeBonificacion() != null ? r.porcentajeBonificacion() : BigDecimal.ZERO;
    }

    /** Carga los renglones, recalcula neto/IVA/total de la cabecera y el saldo. */
    private void recalcular(FacturaVenta factura, List<FacturaDetalleDTO> renglones) {
        List<ImportesRenglon> importes = cargarRenglones(factura, factura.getListaPrecio().getId(), renglones);
        Totales totales = CalculadoraIva.totalizar(importes);
        factura.setImporteNeto(totales.neto());
        factura.setImporteIva(totales.iva());
        factura.setImporteTotal(totales.total());
        factura.setImporteSaldo(totales.total().subtract(factura.getImporteCobrado()));
    }

    /**
     * Agrega los renglones tomando el precio (neto) de la lista elegida y la alícuota del artículo, que se
     * guarda como snapshot. Si el emisor no discrimina IVA la alícuota aplicada es 0 %.
     */
    private List<ImportesRenglon> cargarRenglones(FacturaVenta factura, Long listaPrecioId,
                                                  List<FacturaDetalleDTO> renglones) {
        List<ImportesRenglon> importes = new ArrayList<>();
        for (FacturaDetalleDTO dtoDetalle : renglones) {
            ListaPrecioArticulo listaPrecioArticulo = listaPrecioArticuloRepository
                    .findByListaPrecioIdAndArticuloId(listaPrecioId, dtoDetalle.articuloId())
                    .orElseThrow(() -> new NullPointerException("No se encontró precio para el artículo id "
                            + dtoDetalle.articuloId() + " en la lista de precios " + listaPrecioId));

            BigDecimal precioUnitario = listaPrecioArticulo.getPrecioVenta();
            int cantidad = dtoDetalle.cantidad();
            BigDecimal bonificacion = bonificacion(dtoDetalle);
            Alicuota alicuota = CalculadoraIva.alicuotaAplicable(emisor.discriminaIva(),
                    listaPrecioArticulo.getArticulo().getAlicuotaIva().getCodigoAfip(),
                    listaPrecioArticulo.getArticulo().getAlicuotaIva().getPorcentaje());

            ImportesRenglon importe = CalculadoraIva.calcularRenglon(precioUnitario, cantidad, bonificacion,
                    alicuota.porcentaje());
            importes.add(importe);

            String descripcion = dtoDetalle.descripcion() != null && !dtoDetalle.descripcion().isBlank()
                    ? dtoDetalle.descripcion()
                    : listaPrecioArticulo.getArticulo().getDenominacion();

            factura.getDetalles().add(FacturaVentaDetalle.builder()
                    .descripcion(descripcion)
                    .cantidad(cantidad)
                    .precioUnitario(precioUnitario)
                    .porcentajeBonificacion(bonificacion)
                    .alicuotaPorcentaje(alicuota.porcentaje())
                    .alicuotaCodigoAfip(alicuota.codigoAfip())
                    .importeNeto(importe.neto())
                    .importeIva(importe.iva())
                    .importeSubtotal(importe.subtotal())
                    .listaPrecioArticulo(listaPrecioArticulo)
                    .factura(factura)
                    .build());
        }
        return importes;
    }

    private List<LineaIva> lineasIva(FacturaVenta factura) {
        return factura.getDetalles().stream()
                .map(d -> new LineaIva(d.getAlicuotaCodigoAfip(), d.getAlicuotaPorcentaje(),
                        d.getImporteNeto(), d.getImporteIva()))
                .toList();
    }

    /** Cantidad de renglones de cada factura de la lista, en una sola consulta. */
    private Map<Long, Long> contarRenglones(Collection<FacturaVenta> facturas) {
        Map<Long, Long> items = new HashMap<>();
        if (facturas.isEmpty()) {
            return items;
        }
        List<Long> ids = facturas.stream().map(FacturaVenta::getId).toList();
        for (Object[] fila : facturaVentaRepository.contarRenglones(ids)) {
            items.put((Long) fila[0], (Long) fila[1]);
        }
        return items;
    }

    /** Tipo de comprobante: el guardado al emitir, o el calculado (emisor x condición IVA del receptor). */
    private TipoComprobante tipoComprobante(FacturaVenta f) {
        if (f.getTipoComprobante() != null) {
            return TipoComprobante.deCodigoAfip(f.getTipoComprobante());
        }
        return TipoComprobanteResolver.resolver(emisor.getCondicionIvaEmisor(), f.getCondicionIva().getCodigoAfip());
    }

    private FacturaDTO aDTO(FacturaVenta f, long cantidadItems, boolean conDetalles) {
        Cliente cliente = f.getCliente();
        return new FacturaDTO(
                f.getId(),
                f.getNumero(),
                f.getEstado(),
                tipoComprobante(f).getLetra(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(f.getFechaAlta().getTime()), ZoneId.systemDefault()),
                f.getFechaEmision(),
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getDenominacion() : "Consumidor Final",
                f.getListaPrecio().getId(),
                f.getListaPrecio().getDenominacion(),
                f.getCondicionIva().getId(),
                f.getCondicionIva().getDenominacion(),
                f.getPuntoVenta().getId(),
                f.getPuntoVenta().getDescripcion(),
                f.getTipoMoneda().getId(),
                f.getCotizacion(),
                f.getImporteNeto(),
                f.getImporteIva(),
                f.getImporteTotal(),
                f.getCae(),
                f.getCaeFechaVencimiento(),
                f.getMotivoRechazo(),
                f.getObservaciones(),
                cantidadItems,
                conDetalles ? detallesDTO(f) : null);
    }

    private List<FacturaDetalleDTO> detallesDTO(FacturaVenta f) {
        return f.getDetalles().stream()
                .map(d -> new FacturaDetalleDTO(
                        d.getListaPrecioArticulo().getArticulo().getId(),
                        d.getDescripcion(),
                        d.getCantidad(),
                        d.getPorcentajeBonificacion(),
                        d.getPrecioUnitario(),
                        d.getAlicuotaPorcentaje(),
                        d.getImporteNeto(),
                        d.getImporteIva(),
                        d.getImporteSubtotal()))
                .toList();
    }

    private static String numeroOGuion(Long numero) {
        return numero != null ? String.valueOf(numero) : "-";
    }

    private static String fechaOGuion(LocalDate fecha) {
        return fecha != null ? FECHA_REPORTE.format(fecha) : "-";
    }

    // --------------------------------------------------------------- reportes

    // Genera el archivo PDF en memoria y retorna los bytes
    public byte[] generarReportePDF(List<FacturaDTO> datos) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Reporte Ejecutivo de Ventas").setBold().setFontSize(16));
            document.add(new Paragraph(" ")); // Espacio

            Table table = new Table(new float[] { 1, 1, 2, 2, 3, 2, 2, 2, 1 });
            table.useAllAvailableWidth();

            String[] encabezados = { "Número", "Tipo", "Creación", "Emisión", "Cliente", "Cond. IVA", "Pto. Venta",
                    "Total", "Items" };
            for (String header : encabezados) {
                table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
            }

            DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (FacturaDTO dto : datos) {
                table.addCell(numeroOGuion(dto.numeroFactura()));
                table.addCell(dto.tipoComprobante());
                table.addCell(fechaHora.format(dto.fechaCreacion()));
                table.addCell(fechaOGuion(dto.fechaEmision()));
                table.addCell(dto.clienteDenominacion());
                table.addCell(dto.condicionIvaDenominacion());
                table.addCell(dto.puntoVentaDescripcion());
                table.addCell(String.format("$%.2f", dto.importeTotal()));
                table.addCell(String.valueOf(dto.cantidadItems()));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Genera un archivo TXT en memoria separado por tabulaciones y retorna los bytes
    public byte[] generarReporteExcelTxt(List<FacturaDTO> datos) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);

            pw.println("Número\tTipo\tFecha de Creación\tFecha de Emisión\tCliente\tCondición IVA\tPunto de Venta"
                    + "\tImporte Total\tCantidad de Ítems");

            for (FacturaDTO dto : datos) {
                pw.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%.2f\t%d%n",
                        numeroOGuion(dto.numeroFactura()),
                        dto.tipoComprobante(),
                        FECHA_REPORTE.format(dto.fechaCreacion()),
                        fechaOGuion(dto.fechaEmision()),
                        dto.clienteDenominacion(),
                        dto.condicionIvaDenominacion(),
                        dto.puntoVentaDescripcion(),
                        dto.importeTotal(),
                        dto.cantidadItems());
            }
            pw.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
