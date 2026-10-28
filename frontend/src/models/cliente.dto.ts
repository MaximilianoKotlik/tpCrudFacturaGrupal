export type ClienteDTO = {
  readonly id: number;
  readonly denominacion: string;
  readonly cuitCuil: string;
  readonly condicionIvaId: number;
  readonly condicionIvaDenominacion: string;
};

export type ClienteOrden = 'denominacion' | 'cuit';

export type ClienteBusquedaDTO = {
  readonly q?: string;
  readonly orden?: ClienteOrden;
  readonly dir?: 'asc' | 'desc';
  readonly pagina?: number;
  readonly tamanio?: number;
};
