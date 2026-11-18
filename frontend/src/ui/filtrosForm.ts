import type { FacturaFiltrosDTO } from '../models/factura.dto';
import { ESTADO_INFO, TIPOS_COMPROBANTE } from '../config/constantes';
import { mensajeDeError } from '../services/httpClient';
import { qs } from '../utils/dom';
import { construirFiltros, validarRangoFechas } from '../utils/validaciones';
import { explorarClientes } from './explorerClientes';
import { SVG_LUPA } from './iconos';

export type FiltrosCallbacks = {
  readonly alBuscar: (filtros: FacturaFiltrosDTO) => void;
  readonly alLimpiar: () => void;
};

/** Conecta el formulario de filtros: valida desde ≤ hasta y emite el DTO. */
export function initFiltrosForm(form: HTMLFormElement, cb: FiltrosCallbacks): { limpiar: () => void } {
  const desde = qs<HTMLInputElement>('#filtro-desde', form);
  const hasta = qs<HTMLInputElement>('#filtro-hasta', form);
  const estado = qs<HTMLSelectElement>('#filtro-estado', form);
  const monto = qs<HTMLInputElement>('#filtro-monto', form);
  const clienteTexto = qs<HTMLInputElement>('#filtro-cliente', form);
  const btnCliente = qs<HTMLButtonElement>('#btn-filtro-cliente', form);
  const btnQuitarCliente = qs<HTMLButtonElement>('#btn-filtro-cliente-quitar', form);
  const tipo = qs<HTMLSelectElement>('#filtro-tipo', form);
  const cae = qs<HTMLInputElement>('#filtro-cae', form);
  for (const t of TIPOS_COMPROBANTE) tipo.appendChild(new Option(t, t));
  btnCliente.innerHTML = SVG_LUPA; // SVG estático propio
  /** Cliente elegido con el explorer (el input solo muestra su nombre). */
  let clienteId: number | null = null;

  function fijarCliente(id: number | null, texto: string): void {
    clienteId = id;
    clienteTexto.value = texto;
    btnQuitarCliente.hidden = id === null;
  }
  btnCliente.addEventListener('click', () => {
    void explorarClientes().then(
      (elegido) => {
        if (elegido) fijarCliente(elegido.id, `${elegido.denominacion} · ${elegido.cuitCuil}`);
      },
      (e: unknown) => {
        mostrarError(mensajeDeError(e));
      },
    );
  });
  btnQuitarCliente.addEventListener('click', () => {
    fijarCliente(null, '');
    btnCliente.focus();
  });
  // Opciones generadas desde el mapa de estados (junto a "Todos", que está en el HTML).
  for (const [valor, info] of Object.entries(ESTADO_INFO)) estado.appendChild(new Option(info.etiqueta, valor));
  const error = qs<HTMLElement>('#error-filtros');
  const btnLimpiar = qs<HTMLButtonElement>('#btn-limpiar', form);

  function mostrarError(mensaje: string): void {
    error.textContent = mensaje;
    error.hidden = mensaje.length === 0;
  }

  function leer(): FacturaFiltrosDTO | null {
    const msgRango = validarRangoFechas(desde.value, hasta.value);
    if (msgRango) {
      mostrarError(msgRango);
      hasta.focus();
      return null;
    }
    if (monto.value.trim() !== '' && !(Number(monto.value) >= 0)) {
      mostrarError('El monto mínimo debe ser un número mayor o igual a 0.');
      monto.focus();
      return null;
    }
    mostrarError('');
    return construirFiltros({
      desde: desde.value,
      hasta: hasta.value,
      estado: estado.value,
      monto: monto.value,
      clienteId,
      tipo: tipo.value,
      cae: cae.value,
    });
  }

  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    const filtros = leer();
    if (filtros) cb.alBuscar(filtros);
  });

  function limpiar(): void {
    form.reset();
    fijarCliente(null, '');
    mostrarError('');
    cb.alLimpiar();
  }
  btnLimpiar.addEventListener('click', limpiar);

  return { limpiar };
}
