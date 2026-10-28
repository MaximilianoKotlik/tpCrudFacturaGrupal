/** Selector tipado que falla fuerte si el elemento no existe. */
export function qs<T extends Element>(selector: string, raiz: ParentNode = document): T {
  const el = raiz.querySelector(selector);
  if (el === null) throw new Error(`No se encontró el elemento: ${selector}`);
  return el as T;
}

export function crearElemento<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  clases?: string,
  texto?: string,
): HTMLElementTagNameMap[K] {
  const el = document.createElement(tag);
  if (clases) el.className = clases;
  if (texto !== undefined) el.textContent = texto;
  return el;
}

export function limpiarHijos(nodo: Element): void {
  while (nodo.firstChild) nodo.removeChild(nodo.firstChild);
}

/** Muestra u oculta el <small class="error" data-error-for="id"> del campo. */
export function fijarErrorCampo(campo: { readonly id: string }, mensaje: string): void {
  const input = document.getElementById(campo.id);
  const small = document.querySelector(`small[data-error-for="${campo.id}"]`);
  if (input) input.setAttribute('aria-invalid', mensaje ? 'true' : 'false');
  if (small instanceof HTMLElement) {
    small.textContent = mensaje;
    small.hidden = mensaje.length === 0;
  }
}

/** Limpia todos los errores dentro de un formulario. */
export function limpiarErrores(form: HTMLFormElement): void {
  form.querySelectorAll('small.error').forEach((small) => {
    small.textContent = '';
    if (small instanceof HTMLElement) small.hidden = true;
  });
  form.querySelectorAll('[aria-invalid]').forEach((el) => {
    el.setAttribute('aria-invalid', 'false');
  });
}
