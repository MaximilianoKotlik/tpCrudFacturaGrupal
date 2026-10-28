export type CondicionIvaEmisor = 'RESPONSABLE_INSCRIPTO' | 'MONOTRIBUTO' | 'EXENTO';

/** GET /api/emisor: si `discriminaIva` es false (comprobante C) la UI no muestra IVA. */
export type EmisorDTO = {
  readonly condicionIva: CondicionIvaEmisor;
  readonly discriminaIva: boolean;
};
