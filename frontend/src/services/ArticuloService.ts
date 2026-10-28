import type { ArticuloBusquedaDTO, ArticuloListaDTO, MarcaDTO, RubroDTO } from '../models/listaPrecio.dto';
import type { PageResponse } from '../models/page.dto';
import { RUTAS } from '../config/api';
import { construirQuery, peticion } from './httpClient';

export class ArticuloService {
  /** Página de artículos de una lista de precios, con el precio y la alícuota de esa lista. */
  public listarPorLista(
    listaPrecioId: number,
    filtros: ArticuloBusquedaDTO = {},
  ): Promise<PageResponse<ArticuloListaDTO>> {
    const query = construirQuery({
      q: filtros.q?.trim(),
      rubroId: filtros.rubroId,
      marcaId: filtros.marcaId,
      orden: filtros.orden,
      dir: filtros.dir,
      page: filtros.pagina,
      size: filtros.tamanio,
    });
    return peticion<PageResponse<ArticuloListaDTO>>(`${RUTAS.listasPrecio}/${listaPrecioId}/articulos${query}`);
  }

  public rubros(): Promise<RubroDTO[]> {
    return peticion<RubroDTO[]>(RUTAS.rubros);
  }

  public marcas(): Promise<MarcaDTO[]> {
    return peticion<MarcaDTO[]>(RUTAS.marcas);
  }
}
