import { DEBOUNCE_MS } from '../config/constantes';
import type { PageResponse } from '../models/page.dto';
import { mensajeDeError } from '../services/httpClient';
import { crearElemento, limpiarHijos } from '../utils/dom';
import { SVG_LUPA } from './iconos';
import { crearPaginador } from './paginador';

export type OpcionExplorer = {
  readonly valor: string;
  readonly texto: string;
};

/** Filtro desplegable; la primera opción debería ser "Todos" con valor ''. */
export type FiltroExplorer = {
  readonly clave: string;
  readonly etiqueta: string;
  readonly opciones: readonly OpcionExplorer[];
};

export type OrdenExplorer = {
  readonly valor: string;
  readonly texto: string;
  /** Campo de orden que entiende el servidor. */
  readonly orden: string;
  readonly dir: 'asc' | 'desc';
};

export type ColumnaExplorer<T> = {
  readonly titulo: string;
  readonly valor: (item: T) => string;
  readonly numerica?: boolean;
};

export type ParamsExplorer = {
  readonly q: string;
  /** clave del filtro → valor elegido ('' = sin filtro). */
  readonly filtros: Readonly<Record<string, string>>;
  readonly orden: OrdenExplorer;
  /** Página pedida (0-based). */
  readonly pagina: number;
};

export type ConfigExplorer<T> = {
  readonly titulo: string;
  readonly placeholderBusqueda: string;
  readonly columnas: readonly ColumnaExplorer<T>[];
  readonly filtros: readonly FiltroExplorer[];
  /** Al menos una; la primera es el orden inicial. */
  readonly ordenes: readonly OrdenExplorer[];
  readonly buscar: (params: ParamsExplorer) => Promise<PageResponse<T>>;
  readonly mensajeVacio: string;
};

let dialogo: HTMLDialogElement | null = null;

function selectConEtiqueta(
  id: string,
  etiqueta: string,
  opciones: readonly OpcionExplorer[],
): { campo: HTMLElement; select: HTMLSelectElement } {
  const campo = crearElemento('div', 'campo');
  const lab = crearElemento('label', undefined, etiqueta);
  lab.setAttribute('for', id);
  const select = crearElemento('select');
  select.id = id;
  for (const op of opciones) select.appendChild(new Option(op.texto, op.valor));
  campo.append(lab, select);
  return { campo, select };
}

/** Explorer genérico (modal secundario, apilable sobre otros diálogos).
 *  Resuelve el ítem elegido (doble clic, Enter o botón "Seleccionar") o null si se cancela. */
