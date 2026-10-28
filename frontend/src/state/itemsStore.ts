import type { ItemEnMemoria } from '../models/factura.dto';

/** Ítem con identidad de fila para el editor (el servidor no lo conoce). */
export type ItemBorrador = ItemEnMemoria & { readonly uid: number };

export type RenglonCalculado = {
  readonly neto: number;
  readonly iva: number;
  readonly subtotal: number;
};

export type IvaPorAlicuota = {
  /** Porcentaje de la alícuota (21, 10.5, ...). */
  readonly alicuota: number;
  readonly base: number;
  readonly iva: number;
};

export type TotalesItems = {
  readonly neto: number;
  readonly iva: number;
  readonly total: number;
  /** Desglose por alícuota (ascendente); vacío si el emisor no discrimina IVA. */
  readonly porAlicuota: readonly IvaPorAlicuota[];
};

/** Cociente num/den redondeado HALF_UP (num, den enteros positivos). */
function dividirHalfUp(num: number, den: number): number {
  return Math.floor((2 * num + den) / (2 * den));
}

/** Importes de un renglón en centavos enteros (evita errores de punto flotante).
 *  Igual que el servidor: neto = precio × cant × (1 − bonif/100) y IVA = neto × alícuota,
 *  cada uno redondeado a 2 decimales HALF_UP por renglón. */
function renglonEnCentavos(item: ItemEnMemoria, discriminaIva: boolean): { neto: number; iva: number } {
  const precioCent = Math.round(item.precioReferencial * 100);
  const bonifCentesimas = Math.round(item.porcentajeBonificacion * 100);
  const neto = dividirHalfUp(precioCent * item.cantidad * (10000 - bonifCentesimas), 10000);
  const alicuotaCentesimas = Math.round(item.alicuotaPorcentaje * 100);
  const iva = discriminaIva ? dividirHalfUp(neto * alicuotaCentesimas, 10000) : 0;
  return { neto, iva };
}

/** Neto, IVA y subtotal de un renglón. Si el emisor no discrimina IVA: IVA 0 y subtotal = neto. */
export function calcularRenglon(item: ItemEnMemoria, discriminaIva: boolean): RenglonCalculado {
  const { neto, iva } = renglonEnCentavos(item, discriminaIva);
  return { neto: neto / 100, iva: iva / 100, subtotal: (neto + iva) / 100 };
}

/** Suma neto/IVA/total de los ítems y agrupa el IVA por alícuota. Función pura: fácil de testear. */
export function calcularTotales(items: readonly ItemEnMemoria[], discriminaIva: boolean): TotalesItems {
  let neto = 0;
  let iva = 0;
  const grupos = new Map<number, { base: number; iva: number }>();
  for (const item of items) {
    const r = renglonEnCentavos(item, discriminaIva);
    neto += r.neto;
    iva += r.iva;
    if (!discriminaIva) continue;
    const grupo = grupos.get(item.alicuotaPorcentaje) ?? { base: 0, iva: 0 };
    grupos.set(item.alicuotaPorcentaje, { base: grupo.base + r.neto, iva: grupo.iva + r.iva });
  }
  const porAlicuota = [...grupos.entries()]
    .sort(([a], [b]) => a - b)
    .map(([alicuota, g]) => ({ alicuota, base: g.base / 100, iva: g.iva / 100 }));
  return { neto: neto / 100, iva: iva / 100, total: (neto + iva) / 100, porAlicuota };
}

type Oyente = () => void;

/** Estado en memoria de los renglones: la UI se re-renderiza a partir de él.
 *  `discriminaIva` (del emisor) define si se calcula y muestra IVA. */
export function crearItemsStore(discriminaIva: boolean): ItemsStore {
  let items: ItemBorrador[] = [];
  let siguienteUid = 1;
  const oyentes = new Set<Oyente>();

  function notificar(): void {
    oyentes.forEach((oyente) => {
      oyente();
    });
  }

  return {
    discriminaIva,
    suscribir(oyente: Oyente): () => void {
      oyentes.add(oyente);
      return () => {
        oyentes.delete(oyente);
      };
    },
    obtener(): readonly ItemBorrador[] {
      return items;
    },
    agregar(): ItemBorrador {
      const creado: ItemBorrador = {
        uid: siguienteUid,
        descripcion: '',
        cantidad: 1,
        porcentajeBonificacion: 0,
        articuloId: 0,
        precioReferencial: 0,
        alicuotaPorcentaje: 0,
        articuloEtiqueta: '',
      };
      siguienteUid += 1;
      items = [...items, creado];
      notificar();
      return creado;
    },
    quitar(uid: number): void {
      items = items.filter((item) => item.uid !== uid);
      notificar();
    },
    actualizar(uid: number, cambios: Partial<ItemEnMemoria>): void {
      items = items.map((item) => (item.uid === uid ? { ...item, ...cambios } : item));
      notificar();
    },
    limpiar(): void {
      items = [];
      notificar();
    },
    renglon(item: ItemEnMemoria): RenglonCalculado {
      return calcularRenglon(item, discriminaIva);
    },
    totales(): TotalesItems {
      return calcularTotales(items, discriminaIva);
    },
  };
}

export type ItemsStore = {
  readonly discriminaIva: boolean;
  readonly suscribir: (oyente: Oyente) => () => void;
  readonly obtener: () => readonly ItemBorrador[];
  readonly agregar: () => ItemBorrador;
  readonly quitar: (uid: number) => void;
  readonly actualizar: (uid: number, cambios: Partial<ItemEnMemoria>) => void;
  readonly limpiar: () => void;
  readonly renglon: (item: ItemEnMemoria) => RenglonCalculado;
  readonly totales: () => TotalesItems;
};
