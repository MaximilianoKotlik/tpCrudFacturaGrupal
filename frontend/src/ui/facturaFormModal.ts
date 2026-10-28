import type { PuntoVentaDTO, TipoMonedaDTO } from '../models/catalogo.dto';
import type { EmisorDTO } from '../models/emisor.dto';
import type { ListaPrecioDTO } from '../models/listaPrecio.dto';
import type { FacturaDTO, FacturaGuardarDTO } from '../models/factura.dto';
import type { FacturaService } from '../services/FacturaService';
import { ESTADO_INFO, esEditable } from '../config/constantes';
import { crearItemsStore } from '../state/itemsStore';
import { mensajeDeError } from '../services/httpClient';
import { crearElemento, fijarErrorCampo, limpiarErrores, limpiarHijos } from '../utils/dom';
import { formatearMoneda, formatearPorcentaje } from '../utils/formato';
import { validarCabecera, validarItem } from '../utils/validaciones';
import { confirmar } from './confirmDialog';
import { explorarClientes } from './explorerClientes';
import { SVG_LUPA } from './iconos';
import { initEditorItems } from './itemsEditor';

export type FormCatalogos = {
  readonly puntosVenta: readonly PuntoVentaDTO[];
  readonly tiposMoneda: readonly TipoMonedaDTO[];
  readonly listasPrecio: readonly ListaPrecioDTO[];
  readonly emisor: EmisorDTO;
};

export type ModoFormulario =
  | { readonly modo: 'crear' }
  | { readonly modo: 'editar'; readonly factura: FacturaDTO };

type OpcionSelect = { readonly id: number; readonly texto: string };

/** Cliente elegido; su condición IVA se muestra y determina el tipo de comprobante en el servidor. */
type ClienteElegido = OpcionSelect & { readonly condicionIva: string };

let dialogo: HTMLDialogElement | null = null;

function campo(
  id: string,
  etiqueta: string,
  control: HTMLElement,
  clase = 'campo',
): HTMLElement {
  const cont = crearElemento('div', clase);
  const lab = crearElemento('label', undefined, etiqueta);
  lab.setAttribute('for', id);
  const err = crearElemento('small', 'error');
  err.dataset['errorFor'] = id;
  err.hidden = true;
  cont.append(lab, control, err);
  return cont;
}

function crearSelect(id: string, opciones: readonly OpcionSelect[], actual: number | null): HTMLSelectElement {
  const sel = crearElemento('select');
  sel.id = id;
  sel.appendChild(new Option('Seleccionar…', ''));
  for (const op of opciones) sel.appendChild(new Option(op.texto, String(op.id)));
  if (actual !== null) sel.value = String(actual);
  return sel;
}

/** Modal único de factura (crear / editar) construido por JS.
 *  Guarda por sí mismo (POST o PUT) y resuelve la factura resultante, o null si se cancela. */
