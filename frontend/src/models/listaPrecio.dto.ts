export type ListaPrecioDTO = {
  readonly id: number;
  readonly codigo: string;
  readonly denominacion: string;
};

/** Precio (neto) de un artículo en una lista. */
export type PrecioListaDTO = {
  readonly listaPrecioId: number;
  readonly precioVenta: number;
};

/** Artículo con el precio (neto) y la alícuota de IVA que tiene en una lista de precios concreta.
 *  `precios` trae su precio en cada lista donde figura (para recalcular al cambiar de lista). */
export type ArticuloListaDTO = {
  readonly articuloId: number;
  readonly codigo: string;
  readonly denominacion: string;
  readonly rubro: string | null;
  readonly marca: string | null;
  readonly precioVenta: number;
  readonly alicuotaPorcentaje: number;
  readonly precios: readonly PrecioListaDTO[];
};

export type RubroDTO = {
  readonly id: number;
  readonly denominacion: string;
};

export type MarcaDTO = {
  readonly id: number;
  readonly denominacion: string;
};

export type ArticuloOrden = 'codigo' | 'denominacion' | 'precio';

export type ArticuloBusquedaDTO = {
  readonly q?: string;
  readonly rubroId?: number;
  readonly marcaId?: number;
  readonly orden?: ArticuloOrden;
  readonly dir?: 'asc' | 'desc';
  readonly pagina?: number;
  readonly tamanio?: number;
};
