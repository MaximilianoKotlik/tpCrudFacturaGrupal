import type { FacturaDTO, FacturaFiltrosDTO, FacturaGuardarDTO } from '../models/factura.dto';
import type { PageResponse } from '../models/page.dto';
import { RUTAS } from '../config/api';
import { construirQuery, peticion } from './httpClient';

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

  /** Anular = baja lógica vía DELETE /{id}; 409 si está EMITIDA. La factura sigue listada como ANULADA. */
  public anularFactura(id: number): Promise<void> {
    return peticion<void>(`${this.baseUrl}/${id}`, { method: 'DELETE' });
  }

  /** Actualización vía PUT /{id} (409 si no es editable). */
  public actualizarFactura(id: number, cambios: FacturaGuardarDTO): Promise<FacturaDTO> {
    return peticion<FacturaDTO>(`${this.baseUrl}/${id}`, jsonConCuerpo('PUT', cambios));
  }
}
