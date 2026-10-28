import { esEstadoFactura } from '../config/constantes';
import type { FacturaFiltrosDTO, ItemEnMemoria } from '../models/factura.dto';

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

export function construirFiltros(desde: string, hasta: string, estado: string, monto: string): FacturaFiltrosDTO {
  const filtros: FacturaFiltrosDTO = {};
  if (desde) filtros.fechaDesde = desde;
  if (hasta) filtros.fechaHasta = hasta;
  if (esEstadoFactura(estado)) filtros.estado = estado;
  if (monto.trim() !== '') filtros.montoMinimo = Number(monto);
  return filtros;
}

export type CabeceraFactura = {
  clienteId: string;
  listaPrecioId: string;
  puntoVentaId: string;
  monedaId: string;
};

/** Valida la cabecera; devuelve errores por id de campo. */
export function validarCabecera(c: CabeceraFactura): Errores {
  const errores: Errores = {};
  if (!c.clienteId) errores['ff-cliente'] = 'Seleccioná un cliente.';
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
