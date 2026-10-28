import type { ListaPrecioDTO } from '../models/listaPrecio.dto';
import { RUTAS } from '../config/api';
import { peticion } from './httpClient';

export class ListaPrecioService {
  public listar(): Promise<ListaPrecioDTO[]> {
    return peticion<ListaPrecioDTO[]>(RUTAS.listasPrecio);
  }
}
