import type { PuntoVentaDTO, TipoMonedaDTO } from '../models/catalogo.dto';
import type { EmisorDTO } from '../models/emisor.dto';
import { RUTAS } from '../config/api';
import { peticion } from './httpClient';

export type Catalogos = {
  readonly puntosVenta: readonly PuntoVentaDTO[];
  readonly tiposMoneda: readonly TipoMonedaDTO[];
  readonly emisor: EmisorDTO;
};

/** Carga los catálogos chicos (puntos de venta, monedas) y el emisor en paralelo. */
export async function obtenerCatalogos(): Promise<Catalogos> {
  const [puntosVenta, tiposMoneda, emisor] = await Promise.all([
    peticion<PuntoVentaDTO[]>(RUTAS.puntosVenta),
    peticion<TipoMonedaDTO[]>(RUTAS.tiposMoneda),
    peticion<EmisorDTO>(RUTAS.emisor),
  ]);
  return { puntosVenta, tiposMoneda, emisor };
}
