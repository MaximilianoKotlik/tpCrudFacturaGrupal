import type { FacturaFiltrosDTO } from '../models/factura.dto';
import { ESTADO_INFO } from '../config/constantes';
import { qs } from '../utils/dom';
import { construirFiltros, validarRangoFechas } from '../utils/validaciones';

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
    return construirFiltros(desde.value, hasta.value, estado.value, monto.value);
  }

  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    const filtros = leer();
    if (filtros) cb.alBuscar(filtros);
  });

  function limpiar(): void {
    form.reset();
    mostrarError('');
    cb.alLimpiar();
  }
  btnLimpiar.addEventListener('click', limpiar);

  return { limpiar };
}
