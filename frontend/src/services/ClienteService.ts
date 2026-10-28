import type { ClienteBusquedaDTO, ClienteDTO } from '../models/cliente.dto';
import type { PageResponse } from '../models/page.dto';
import { RUTAS } from '../config/api';
import { construirQuery, peticion } from './httpClient';

export class ClienteService {
  /** Búsqueda paginada en servidor por denominación o CUIT/CUIL, con orden. */
  public buscar(filtros: ClienteBusquedaDTO = {}): Promise<PageResponse<ClienteDTO>> {
    const query = construirQuery({
      q: filtros.q?.trim(),
      orden: filtros.orden,
      dir: filtros.dir,
      page: filtros.pagina,
      size: filtros.tamanio,
    });
    return peticion<PageResponse<ClienteDTO>>(`${RUTAS.clientes}${query}`);
  }
}
