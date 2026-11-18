import { esEstadoFactura } from '../config/constantes';
import { TIPOS_COMPROBANTE } from '../config/constantes';
import type { FacturaFiltrosDTO, ItemEnMemoria, TipoComprobante } from '../models/factura.dto';

/** Mapa campo → mensaje. Vacío = válido. */
export type Errores = Record<string, string>;

export function validarRangoFechas(desde: string, hasta: string): string {
  if (desde && hasta && desde > hasta) {
    return 'La fecha desde no puede ser posterior a la fecha hasta.';
  }
  return '';
}

export function filtrosValidos(desde: string, hasta: string, monto: string): boolean {
  if (validarRangoFechas(desde, hasta)) return false;
  if (monto.trim() !== '' && !(Number(monto) >= 0)) return false;
  return true;
}

export type ValoresFiltros = {
  readonly desde: string;
  readonly hasta: string;
  readonly estado: string;
  readonly monto: string;
  readonly clienteId: number | null;
  readonly tipo: string;
  readonly cae: string;
};

function esTipoComprobante(valor: string): valor is TipoComprobante {
  return (TIPOS_COMPROBANTE as readonly string[]).includes(valor);
}

export function construirFiltros(v: ValoresFiltros): FacturaFiltrosDTO {
  const filtros: FacturaFiltrosDTO = {};
  if (v.desde) filtros.fechaDesde = v.desde;
  if (v.hasta) filtros.fechaHasta = v.hasta;
  if (esEstadoFactura(v.estado)) filtros.estado = v.estado;
  if (v.monto.trim() !== '') filtros.montoMinimo = Number(v.monto);
  if (v.clienteId !== null) filtros.clienteId = v.clienteId;
  if (esTipoComprobante(v.tipo)) filtros.tipoComprobante = v.tipo;
  if (v.cae.trim() !== '') filtros.cae = v.cae.trim();
  return filtros;
}

export type CabeceraFactura = {
  /** Sin cliente: Consumidor Final sin identificar. */
  consumidorFinal: boolean;
  clienteId: string;
  listaPrecioId: string;
  puntoVentaId: string;
  monedaId: string;
};

/** Valida la cabecera; devuelve errores por id de campo. */
export function validarCabecera(c: CabeceraFactura): Errores {
  const errores: Errores = {};
  if (!c.consumidorFinal && !c.clienteId) {
    errores['ff-cliente'] = 'Seleccioná un cliente o marcá Consumidor Final.';
  }
  if (!c.listaPrecioId) errores['ff-lista'] = 'Seleccioná la lista de precios.';
  if (!c.puntoVentaId) errores['ff-pv'] = 'Seleccioná el punto de venta.';
  if (!c.monedaId) errores['ff-moneda'] = 'Seleccioná el tipo de moneda.';
  return errores;
}

/** Valida un renglón; devuelve el primer mensaje o '' si está OK. */
export function validarItem(item: ItemEnMemoria): string {
  if (!item.articuloId) return 'Elegí un artículo.';
  if (item.descripcion.trim().length === 0) return 'La descripción no puede estar vacía.';
  if (!Number.isInteger(item.cantidad) || item.cantidad < 1) return 'La cantidad debe ser un entero mayor o igual a 1.';
  if (!(item.porcentajeBonificacion >= 0 && item.porcentajeBonificacion <= 100)) {
    return 'La bonificación debe estar entre 0 y 100.';
  }
  if (!(item.precioReferencial > 0)) return 'El artículo no tiene precio de referencia.';
  return '';
}
