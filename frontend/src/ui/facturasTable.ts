import type { FacturaDTO } from '../models/factura.dto';
import { ESTADO, ESTADO_INFO, esAnulable, esEditable } from '../config/constantes';
import { crearElemento, limpiarHijos, qs } from '../utils/dom';
import { etiquetaFactura, formatearFechaISO, formatearMoneda } from '../utils/formato';

export type AccionFila = 'editar' | 'emitir' | 'anular';

export type TablaCallbacks = {
  readonly alAccionar: (accion: AccionFila, factura: FacturaDTO) => void;
};

const ATRIBUTOS_SVG =
  'viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
// Papelera (anular) y lápiz (editar): SVG estático propio.
const SVG_ANULAR = `<svg ${ATRIBUTOS_SVG}><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>`;
const SVG_EDITAR = `<svg ${ATRIBUTOS_SVG}><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>`;

const ICONOS: Record<Exclude<AccionFila, 'emitir'>, string> = {
  editar: SVG_EDITAR,
  anular: SVG_ANULAR,
};

function celda(texto: string, clase?: string): HTMLTableCellElement {
  return crearElemento('td', clase, texto);
}

function badgeEstado(f: FacturaDTO): HTMLSpanElement {
  const info = ESTADO_INFO[f.estado];
  const span = crearElemento('span', `badge ${info.badge}`, info.etiqueta);
  // Motivo del rechazo de ARCA al pasar el mouse.
  if (f.estado === ESTADO.ERROR_AFIP && f.motivoRechazo) span.title = f.motivoRechazo;
  return span;
}

function botonIcono(accion: AccionFila, etiqueta: string, deshabilitado: boolean): HTMLButtonElement {
  if (accion === 'emitir') {
    // Emitir es la acción principal: botón con texto, no un ícono.
    const emitir = crearElemento('button', 'btn btn-chico btn-exito btn-emitir', 'Emitir');
    emitir.type = 'button';
    emitir.dataset['accion'] = accion;
    emitir.title = etiqueta;
    emitir.setAttribute('aria-label', etiqueta);
    emitir.disabled = deshabilitado;
    return emitir;
  }
  const btn = crearElemento('button', `btn-icono btn-${accion}`);
  btn.type = 'button';
  btn.dataset['accion'] = accion;
  btn.title = etiqueta;
  btn.setAttribute('aria-label', etiqueta);
  btn.disabled = deshabilitado;
  btn.innerHTML = ICONOS[accion];
  return btn;
}