export function abrirExplorer<T>(config: ConfigExplorer<T>): Promise<T | null> {
  if (!dialogo) {
    dialogo = crearElemento('dialog');
    dialogo.id = 'dialogo-explorer';
    document.body.appendChild(dialogo);
  }
  const dlg = dialogo;
  limpiarHijos(dlg);

  return new Promise<T | null>((resolver) => {
    const ctrl = new AbortController();
    const { signal } = ctrl;
    let resuelto = false;
    let secuencia = 0;
    let temporizador: number | undefined;
    let seleccionado: T | null = null;

    function cerrar(valor: T | null): void {
      if (resuelto) return;
      resuelto = true;
      window.clearTimeout(temporizador);
      ctrl.abort();
      dlg.close();
      resolver(valor);
    }

    // --- Estructura ---
    const titulo = crearElemento('h2', undefined, config.titulo);
    titulo.id = 'explorer-titulo';
    dlg.setAttribute('aria-labelledby', 'explorer-titulo');

    const btnX = crearElemento('button', 'btn-icono', '×');
    btnX.type = 'button';
    btnX.setAttribute('aria-label', 'Cerrar buscador');
    const encabezado = crearElemento('div', 'modal-encabezado');
    encabezado.append(titulo, btnX);

    const campoBusqueda = crearElemento('div', 'campo explorer-busqueda');
    // La lupa reemplaza al texto "Buscar"; el label queda solo para lectores de pantalla.
    const labBusqueda = crearElemento('label', 'solo-lectores', 'Buscar');
    labBusqueda.setAttribute('for', 'explorer-q');
    const inputBusqueda = crearElemento('input');
    inputBusqueda.id = 'explorer-q';
    inputBusqueda.type = 'search';
    inputBusqueda.placeholder = config.placeholderBusqueda;
    inputBusqueda.autocomplete = 'off';
    const lupa = crearElemento('span', 'input-lupa-icono');
    lupa.innerHTML = SVG_LUPA; // SVG estático propio
    const envoltorioBusqueda = crearElemento('div', 'input-lupa');
    envoltorioBusqueda.append(lupa, inputBusqueda);
    campoBusqueda.append(labBusqueda, envoltorioBusqueda);

    const barra = crearElemento('div', 'explorer-barra');
    barra.appendChild(campoBusqueda);
    const selectsFiltro = new Map<string, HTMLSelectElement>();
    for (const filtro of config.filtros) {
      const { campo, select } = selectConEtiqueta(`explorer-f-${filtro.clave}`, filtro.etiqueta, filtro.opciones);
      selectsFiltro.set(filtro.clave, select);
      barra.appendChild(campo);
    }
    const { campo: campoOrden, select: selectOrden } = selectConEtiqueta(
      'explorer-orden',
      'Ordenar por',
      config.ordenes.map((o) => ({ valor: o.valor, texto: o.texto })),
    );
    barra.appendChild(campoOrden);

    const thead = crearElemento('thead');
    const trHead = crearElemento('tr');
    for (const col of config.columnas) {
      const th = crearElemento('th', col.numerica ? 'num' : undefined, col.titulo);
      th.scope = 'col';
      trHead.appendChild(th);
    }
    thead.appendChild(trHead);
    const tbody = crearElemento('tbody');
    tbody.className = 'explorer-filas';
    const tabla = crearElemento('table');
    tabla.append(thead, tbody);
    const scroll = crearElemento('div', 'tabla-scroll explorer-scroll');
    scroll.appendChild(tabla);
    const estado = crearElemento('div', 'estado-listado');
    estado.setAttribute('aria-live', 'polite');
    estado.hidden = true;

    const cuerpo = crearElemento('div', 'modal-cuerpo explorer-cuerpo');
    cuerpo.append(barra, scroll, estado);

    const contenedorPaginador = crearElemento('div', 'explorer-paginador');
    const btnCancelar = crearElemento('button', 'btn btn-secundario', 'Cancelar');
    btnCancelar.type = 'button';
    const btnSeleccionar = crearElemento('button', 'btn btn-primario', 'Seleccionar');
    btnSeleccionar.type = 'button';
    btnSeleccionar.disabled = true;
    const pie = crearElemento('div', 'modal-pie explorer-pie');
    pie.append(contenedorPaginador, btnCancelar, btnSeleccionar);

    dlg.append(encabezado, cuerpo, pie);

    // --- Comportamiento ---
    // Parámetros de la última búsqueda: paginar no debe tomar lo que se esté tecleando.
    let ultimos: ParamsExplorer | null = null;
    const paginador = crearPaginador(contenedorPaginador, {
      alCambiarPagina: (pagina) => {
        if (ultimos) ejecutar({ ...ultimos, pagina });
      },
    });

    function ordenActual(): OrdenExplorer {
      return config.ordenes.find((o) => o.valor === selectOrden.value) ?? (config.ordenes[0] as OrdenExplorer);
    }

    function params(): ParamsExplorer {
      const filtros: Record<string, string> = {};
      selectsFiltro.forEach((sel, clave) => {
        filtros[clave] = sel.value;
      });
      return { q: inputBusqueda.value, filtros, orden: ordenActual(), pagina: 0 };
    }

    function fijarSeleccion(item: T | null, fila: HTMLTableRowElement | null): void {
      seleccionado = item;
      btnSeleccionar.disabled = item === null;
      tbody.querySelectorAll('tr').forEach((tr) => {
        const activa = tr === fila;
        tr.classList.toggle('seleccionada', activa);
        tr.setAttribute('aria-selected', activa ? 'true' : 'false');
      });
    }

    function mostrarMensaje(texto: string, esError: boolean, reintentar?: () => void): void {
      limpiarHijos(tbody);
      limpiarHijos(estado);
      estado.hidden = false;
      estado.appendChild(crearElemento('p', esError ? 'error' : undefined, texto));
      if (reintentar) {
        const btn = crearElemento('button', 'btn btn-secundario', 'Reintentar');
        btn.type = 'button';
        btn.addEventListener('click', reintentar);
        estado.appendChild(btn);
      }
    }

    function renderFilas(items: readonly T[]): void {
      limpiarHijos(tbody);
      limpiarHijos(estado);
      estado.hidden = true;
      items.forEach((item) => {
        const tr = crearElemento('tr', 'explorer-fila');
        tr.tabIndex = 0;
        tr.setAttribute('aria-selected', 'false');
        for (const col of config.columnas) {
          tr.appendChild(crearElemento('td', col.numerica ? 'num' : undefined, col.valor(item)));
        }
        tr.addEventListener('click', () => fijarSeleccion(item, tr));
        tr.addEventListener('dblclick', () => cerrar(item));
        tr.addEventListener('keydown', (ev) => {
          if (ev.key === 'Enter') {
            ev.preventDefault();
            cerrar(item);
          } else if (ev.key === ' ') {
            ev.preventDefault();
            fijarSeleccion(item, tr);
          }
        });
        tbody.appendChild(tr);
      });
    }

    function ejecutar(p: ParamsExplorer): void {
      ultimos = p;
      secuencia += 1;
      const mia = secuencia;
      fijarSeleccion(null, null);
      paginador.ocultar();
      mostrarMensaje('Cargando…', false);
      config.buscar(p).then(
        (resp) => {
          if (resuelto || mia !== secuencia) return;
          if (resp.contenido.length === 0) {
            // Página fuera de rango (los datos cambiaron): volver a la última existente.
            if (p.pagina > 0 && resp.totalPaginas > 0) {
              ejecutar({ ...p, pagina: resp.totalPaginas - 1 });
              return;
            }
            mostrarMensaje(config.mensajeVacio, false);
          } else {
            renderFilas(resp.contenido);
          }
          paginador.actualizar(resp);
        },
        (error: unknown) => {
          if (resuelto || mia !== secuencia) return;
          mostrarMensaje(mensajeDeError(error), true, () => ejecutar(p));
        },
      );
    }

    /** Nueva búsqueda (texto, filtro u orden cambiados): siempre desde la página 0. */
    function cargar(): void {
      ejecutar(params());
    }

    inputBusqueda.addEventListener(
      'input',
      () => {
        window.clearTimeout(temporizador);
        temporizador = window.setTimeout(cargar, DEBOUNCE_MS);
      },
      { signal },
    );
    inputBusqueda.addEventListener(
      'keydown',
      (ev) => {
        if (ev.key === 'Enter') {
          ev.preventDefault();
          window.clearTimeout(temporizador);
          cargar();
        }
      },
      { signal },
    );
    const alCambiarSelect = (): void => {
      window.clearTimeout(temporizador);
      cargar();
    };
    selectOrden.addEventListener('change', alCambiarSelect, { signal });
    selectsFiltro.forEach((sel) => sel.addEventListener('change', alCambiarSelect, { signal }));

    btnX.addEventListener('click', () => cerrar(null), { signal });
    btnCancelar.addEventListener('click', () => cerrar(null), { signal });
    btnSeleccionar.addEventListener(
      'click',
      () => {
        if (seleccionado !== null) cerrar(seleccionado);
      },
      { signal },
    );
    dlg.addEventListener(
      'cancel',
      (ev) => {
        ev.preventDefault();
        cerrar(null);
      },
      { signal },
    );
    dlg.addEventListener(
      'click',
      (ev) => {
        if (ev.target === dlg) cerrar(null);
      },
      { signal },
    );

    dlg.showModal();
    inputBusqueda.focus();
    cargar();
  });
}
