import { crearElemento, limpiarHijos } from '../utils/dom';

export type DatosConfirm = {
  readonly titulo: string;
  readonly mensaje: string;
  readonly textoAccion: string;
  readonly peligrosa?: boolean;
};

let dialogo: HTMLDialogElement | null = null;

/** Diálogo de confirmación propio con <dialog> nativo. Resuelve true/false. */
export function confirmar(datos: DatosConfirm): Promise<boolean> {
  if (!dialogo) {
    dialogo = crearElemento('dialog');
    dialogo.id = 'dialogo-confirm';
    document.body.appendChild(dialogo);
  }
  const dlg = dialogo;
  limpiarHijos(dlg);

  return new Promise<boolean>((resolver) => {
    let resuelto = false;
    function cerrar(valor: boolean): void {
      if (resuelto) return;
      resuelto = true;
      dlg.close();
      resolver(valor);
    }

    const titulo = crearElemento('h2', 'confirm-titulo', datos.titulo);
    titulo.id = 'confirm-titulo';
    dlg.setAttribute('aria-labelledby', 'confirm-titulo');

    const mensaje = crearElemento('p', 'confirm-mensaje', datos.mensaje);

    const pie = crearElemento('div', 'confirm-pie');
    const btnCancelar = crearElemento('button', 'btn btn-secundario', 'Cancelar');
    btnCancelar.type = 'button';
    const btnAccion = crearElemento('button', datos.peligrosa ? 'btn btn-peligro' : 'btn btn-primario', datos.textoAccion);
    btnAccion.type = 'button';
    pie.append(btnCancelar, btnAccion);
    dlg.append(titulo, mensaje, pie);

    btnCancelar.addEventListener('click', () => cerrar(false));
    btnAccion.addEventListener('click', () => cerrar(true));
    dlg.addEventListener('cancel', () => cerrar(false), { once: true });
    // Clic en el backdrop = cancelar
    dlg.addEventListener('click', (ev) => {
      if (ev.target === dlg) cerrar(false);
    });

    dlg.showModal();
    btnAccion.focus();
  });
}
