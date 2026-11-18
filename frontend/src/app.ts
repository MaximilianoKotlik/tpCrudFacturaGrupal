import type { FacturaDTO, FacturaFiltrosDTO } from './models/factura.dto';
import { ESTADO, TAMANIOS_PAGINA } from './config/constantes';
import { obtenerCatalogos } from './services/CatalogoService';
import { FacturaService } from './services/FacturaService';
import { mensajeDeError } from './services/httpClient';
import { ListaPrecioService } from './services/ListaPrecioService';
import { etiquetaFactura } from './utils/formato';
import { qs } from './utils/dom';
import { confirmar } from './ui/confirmDialog';
import { abrirFacturaForm, type FormCatalogos } from './ui/facturaFormModal';
import { initTablaFacturas, type AccionFila } from './ui/facturasTable';
import { initFiltrosForm } from './ui/filtrosForm';
import { crearPaginador } from './ui/paginador';
import { initToasts, mostrarToast } from './ui/toast';

const servicio = new FacturaService();
const listasPrecio = new ListaPrecioService();

/** Orquestador: conecta filtros, tabla, modal y servicios. Sin estado global. */
export async function iniciarApp(): Promise<void> {
  initToasts();
  const tabla = initTablaFacturas({ alAccionar: (accion, factura) => void onAccion(accion, factura) });

  let catalogos: FormCatalogos;
  try {
    const [chicos, listas] = await Promise.all([obtenerCatalogos(), listasPrecio.listar()]);
    catalogos = { ...chicos, listasPrecio: listas };
  } catch (error: unknown) {
    mostrarFatal(mensajeDeError(error));
    return;
  }

  let filtros: FacturaFiltrosDTO = {};
  let pagina = 0;
  let tamanio: number = TAMANIOS_PAGINA[0];
  // Descarta respuestas viejas si se dispara otra carga mientras una sigue en vuelo.
  let secuencia = 0;

  const paginador = crearPaginador(qs<HTMLElement>('#paginador-facturas'), {
    tamanios: TAMANIOS_PAGINA,
    alCambiarPagina: (nueva) => {
      pagina = nueva;
      void cargarListado();
    },
    alCambiarTamanio: (nuevo) => {
      tamanio = nuevo;
      pagina = 0;
      void cargarListado();
    },
  });

  async function cargarListado(): Promise<void> {
    secuencia += 1;
    const mia = secuencia;
    tabla.carga();
    tabla.resumen(null);
    paginador.ocultar();
    try {
      const resp = await servicio.obtenerFacturas(filtros, pagina, tamanio);
      if (mia !== secuencia) return;
      if (resp.contenido.length === 0 && pagina > 0 && resp.totalPaginas > 0) {
        // La página pedida ya no existe (p. ej. se eliminó el último registro): ir a la última.
        pagina = resp.totalPaginas - 1;
        await cargarListado();
        return;
      }
      if (resp.contenido.length === 0) {
        tabla.vacio();
      } else {
        tabla.render(resp.contenido);
      }
      tabla.resumen(resp.totalElementos);
      paginador.actualizar(resp);
    } catch (error: unknown) {
      if (mia !== secuencia) return;
      tabla.error(mensajeDeError(error), () => void cargarListado());
      tabla.resumen(null);
    }
  }

  async function onAccion(accion: AccionFila, factura: FacturaDTO): Promise<void> {
    if (accion === 'editar') {
      const guardada = await abrirFacturaForm({ modo: 'editar', factura }, catalogos, servicio);
      if (!guardada) return;
      mostrarToast('exito', `Factura ${etiquetaFactura(guardada)} actualizada.`);
      await cargarListado();
      return;
    }
    if (accion === 'emitir') {
      const ok = await confirmar({
        titulo: 'Emitir factura',
        mensaje: `Se enviará la factura ${etiquetaFactura(factura)} a ARCA para su autorización. Una vez emitida no se podrá editar.`,
        textoAccion: 'Emitir',
      });
      if (!ok) return;
      tabla.emitiendo(factura.id);
      mostrarToast('exito', `Enviando la factura ${etiquetaFactura(factura)} a ARCA…`);
      try {
        const emitida = await servicio.emitirFactura(factura.id);
        if (emitida.estado === ESTADO.EMITIDA) {
          const cae = emitida.cae ? ` CAE: ${emitida.cae}.` : '';
          mostrarToast('exito', `Factura ${etiquetaFactura(emitida)} emitida.${cae}`);
        } else {
          // motivoRechazo trae un error u observación de ARCA por línea ("[código] mensaje").
          const motivo = emitida.motivoRechazo ? `\n${emitida.motivoRechazo}` : '';
          mostrarToast('error', `No se pudo emitir la factura ${etiquetaFactura(factura)}.${motivo}`);
        }
      } catch (error: unknown) {
        mostrarToast('error', mensajeDeError(error));
      }
      await cargarListado();
      return;
    }
    if (accion === 'imprimir') {
      // La pestaña se abre ya (dentro del clic) para que el navegador no la bloquee; el PDF se carga después.
      const pestania = window.open('', '_blank');
      try {
        const pdf = await servicio.obtenerPdf(factura.id);
        const url = URL.createObjectURL(pdf);
        if (pestania) {
          pestania.location.href = url;
        } else {
          window.open(url, '_blank');
        }
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      } catch (error: unknown) {
        pestania?.close();
        mostrarToast('error', mensajeDeError(error));
      }
      return;
    }
    if (accion === 'anular') {
      const ok = await confirmar({
        titulo: 'Anular factura',
        mensaje: `Se anulará la factura emitida ${etiquetaFactura(factura)}, como si se emitiera una nota de crédito. `
          + 'Seguirá en el listado en estado Anulada.',
        textoAccion: 'Anular',
        peligrosa: true,
      });
      if (!ok) return;
      try {
        await servicio.anularFactura(factura.id);
        mostrarToast('exito', `Factura ${etiquetaFactura(factura)} anulada.`);
        await cargarListado();
      } catch (error: unknown) {
        mostrarToast('error', mensajeDeError(error));
      }
      return;
    }
    const ok = await confirmar({
      titulo: 'Eliminar factura',
      mensaje: `Se eliminará la factura ${etiquetaFactura(factura)} y no se mostrará más en el listado.`,
      textoAccion: 'Eliminar',
      peligrosa: true,
    });
    if (!ok) return;
    try {
      await servicio.eliminarFactura(factura.id);
      mostrarToast('exito', `Factura ${etiquetaFactura(factura)} eliminada.`);
      await cargarListado();
    } catch (error: unknown) {
      mostrarToast('error', mensajeDeError(error));
    }
  }

  qs<HTMLButtonElement>('#btn-nueva').addEventListener('click', () => {
    void abrirFacturaForm({ modo: 'crear' }, catalogos, servicio).then(async (creada) => {
      if (!creada) return;
      mostrarToast('exito', 'Factura creada como PENDIENTE. Emitila cuando esté lista.');
      await cargarListado();
    });
  });
  initFiltrosForm(qs<HTMLFormElement>('#form-filtros'), {
    alBuscar: (f) => {
      filtros = f;
      pagina = 0;
      void cargarListado();
    },
    alLimpiar: () => {
      filtros = {};
      pagina = 0;
      void cargarListado();
    },
  });

  await cargarListado();
}

function mostrarFatal(mensaje: string): void {
  const fatal = qs<HTMLElement>('#fatal');
  fatal.hidden = false;
  fatal.textContent = `No se pudo iniciar la app: ${mensaje}`;
}
