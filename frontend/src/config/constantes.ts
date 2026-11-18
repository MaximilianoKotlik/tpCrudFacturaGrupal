/** Estados de factura: valores exactos del enum del servidor (EstadoFactura). */
export const ESTADO = {
  PENDIENTE: 'PENDIENTE',
  EMITIDA: 'EMITIDA',
  ERROR_AFIP: 'ERROR_AFIP',
  ANULADA: 'ANULADA',
} as const;

export type EstadoFactura = (typeof ESTADO)[keyof typeof ESTADO];

export type InfoEstado = {
  readonly etiqueta: string;
  /** Clase CSS del badge (badges.css). */
  readonly badge: string;
};

/** Fuente única de etiquetas y badges; el orden define el del filtro de estado. */
export const ESTADO_INFO: Readonly<Record<EstadoFactura, InfoEstado>> = {
  PENDIENTE: { etiqueta: 'Pendiente', badge: 'badge-pendiente' },
  EMITIDA: { etiqueta: 'Emitida', badge: 'badge-emitida' },
  ERROR_AFIP: { etiqueta: 'Error ARCA', badge: 'badge-error' },
  ANULADA: { etiqueta: 'Anulada', badge: 'badge-anulada' },
};

export function esEstadoFactura(valor: string): valor is EstadoFactura {
  return Object.prototype.hasOwnProperty.call(ESTADO_INFO, valor);
}

/** Editable y emitible: PENDIENTE y ERROR_AFIP (no emitida, reintentable). */
export function esEditable(estado: EstadoFactura): boolean {
  return estado === ESTADO.PENDIENTE || estado === ESTADO.ERROR_AFIP;
}

/** Eliminable (baja lógica): solo las que nunca se emitieron, PENDIENTE y ERROR_AFIP. */
export function esEliminable(estado: EstadoFactura): boolean {
  return estado === ESTADO.PENDIENTE || estado === ESTADO.ERROR_AFIP;
}

/** Anulable: solo EMITIDA (simula la Nota de Crédito). */
export function esAnulable(estado: EstadoFactura): boolean {
  return estado === ESTADO.EMITIDA;
}

/** Imprimible: tiene CAE (EMITIDA, o ANULADA después de emitida). */
export function esImprimible(estado: EstadoFactura): boolean {
  return estado === ESTADO.EMITIDA || estado === ESTADO.ANULADA;
}

export const TIPOS_COMPROBANTE = ['A', 'B', 'C'] as const;

/** Tamaños de página elegibles en la tabla de facturas (el primero es el inicial). */
export const TAMANIOS_PAGINA = [10, 25, 50] as const;

/** Tamaño de página fijo de los explorers. */
export const TAMANIO_EXPLORER = 10;

export const TOAST_MS = 4500;

/** Espera (ms) tras el último tecleo antes de buscar en el servidor. */
export const DEBOUNCE_MS = 300;