export function abrirFacturaForm(
  config: ModoFormulario,
  catalogos: FormCatalogos,
  servicio: FacturaService,
): Promise<FacturaDTO | null> {
  if (!dialogo) {
    dialogo = crearElemento('dialog');
    dialogo.id = 'dialogo-factura-form';
    document.body.appendChild(dialogo);
  }
  const dlg = dialogo;
  limpiarHijos(dlg);

  return new Promise<FacturaDTO | null>((resolver) => {
    const ctrl = new AbortController();
    let resuelto = false;
    let sucio = false;
    let guardando = false;

    function cerrar(valor: FacturaDTO | null): void {
      if (resuelto) return;
      resuelto = true;
      ctrl.abort();
      dlg.close();
      resolver(valor);
    }
    async function intentarCerrar(): Promise<void> {
      if (resuelto || guardando) return;
      if (sucio) {
        const salir = await confirmar({
          titulo: 'Descartar cambios',
          mensaje: 'Tenés cambios sin guardar. ¿Cerrar y descartarlos?',
          textoAccion: 'Descartar',
          peligrosa: true,
        });
        if (!salir) return;
      }
      cerrar(null);
    }

    dlg.addEventListener(
      'cancel',
      (ev) => {
        ev.preventDefault();
        void intentarCerrar();
      },
      { signal: ctrl.signal },
    );
    dlg.addEventListener(
      'click',
      (ev) => {
        if (ev.target === dlg) void intentarCerrar();
      },
      { signal: ctrl.signal },
    );

    /** Arma encabezado + cuerpo + pie dentro de un <form> y lo deja en el diálogo. */
    function montar(titulo: string, cuerpo: readonly Node[], pie: readonly Node[]): HTMLFormElement {
      limpiarHijos(dlg);
      const form = crearElemento('form');
      form.noValidate = true;
      const h2 = crearElemento('h2', undefined, titulo);
      h2.id = 'ff-titulo';
      dlg.setAttribute('aria-labelledby', 'ff-titulo');
      const btnX = crearElemento('button', 'btn-icono', '×');
      btnX.type = 'button';
      btnX.setAttribute('aria-label', 'Cerrar formulario');
      btnX.addEventListener('click', () => void intentarCerrar());
      const encabezado = crearElemento('div', 'modal-encabezado');
      encabezado.append(h2, btnX);
      const divCuerpo = crearElemento('div', 'modal-cuerpo');
      divCuerpo.append(...cuerpo);
      const divPie = crearElemento('div', 'modal-pie');
      divPie.append(...pie);
      form.append(encabezado, divCuerpo, divPie);
      dlg.appendChild(form);
      return form;
    }

    function botonCerrar(texto: string): HTMLButtonElement {
      const b = crearElemento('button', 'btn btn-secundario', texto);
      b.type = 'button';
      b.addEventListener('click', () => void intentarCerrar());
      return b;
    }

    function mostrarMensaje(titulo: string, mensaje: string, esError: boolean, reintentar?: () => void): void {
      const cuerpo: Node[] = [crearElemento('p', esError ? 'error' : undefined, mensaje)];
      if (reintentar) {
        const b = crearElemento('button', 'btn btn-secundario', 'Reintentar');
        b.type = 'button';
        b.addEventListener('click', reintentar);
        cuerpo.push(b);
      }
      montar(titulo, cuerpo, [botonCerrar('Cerrar')]);
    }

    function renderForm(titulo: string, dto: FacturaDTO | null, clienteInicial: ClienteElegido | null): void {
      const store = crearItemsStore(catalogos.emisor.discriminaIva);
      if (dto) {
        for (const det of dto.detalles ?? []) {
          const item = store.agregar();
          store.actualizar(item.uid, {
            articuloId: det.articuloId,
            articuloEtiqueta: det.descripcion,
            descripcion: det.descripcion,
            cantidad: det.cantidad,
            porcentajeBonificacion: det.porcentajeBonificacion,
            precioReferencial: det.precioUnitario,
            alicuotaPorcentaje: det.alicuotaPorcentaje,
          });
        }
      }
      let cliente: ClienteElegido | null = clienteInicial;
      let listaPrevia: string = dto ? String(dto.listaPrecioId) : '';

      // --- Cabecera ---
      const iCliente = crearElemento('input');
      iCliente.id = 'ff-cliente';
      iCliente.type = 'text';
      iCliente.readOnly = true;
      iCliente.placeholder = 'Ningún cliente seleccionado';
      iCliente.value = cliente?.texto ?? '';
      const btnCliente = crearElemento('button', 'btn btn-secundario btn-lupa');
      btnCliente.type = 'button';
      btnCliente.title = 'Buscar cliente';
      btnCliente.setAttribute('aria-label', 'Buscar cliente');
      btnCliente.innerHTML = SVG_LUPA; // SVG estático propio
      const selectorCliente = crearElemento('div', 'selector');
      selectorCliente.append(iCliente, btnCliente);

      const sLista = crearSelect(
        'ff-lista',
        catalogos.listasPrecio.map((l) => ({ id: l.id, texto: `${l.codigo} · ${l.denominacion}` })),
        dto?.listaPrecioId ?? null,
      );
      // La condición IVA es del cliente: solo se muestra, no se elige.
      const iCondIva = crearElemento('input');
      iCondIva.id = 'ff-condiva';
      iCondIva.type = 'text';
      iCondIva.readOnly = true;
      iCondIva.placeholder = 'Se completa al elegir el cliente';
      iCondIva.value = cliente?.condicionIva ?? '';
      const sPv = crearSelect(
        'ff-pv',
        catalogos.puntosVenta.map((p) => ({ id: p.id, texto: p.descripcion })),
        dto?.puntoVentaId ?? null,
      );
      const sMoneda = crearSelect(
        'ff-moneda',
        catalogos.tiposMoneda.map((t) => ({ id: t.id, texto: `${t.denominacion} (${t.simbolo})` })),
        dto?.tipoMonedaId ?? null,
      );
      // Cotización informada por ARCA al emitir; solo lectura y solo si la moneda no es pesos (cotización 1).
      const cotizacion = dto?.cotizacion ?? null;
      const iCotiz = crearElemento('input');
      iCotiz.id = 'ff-cotiz';
      iCotiz.type = 'text';
      iCotiz.readOnly = true;
      iCotiz.value = cotizacion !== null && cotizacion !== 1 ? String(cotizacion) : '';
      const iObs = crearElemento('input');
      iObs.id = 'ff-obs';
      iObs.type = 'text';
      iObs.maxLength = 255;
      iObs.placeholder = 'Opcional';
      iObs.value = dto?.observaciones ?? '';

      const grid = crearElemento('div', 'form-grid');
      grid.append(
        campo('ff-cliente', 'Cliente', selectorCliente),
        campo('ff-lista', 'Lista de precios', sLista),
        campo('ff-condiva', 'Condición IVA del cliente', iCondIva),
        campo('ff-pv', 'Punto de venta', sPv),
        campo('ff-moneda', 'Tipo de moneda', sMoneda),
        ...(iCotiz.value !== '' ? [campo('ff-cotiz', 'Cotización (ARCA)', iCotiz)] : []),
        campo('ff-obs', 'Observaciones', iObs, 'campo campo-ancho'),
      );
      const fsCabecera = crearElemento('fieldset');
      fsCabecera.append(crearElemento('legend', undefined, 'Comprobante'), grid);

      // --- Renglones ---
      const btnAgregar = crearElemento('button', 'btn btn-secundario', 'Agregar renglón');
      btnAgregar.type = 'button';
      const aviso = crearElemento('p', 'nota', 'Elegí una lista de precios para poder agregar renglones.');
      const errItems = crearElemento('p', 'error');
      errItems.hidden = true;
      const trHead = crearElemento('tr');
      const encabezados = ['Artículo', 'Descripción', 'Cant.', 'Bonif. %', 'Precio lista'];
      if (catalogos.emisor.discriminaIva) encabezados.push('IVA');
      encabezados.push('Subtotal', '');
      for (const h of encabezados) {
        trHead.appendChild(crearElemento('th', h === '' || h === 'Artículo' || h === 'Descripción' ? undefined : 'num', h));
      }
      const thead = crearElemento('thead');
      thead.appendChild(trHead);
      const tbody = crearElemento('tbody');
      const tabla = crearElemento('table');
      tabla.id = 'tabla-items';
      tabla.append(thead, tbody);
      const scroll = crearElemento('div', 'tabla-scroll');
      scroll.appendChild(tabla);
      const fsRenglones = crearElemento('fieldset');
      fsRenglones.append(crearElemento('legend', undefined, 'Renglones'), btnAgregar, aviso, errItems, scroll);

      // --- Totales ---
      const dl = crearElemento('dl');
      const fila = (dt: string, valor: number, clase?: string): HTMLElement => {
        const d = crearElemento('div', clase);
        d.append(crearElemento('dt', undefined, dt), crearElemento('dd', undefined, formatearMoneda(valor)));
        return d;
      };
      const aside = crearElemento('aside', 'totales');
      aside.setAttribute('aria-label', 'Totales de la factura');
      aside.setAttribute('aria-live', 'polite');
      aside.append(dl);

      const errGeneral = crearElemento('p', 'error');
      errGeneral.hidden = true;

      const btnCancelar = botonCerrar('Cancelar');
      const btnGuardar = crearElemento('button', 'btn btn-primario');
      btnGuardar.type = 'submit';
      const txtGuardar = crearElemento('span', 'btn-texto', config.modo === 'crear' ? 'Guardar factura' : 'Guardar cambios');
      btnGuardar.appendChild(txtGuardar);

      const form = montar(titulo, [fsCabecera, fsRenglones, aside, errGeneral], [btnCancelar, btnGuardar]);

      // --- Lógica ---
      function listaElegida(): number | null {
        return sLista.value ? Number(sLista.value) : null;
      }
      function actualizarTotales(): void {
        const t = store.totales();
        const filas: HTMLElement[] = [fila('Neto', t.neto)];
        // IVA por alícuota solo si el emisor discrimina; con varias alícuotas se suma además el total de IVA.
        for (const g of t.porAlicuota) filas.push(fila(`IVA ${formatearPorcentaje(g.alicuota)}`, g.iva));
        if (t.porAlicuota.length > 1) filas.push(fila('IVA total', t.iva));
        filas.push(fila('Total', t.total, 'total-final'));
        dl.replaceChildren(...filas);
      }
      function sincronizarLista(): void {
        const hay = listaElegida() !== null;
        btnAgregar.disabled = !hay;
        aviso.hidden = hay;
      }

      const editor = initEditorItems(tbody, errItems, store, { listaPrecioId: listaElegida, alCambiar: actualizarTotales });
      actualizarTotales();
      sincronizarLista();

      function marcarSucio(): void {
        sucio = true;
      }
      form.addEventListener('input', marcarSucio);
      form.addEventListener('change', marcarSucio);

      sLista.addEventListener('change', () => {
        const nueva = sLista.value;
        if (store.obtener().length === 0) {
          listaPrevia = nueva;
          sincronizarLista();
          return;
        }
        // Cambiar de lista invalida precios y artículos ya cargados: confirmar y vaciar.
        void confirmar({
          titulo: 'Cambiar lista de precios',
          mensaje: 'Al cambiar la lista de precios se eliminarán los renglones cargados. ¿Continuar?',
          textoAccion: 'Cambiar y vaciar',
          peligrosa: true,
        }).then((ok) => {
          if (ok) {
            listaPrevia = nueva;
            editor.limpiar();
          } else {
            sLista.value = listaPrevia;
          }
          sincronizarLista();
        });
      });

      btnCliente.addEventListener('click', () => {
        void explorarClientes().then(
          (elegido) => {
            if (!elegido) return;
            cliente = {
              id: elegido.id,
              texto: `${elegido.denominacion} · ${elegido.cuitCuil}`,
              condicionIva: elegido.condicionIvaDenominacion,
            };
            iCliente.value = cliente.texto;
            iCondIva.value = cliente.condicionIva;
            marcarSucio();
            fijarErrorCampo(iCliente, '');
          },
          (error: unknown) => {
            errGeneral.textContent = mensajeDeError(error);
            errGeneral.hidden = false;
          },
        );
      });
      btnAgregar.addEventListener('click', () => {
        marcarSucio();
        editor.agregarFila();
      });

      async function guardar(): Promise<void> {
        if (guardando) return;
        limpiarErrores(form);
        errGeneral.hidden = true;

        const errores = validarCabecera({
          clienteId: cliente ? String(cliente.id) : '',
          listaPrecioId: sLista.value,
          puntoVentaId: sPv.value,
          monedaId: sMoneda.value,
        });
        const idsConError = Object.keys(errores);
        for (const id of idsConError) fijarErrorCampo({ id }, errores[id] ?? '');
        if (idsConError.length > 0) {
          const primero = idsConError[0];
          if (primero) document.getElementById(primero)?.focus();
          return;
        }

        const items = store.obtener();
        if (items.length === 0) {
          errItems.textContent = 'La factura debe tener al menos 1 renglón.';
          errItems.hidden = false;
          btnAgregar.focus();
          return;
        }
        for (const [i, item] of items.entries()) {
          const m = validarItem(item);
          if (m) {
            errItems.textContent = `Renglón ${i + 1}: ${m}`;
            errItems.hidden = false;
            btnAgregar.focus();
            return;
          }
        }

        const detalles: FacturaGuardarDTO['detalles'] = items.map((item) => ({
          articuloId: item.articuloId,
          descripcion: item.descripcion.trim(),
          cantidad: item.cantidad,
          porcentajeBonificacion: item.porcentajeBonificacion,
        }));
        const cuerpo: FacturaGuardarDTO = {
          observaciones: iObs.value.trim(),
          clienteId: (cliente as OpcionSelect).id,
          listaPrecioId: Number(sLista.value),
          puntoVentaId: Number(sPv.value),
          tipoMonedaId: Number(sMoneda.value),
          detalles,
        };

        guardando = true;
        btnGuardar.disabled = true;
        btnGuardar.classList.add('cargando');
        txtGuardar.textContent = 'Guardando…';
        try {
          const resultado =
            config.modo === 'crear'
              ? await servicio.crearFactura(cuerpo)
              : await servicio.actualizarFactura(config.factura.id, cuerpo);
          guardando = false;
          cerrar(resultado);
        } catch (error: unknown) {
          guardando = false;
          errGeneral.textContent = mensajeDeError(error);
          errGeneral.hidden = false;
          btnGuardar.disabled = false;
          btnGuardar.classList.remove('cargando');
          txtGuardar.textContent = config.modo === 'crear' ? 'Guardar factura' : 'Guardar cambios';
        }
      }
      form.addEventListener('submit', (ev) => {
        ev.preventDefault();
        void guardar();
      });
    }

    // --- Arranque ---
    if (config.modo === 'crear') {
      renderForm('Nueva factura', null, null);
      dlg.showModal();
      return;
    }

    const factura = config.factura;
    const tituloEditar =
      factura.numeroFactura === null ? 'Editar factura (sin emitir)' : `Editar factura N° ${factura.numeroFactura}`;

    function cargar(): void {
      mostrarMensaje(tituloEditar, 'Cargando factura…', false);
      servicio.obtenerFactura(factura.id).then(
        (dto) => {
          if (resuelto) return;
          if (!esEditable(dto.estado)) {
            mostrarMensaje(
              tituloEditar,
              `La factura está en estado ${ESTADO_INFO[dto.estado].etiqueta} y no se puede editar.`,
              true,
            );
            return;
          }
          const cliente: ClienteElegido = {
            id: dto.clienteId,
            texto: dto.clienteDenominacion,
            condicionIva: dto.condicionIvaDenominacion,
          };
          renderForm(tituloEditar, dto, cliente);
        },
        (error: unknown) => {
          if (resuelto) return;
          mostrarMensaje(tituloEditar, mensajeDeError(error), true, cargar);
        },
      );
    }
    cargar();
    dlg.showModal();
  });
}
