import type { FacturaDTO, FacturaFiltrosDTO, FacturaGuardarDTO } from '../models/factura.dto';
import type { PageResponse } from '../models/page.dto';
import { RUTAS } from '../config/api';
import { construirQuery, peticion, peticionArchivo } from './httpClient';

function jsonConCuerpo(method: 'POST' | 'PUT', body: unknown): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
}

export class FacturaService {
  private readonly baseUrl: string = RUTAS.facturas;

  /** Página del listado; `pagina` es 0-based. */
  public obtenerFacturas(
    filtros: FacturaFiltrosDTO,
    pagina: number,
    tamanio: number,
  ): Promise<PageResponse<FacturaDTO>> {
    const query = construirQuery({
      fechaDesde: filtros.fechaDesde,
      fechaHasta: filtros.fechaHasta,
      estado: filtros.estado,
      montoMinimo: filtros.montoMinimo,
      clienteId: filtros.clienteId,
      tipoComprobante: filtros.tipoComprobante,
      cae: filtros.cae,
      page: pagina,
      size: tamanio,
    });
    return peticion<PageResponse<FacturaDTO>>(`${this.baseUrl}${query}`);
  }

  /** Detalle completo (con renglones) para precargar la edición. */
  public obtenerFactura(id: number): Promise<FacturaDTO> {
    return peticion<FacturaDTO>(`${this.baseUrl}/${id}`);
  }

  public crearFactura(factura: FacturaGuardarDTO): Promise<FacturaDTO> {
    return peticion<FacturaDTO>(this.baseUrl, jsonConCuerpo('POST', factura));
  }

  /** Pide el CAE a ARCA. Resuelve EMITIDA (con número y CAE) o ERROR_AFIP (con motivoRechazo). */
  public emitirFactura(id: number): Promise<FacturaDTO> {
    return peticion<FacturaDTO>(`${this.baseUrl}/${id}/emitir`, { method: 'POST' });
  }

  /** Eliminar = baja lógica vía DELETE /{id} (PENDIENTE / ERROR_AFIP); deja de aparecer en el listado. */
  public eliminarFactura(id: number): Promise<void> {
    return peticion<void>(`${this.baseUrl}/${id}`, { method: 'DELETE' });
  }

  /** Anular una EMITIDA (simula la Nota de Crédito): queda listada como ANULADA. */
  public anularFactura(id: number): Promise<FacturaDTO> {
    return peticion<FacturaDTO>(`${this.baseUrl}/${id}/anular`, { method: 'POST' });
  }

  /** PDF del comprobante autorizado (original, duplicado y triplicado, con QR). 409 si no está emitida. */
  public obtenerPdf(id: number): Promise<Blob> {
    return peticionArchivo(`${this.baseUrl}/${id}/pdf`);
  }

  /** Actualización vía PUT /{id} (409 si no es editable). */
  public actualizarFactura(id: number, cambios: FacturaGuardarDTO): Promise<FacturaDTO> {
    return peticion<FacturaDTO>(`${this.baseUrl}/${id}`, jsonConCuerpo('PUT', cambios));
  }
}
