import type { InfoPagina } from '../models/page.dto';
import { crearElemento } from '../utils/dom';

export type PaginadorOpciones = {
  /** Se llama con la página (0-based) a la que se quiere ir. */
  readonly alCambiarPagina: (pagina: number) => void;
  /** Si se indica junto con `tamanios`, se muestra el selector de tamaño de página. */
  readonly alCambiarTamanio?: (tamanio: number) => void;
  readonly tamanios?: readonly number[];
};

export type Paginador = {
  /** Muestra la página recibida (oculto si no hay registros). */
  readonly actualizar: (info: InfoPagina) => void;
  readonly ocultar: () => void;
};

/** Paginador reutilizable: "N registros · Anterior · Página x de y · Siguiente [· Por página]".
 *  Solo dibuja y avisa; quien lo usa decide qué cargar. */
export function crearPaginador(contenedor: HTMLElement, opciones: PaginadorOpciones): Paginador {
  contenedor.classList.add('paginador');
  contenedor.setAttribute('role', 'navigation');
  contenedor.setAttribute('aria-label', 'Paginación');
  contenedor.hidden = true;

  const total = crearElemento('span', 'paginador-total');
  total.setAttribute('aria-live', 'polite');
  const btnAnterior = crearElemento('button', 'btn btn-secundario btn-chico', 'Anterior');
  btnAnterior.type = 'button';
  const indicador = crearElemento('span', 'paginador-pagina');
  const btnSiguiente = crearElemento('button', 'btn btn-secundario btn-chico', 'Siguiente');
  btnSiguiente.type = 'button';
  const navegacion = crearElemento('div', 'paginador-nav');
  navegacion.append(btnAnterior, indicador, btnSiguiente);
  contenedor.append(total, navegacion);

  let paginaActual = 0;
  let ultimaPagina = 0;

  btnAnterior.addEventListener('click', () => {
    if (paginaActual > 0) opciones.alCambiarPagina(paginaActual - 1);
  });
  btnSiguiente.addEventListener('click', () => {
    if (paginaActual < ultimaPagina) opciones.alCambiarPagina(paginaActual + 1);
  });

  let selectTamanio: HTMLSelectElement | null = null;
  const { tamanios, alCambiarTamanio } = opciones;
  if (tamanios && alCambiarTamanio) {
    const select = crearElemento('select');
    select.id = `paginador-tamanio-${contenedor.id || 'x'}`;
    select.setAttribute('aria-label', 'Registros por página');
    for (const t of tamanios) select.appendChild(new Option(String(t), String(t)));
    select.addEventListener('change', () => alCambiarTamanio(Number(select.value)));
    const lab = crearElemento('label', undefined, 'Por página');
    lab.setAttribute('for', select.id);
    const grupo = crearElemento('div', 'paginador-tamanio');
    grupo.append(lab, select);
    contenedor.appendChild(grupo);
    selectTamanio = select;
  }

  return {
    actualizar(info: InfoPagina): void {
      if (info.totalElementos === 0) {
        contenedor.hidden = true;
        return;
      }
      const totalPaginas = Math.max(1, info.totalPaginas);
      paginaActual = info.pagina;
      ultimaPagina = totalPaginas - 1;
      total.textContent = `${info.totalElementos} registro${info.totalElementos === 1 ? '' : 's'}`;
      indicador.textContent = `Página ${info.pagina + 1} de ${totalPaginas}`;
      btnAnterior.disabled = paginaActual <= 0;
      btnSiguiente.disabled = paginaActual >= ultimaPagina;
      if (selectTamanio && selectTamanio.value !== String(info.tamanio)) selectTamanio.value = String(info.tamanio);
      contenedor.hidden = false;
    },
    ocultar(): void {
      contenedor.hidden = true;
    },
  };
}