/** Tabla con delegación de eventos: un solo listener para todas las filas. */
export function initTablaFacturas(cb: TablaCallbacks): {
  render: (facturas: readonly FacturaDTO[]) => void;
  carga: () => void;
  vacio: () => void;
  error: (mensaje: string, reintentar: () => void) => void;
  /** Total de registros del listado (null = sin dato todavía). */
  resumen: (totalRegistros: number | null) => void;
  /** Marca la factura como "emitiendo" (spinner) y bloquea las acciones hasta que se recargue el listado. */
  emitiendo: (id: number) => void;
} {
  const body = qs<HTMLTableSectionElement>('#facturas-body');
  const estado = qs<HTMLElement>('#estado-listado');
  const resumen = qs<HTMLElement>('#resumen');
  let actuales: readonly FacturaDTO[] = [];

  body.addEventListener('click', (ev) => {
    const btn = (ev.target as HTMLElement).closest('button[data-accion]');
    if (!btn || !(btn instanceof HTMLButtonElement) || btn.disabled) return;
    const id = Number(btn.dataset['id']);
    const factura = actuales.find((f) => f.id === id);
    if (!factura) return;
    const accion = btn.dataset['accion'];
    if (accion !== 'editar' && accion !== 'emitir' && accion !== 'anular') return;
    cb.alAccionar(accion, factura);
  });

  function ocultarEstado(): void {
    estado.hidden = true;
    limpiarHijos(estado);
  }

  function fila(f: FacturaDTO): HTMLTableRowElement {
    const tr = crearElemento('tr');
    const anulada = f.estado === ESTADO.ANULADA;
    if (anulada) tr.classList.add('fila-anulada');

    // Número ('—' mientras no se emitió) y CAE si existe.
    const tdNumero = crearElemento('td', 'num', f.numeroFactura === null ? '—' : String(f.numeroFactura));
    if (f.cae) {
      tdNumero.appendChild(crearElemento('small', 'texto-suave bloque', `CAE: ${f.cae}`));
    }

    const tdEstado = crearElemento('td');
    tdEstado.appendChild(badgeEstado(f));

    const editable = esEditable(f.estado);
    const ref = etiquetaFactura(f);
    const btnEditar = botonIcono('editar', `Editar factura ${ref}`, !editable);
    const btnEmitir = botonIcono('emitir', `Emitir factura ${ref}`, !editable);
    const btnAnular = botonIcono('anular', `Anular factura ${ref}`, !esAnulable(f.estado));
    for (const b of [btnEditar, btnEmitir, btnAnular]) b.dataset['id'] = String(f.id);
    const tdAcc = crearElemento('td', 'acciones');
    tdAcc.append(btnEmitir, btnEditar, btnAnular);

    tr.append(
      tdNumero,
      celda(f.tipoComprobante, 'tipo-comprobante'),
      celda(formatearFechaISO(f.fechaCreacion)),
      celda(f.fechaEmision === null ? '—' : formatearFechaISO(f.fechaEmision)),
      celda(f.clienteDenominacion),
      celda(f.listaPrecioDenominacion),
      celda(f.condicionIvaDenominacion),
      celda(f.puntoVentaDescripcion),
      celda(String(f.cantidadItems), 'num'),
      celda(formatearMoneda(f.importeTotal), 'num total'),
      tdEstado,
      tdAcc,
    );
    return tr;
  }

  return {
    render(facturas: readonly FacturaDTO[]): void {
      actuales = facturas;
      limpiarHijos(body);
      ocultarEstado();
      for (const f of facturas) body.appendChild(fila(f));
    },
    carga(): void {
      actuales = [];
      limpiarHijos(body);
      for (let i = 0; i < 5; i += 1) {
        const tr = crearElemento('tr', 'skeleton-fila');
        for (let c = 0; c < 12; c += 1) {
          const td = crearElemento('td');
          td.appendChild(crearElemento('span', 'skeleton'));
          tr.appendChild(td);
        }
        body.appendChild(tr);
      }
      ocultarEstado();
    },
    vacio(): void {
      actuales = [];
      limpiarHijos(body);
      limpiarHijos(estado);
      estado.hidden = false;
      const icono = crearElemento('p', 'vacio-icono', '🧾');
      icono.setAttribute('aria-hidden', 'true');
      estado.append(icono, crearElemento('p', undefined, 'No hay facturas que coincidan con los filtros.'));
    },
    error(mensaje: string, reintentar: () => void): void {
      actuales = [];
      limpiarHijos(body);
      limpiarHijos(estado);
      estado.hidden = false;
      const btn = crearElemento('button', 'btn btn-secundario', 'Reintentar');
      btn.type = 'button';
      btn.addEventListener('click', reintentar);
      estado.append(crearElemento('p', 'error', mensaje), btn);
    },
    emitiendo(id: number): void {
      for (const b of body.querySelectorAll<HTMLButtonElement>('button[data-accion]')) {
        b.disabled = true;
        if (b.dataset['accion'] === 'emitir' && b.dataset['id'] === String(id)) {
          b.textContent = 'Emitiendo…';
          b.classList.add('cargando');
        }
      }
    },
    resumen(totalRegistros: number | null): void {
      if (totalRegistros === null) {
        resumen.textContent = '';
        return;
      }
      resumen.textContent =
        totalRegistros === 0
          ? 'Sin comprobantes'
          : `${totalRegistros} comprobante${totalRegistros === 1 ? '' : 's'}`;
    },
  };
}
