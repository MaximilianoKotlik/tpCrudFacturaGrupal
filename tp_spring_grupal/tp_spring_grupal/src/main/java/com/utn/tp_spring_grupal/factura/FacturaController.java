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

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
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

    // DELETE: baja LÓGICA (anulación) de una factura PENDIENTE / ERROR_AFIP. EMITIDA o ANULADA → 409.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anularFactura(@PathVariable Long id) {
        facturaService.anularFactura(id);
        return ResponseEntity.noContent().build();
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                facturaService.buscarFacturas(fechaDesde, fechaHasta, estado, montoMinimo, page, size));
    }

    @GetMapping("/generar-pdf")
    public ResponseEntity<byte[]> generarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) BigDecimal montoMinimo) {
        List<FacturaDTO> datos = facturaService.buscarFacturasSinPaginar(fechaDesde, fechaHasta, estado, montoMinimo);
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
            @RequestParam(required = false) BigDecimal montoMinimo) {
        List<FacturaDTO> datos = facturaService.buscarFacturasSinPaginar(fechaDesde, fechaHasta, estado, montoMinimo);
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
