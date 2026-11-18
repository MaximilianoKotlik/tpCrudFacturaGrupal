package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final FacturaPdfGenerator pdfGenerator;

    public FacturaController(FacturaService facturaService, FacturaPdfGenerator pdfGenerator) {
        this.facturaService = facturaService;
        this.pdfGenerator = pdfGenerator;
    }

    // POST: crea la factura en estado PENDIENTE (no llama a AFIP).
    @PostMapping
    public ResponseEntity<FacturaDTO> crearFactura(@RequestBody FacturaGuardarDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crearFactura(dto));
    }

    // PUT: actualización parcial; 409 si la factura no es editable (PENDIENTE / ERROR_AFIP).
    @PutMapping("/{id}")
    public ResponseEntity<FacturaDTO> actualizarFactura(@PathVariable Long id, @RequestBody FacturaGuardarDTO dto) {
        return ResponseEntity.ok(facturaService.actualizarFactura(id, dto));
    }

    // POST /{id}/emitir: pide el CAE a AFIP. EMITIDA con número+CAE, o ERROR_AFIP con motivoRechazo.
    @PostMapping("/{id}/emitir")
    public ResponseEntity<FacturaDTO> emitirFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.emitirFactura(id));
    }

    // DELETE: baja LÓGICA (fechaBaja) de una factura PENDIENTE / ERROR_AFIP; deja de listarse. Otro estado → 409.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id) {
        facturaService.eliminarFactura(id);
        return ResponseEntity.noContent().build();
    }

    // POST /{id}/anular: una EMITIDA pasa a ANULADA (simula la Nota de Crédito, sin ARCA). Otro estado → 409.
    @PostMapping("/{id}/anular")
    public ResponseEntity<FacturaDTO> anularFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.anularFactura(id));
    }

    // GET /{id}/pdf: comprobante autorizado (original, duplicado y triplicado) con QR, para ver o imprimir.
    // 409 si la factura no está emitida.
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> imprimirFactura(@PathVariable Long id) {
        ComprobantePdf comprobante = facturaService.datosComprobante(id);
        byte[] pdf = pdfGenerator.generar(comprobante);
        String nombre = String.format("Factura_%s_%05d-%08d.pdf", comprobante.tipo().getLetra(),
                comprobante.puntoVenta(), comprobante.numero());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(pdf);
    }

    // GET /{id}: detalle completo (cabecera + renglones) para precargar la edición.
    @GetMapping("/{id}")
    public ResponseEntity<FacturaDTO> obtenerFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtenerFactura(id));
    }

    // GET: listado paginado, más recientes primero. Las fechas filtran por fecha de CREACIÓN (inclusivas).
    @GetMapping
    public ResponseEntity<PageResponse<FacturaDTO>> buscarFacturas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) BigDecimal montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) String cae,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                facturaService.buscarFacturas(
                        new FacturaFiltro(fechaDesde, fechaHasta, estado, montoMinimo, clienteId, tipoComprobante, cae), page, size));
    }

    @GetMapping("/generar-pdf")
    public ResponseEntity<byte[]> generarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) BigDecimal montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) String cae) {
        List<FacturaDTO> datos = facturaService.buscarFacturasSinPaginar(
                new FacturaFiltro(fechaDesde, fechaHasta, estado, montoMinimo, clienteId, tipoComprobante, cae));
        byte[] pdfBytes = facturaService.generarReportePDF(datos);

        HttpHeaders headers = new HttpHeaders();
        // Le indicamos al navegador que es un PDF y que debe descargarlo como archivo
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "reporte_ventas.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/generar-excel")
    public ResponseEntity<byte[]> generarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) BigDecimal montoMinimo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) String cae) {
        List<FacturaDTO> datos = facturaService.buscarFacturasSinPaginar(
                new FacturaFiltro(fechaDesde, fechaHasta, estado, montoMinimo, clienteId, tipoComprobante, cae));
        byte[] txtBytes = facturaService.generarReporteExcelTxt(datos);

        HttpHeaders headers = new HttpHeaders();
        // Usamos MediaType.TEXT_PLAIN ya que es un txt con formato tabular
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "reporte_ventas.txt");

        return ResponseEntity.ok()
                .headers(headers)
                .body(txtBytes);
    }
}
