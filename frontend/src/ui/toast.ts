import { TOAST_MS } from '../config/constantes';
import { crearElemento, limpiarHijos, qs } from '../utils/dom';

export type TipoToast = 'exito' | 'error' | 'info';

const ICONOS: Record<TipoToast, string> = {
  exito: '<svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 0 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"/></svg>',
  error: '<svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" fill="currentColor"><path d="M8 1.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM7.25 4a.75.75 0 0 1 1.5 0v4a.75.75 0 0 1-1.5 0V4Zm.75 7.25a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"/></svg>',
  info: '<svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" fill="currentColor"><path d="M8 1.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM7.25 4a.75.75 0 0 1 1.5 0v4a.75.75 0 0 1-1.5 0V4Zm.75 7.25a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"/></svg>',
};

/** Crea el contenedor de toasts (aria-live) si no existe. */
export function initToasts(): void {
  let cont = document.getElementById('toasts');
  if (!cont) {
    cont = crearElemento('div');
    cont.id = 'toasts';
    cont.setAttribute('aria-live', 'polite');
    document.body.appendChild(cont);
  } else {
    limpiarHijos(cont);
  }
}

/** Toast no bloqueante con auto-cierre. El mensaje se inserta como texto (sin HTML). */
export function mostrarToast(tipo: TipoToast, mensaje: string): void {
  const cont = qs<HTMLElement>('#toasts');
  const toast = crearElemento('div', `toast toast-${tipo}`);
  toast.setAttribute('role', tipo === 'error' ? 'alert' : 'status');

  const icono = crearElemento('span', 'toast-icono');
  icono.setAttribute('aria-hidden', 'true');
  icono.innerHTML = ICONOS[tipo]; // solo SVG estático propio, nunca datos externos

  const texto = crearElemento('p', 'toast-texto', mensaje);

  const cerrar = crearElemento('button', 'toast-cerrar');
  cerrar.type = 'button';
  cerrar.setAttribute('aria-label', 'Cerrar aviso');
  cerrar.textContent = '×';
  cerrar.addEventListener('click', () => {
    toast.remove();
  });

  toast.append(icono, texto, cerrar);
  cont.appendChild(toast);

  window.setTimeout(() => {
    toast.classList.add('toast-salida');
    window.setTimeout(() => {
      toast.remove();
    }, 200);
  }, tipo === 'error' ? TOAST_MS * 2 : TOAST_MS); // los errores de ARCA llevan más texto para leer
}
