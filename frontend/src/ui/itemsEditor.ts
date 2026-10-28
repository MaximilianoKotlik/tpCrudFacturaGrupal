import type { ItemBorrador, ItemsStore } from '../state/itemsStore';
import { mensajeDeError } from '../services/httpClient';
import { crearElemento, limpiarHijos } from '../utils/dom';
import { aNumero, formatearMoneda, formatearPorcentaje } from '../utils/formato';
import { explorarArticulos } from './explorerArticulos';
import { SVG_LUPA } from './iconos';
import { mostrarToast } from './toast';

export type EditorItemsOpciones = {
  /** Lista de precios elegida en la cabecera (null = todavía no se eligió). */
  readonly listaPrecioId: () => number | null;
  readonly alCambiar: () => void;
};

/** Editor de renglones: renderiza el store y propaga cambios sin perder el foco.
 *  El artículo se elige con el explorer, siempre sobre la lista de precios de la cabecera. */
export function initEditorItems(
  body: HTMLTableSectionElement,
  errorItems: HTMLElement,
  store: ItemsStore,
  opciones: EditorItemsOpciones,
): { agregarFila: () => void; limpiar: () => void } {
  const { alCambiar } = opciones;

  function selectorArticulo(item: ItemBorrador): HTMLElement {
    const cont = crearElemento('div', 'selector');
    const texto = crearElemento('input');
    texto.type = 'text';
    texto.readOnly = true;
    texto.value = item.articuloEtiqueta;
    texto.placeholder = 'Sin artículo';
    texto.setAttribute('aria-label', 'Artículo del renglón');
    const btn = crearElemento('button', 'btn btn-secundario btn-lupa');
    btn.type = 'button';
    btn.title = 'Buscar artículo';
    btn.setAttribute('aria-label', 'Buscar artículo');
    btn.innerHTML = SVG_LUPA; // SVG estático propio
    btn.dataset['uid'] = String(item.uid);
    btn.addEventListener('click', () => {
      void elegirArticulo(item.uid);
    });
    cont.append(texto, btn);
    return cont;
  }

  async function elegirArticulo(uid: number): Promise<void> {
    const listaId = opciones.listaPrecioId();
    if (listaId === null) {
      mostrarToast('info', 'Elegí primero la lista de precios.');
      return;
    }
    try {
      const art = await explorarArticulos(listaId, store.discriminaIva);
      if (!art) return;
      store.actualizar(uid, {
        articuloId: art.articuloId,
        articuloEtiqueta: `${art.codigo} · ${art.denominacion}`,
        descripcion: art.denominacion,
        precioReferencial: art.precioVenta,
        alicuotaPorcentaje: art.alicuotaPorcentaje,
      });
      dibujar();
      alCambiar();
    } catch (error: unknown) {
      mostrarToast('error', mensajeDeError(error));
    }
  }

  /** La cantidad es entera: se bloquean las teclas que permiten decimales, signo o exponente. */
  const TECLAS_NO_ENTERAS = new Set(['.', ',', 'e', 'E', '+', '-']);

  function inputNumerico(valor: number, etiqueta: string, paso: string): HTMLInputElement {
    const input = crearElemento('input', 'num');
    input.type = 'number';
    input.min = '0';
    input.step = paso;
    input.value = String(valor);
    input.setAttribute('aria-label', etiqueta);
    return input;
  }

  function fila(item: ItemBorrador): HTMLTableRowElement {
    const tr = crearElemento('tr');
    tr.dataset['uid'] = String(item.uid);

    const tdArt = crearElemento('td');
    tdArt.appendChild(selectorArticulo(item));

    const tdDesc = crearElemento('td');
    const desc = crearElemento('input');
    desc.type = 'text';
    desc.value = item.descripcion;
    desc.maxLength = 255;
    desc.placeholder = 'Descripción';
    desc.setAttribute('aria-label', 'Descripción del renglón');
    desc.addEventListener('input', () => {
      store.actualizar(item.uid, { descripcion: desc.value });
      alCambiar();
    });
    tdDesc.appendChild(desc);

    const tdCant = crearElemento('td');
    const cant = inputNumerico(item.cantidad, 'Cantidad', '1');
    cant.min = '1';
    cant.inputMode = 'numeric';
    cant.addEventListener('keydown', (ev) => {
      if (TECLAS_NO_ENTERAS.has(ev.key)) ev.preventDefault();
    });
    cant.addEventListener('input', () => {
      store.actualizar(item.uid, { cantidad: aNumero(cant.value, 0) });
      actualizarSubtotal(tr, item.uid);
      alCambiar();
    });
    tdCant.appendChild(cant);

    const tdBonif = crearElemento('td');
    const bonif = inputNumerico(item.porcentajeBonificacion, 'Porcentaje de bonificación', '0.01');
    bonif.min = '0';
    bonif.max = '100';
    bonif.addEventListener('input', () => {
      store.actualizar(item.uid, { porcentajeBonificacion: aNumero(bonif.value, 0) });
      actualizarSubtotal(tr, item.uid);
      alCambiar();
    });
    tdBonif.appendChild(bonif);

    const tdPrecio = crearElemento('td', 'num ref', formatearMoneda(item.precioReferencial));
    const tdSub = crearElemento('td', 'num subtotal', formatearMoneda(store.renglon(item).subtotal));
    const tdIva = store.discriminaIva
      ? crearElemento('td', 'num ref', item.articuloId ? formatearPorcentaje(item.alicuotaPorcentaje) : '—')
      : null;

    const tdQuitar = crearElemento('td', 'acciones');
    const btn = crearElemento('button', 'btn-icono btn-quitar', 'Quitar renglón');
    btn.type = 'button';
    btn.setAttribute('aria-label', 'Quitar renglón');
    btn.textContent = '×';
    btn.addEventListener('click', () => {
      store.quitar(item.uid);
      dibujar();
      alCambiar();
    });
    tdQuitar.appendChild(btn);

    tr.append(tdArt, tdDesc, tdCant, tdBonif, tdPrecio, ...(tdIva ? [tdIva] : []), tdSub, tdQuitar);
    return tr;
  }

  function actualizarSubtotal(tr: HTMLTableRowElement, uid: number): void {
    const actual = store.obtener().find((i) => i.uid === uid);
    const celdaSub = tr.querySelector('td.subtotal');
    if (actual && celdaSub) {
      celdaSub.textContent = formatearMoneda(store.renglon(actual).subtotal);
    }
  }

  function dibujar(): void {
    limpiarHijos(body);
    const items = store.obtener();
    if (items.length === 0) {
      const tr = crearElemento('tr', 'fila-vacia');
      const td = crearElemento('td', undefined, 'Sin renglones. Usá “Agregar renglón” para sumar el primero.');
      td.colSpan = store.discriminaIva ? 8 : 7;
      tr.appendChild(td);
      body.appendChild(tr);
      return;
    }
    for (const item of items) body.appendChild(fila(item));
  }

  function mostrarErrorItems(mensaje: string): void {
    errorItems.textContent = mensaje;
    errorItems.hidden = mensaje.length === 0;
  }

  store.suscribir(() => {
    mostrarErrorItems('');
  });
  dibujar();

  return {
    agregarFila(): void {
      const creado = store.agregar();
      dibujar();
      alCambiar();
      const btn = body.querySelector(`tr[data-uid="${creado.uid}"] button[data-uid]`);
      if (btn instanceof HTMLButtonElement) btn.focus();
    },
    limpiar(): void {
      store.limpiar();
      dibujar();
      mostrarErrorItems('');
      alCambiar();
    },
  };
}
