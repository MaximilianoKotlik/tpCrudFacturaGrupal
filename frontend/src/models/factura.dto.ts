import type { EstadoFactura } from '../config/constantes';

/** Renglón. En entrada (POST/PUT) solo se envían articuloId, descripcion, cantidad y
 *  porcentajeBonificacion; el resto lo calcula el servidor y vuelve en las respuestas. */
export type FacturaDetalleDTO = {
    articuloId: number;
    descripcion: string;
    /** Entera, >= 1. */
    cantidad: number;
    porcentajeBonificacion: number;
    precioUnitario: number;
    alicuotaPorcentaje: number;
    importeNeto: number;
    importeIva: number;
    importeSubtotal: number;
};

/** Parte del renglón que se envía al guardar. */
export type FacturaDetalleEntradaDTO = Pick<
    FacturaDetalleDTO,
    'articuloId' | 'descripcion' | 'cantidad' | 'porcentajeBonificacion'
>;

/** Ítem en memoria del formulario. `precioReferencial` y `alicuotaPorcentaje` son solo
 *  informativos (cálculo en pantalla): el servidor recalcula desde la lista de precios.
 *  `articuloEtiqueta` es el texto que se muestra para el artículo elegido. */
export type ItemEnMemoria = FacturaDetalleEntradaDTO & {
    precioReferencial: number;
    alicuotaPorcentaje: number;
    articuloEtiqueta: string;
};

/** Alta (POST) y actualización (PUT /api/facturas/{id}). La condición IVA sale del cliente
 *  y las fechas las define el servidor. Si se envía `detalles`, reemplaza todos los renglones. */
export type FacturaGuardarDTO = {
    observaciones: string;
    clienteId: number;
    listaPrecioId: number;
    puntoVentaId: number;
    tipoMonedaId: number;
    detalles: FacturaDetalleEntradaDTO[];
};

export type TipoComprobante = 'A' | 'B' | 'C';

/** Factura devuelta por el servidor (listado, GET/{id}, POST, PUT, emitir). */
export type FacturaDTO = {
    id: number;
    /** null mientras la factura no fue emitida. */
    numeroFactura: number | null;
    estado: EstadoFactura;
    tipoComprobante: TipoComprobante;
    /** ISO datetime. */
    fechaCreacion: string;
    /** ISO date; null hasta que ARCA aprueba. */
    fechaEmision: string | null;
    clienteId: number;
    clienteDenominacion: string;
    listaPrecioId: number;
    listaPrecioDenominacion: string;
    condicionIvaId: number;
    condicionIvaDenominacion: string;
    puntoVentaId: number;
    puntoVentaDescripcion: string;
    tipoMonedaId: number;
    /** Cotización ARCA al emitir (pesos = 1); null mientras no se emitió con éxito. */
    cotizacion: number | null;
    importeNeto: number;
    importeIva: number;
    importeTotal: number;
    cae: string | null;
    caeFechaVencimiento: string | null;
    /** Motivo del rechazo de ARCA (estado ERROR_AFIP). */
    motivoRechazo: string | null;
    observaciones: string | null;
    cantidadItems: number;
    /** null en el listado; presente en GET /api/facturas/{id}. */
    detalles: FacturaDetalleDTO[] | null;
};

/** Filtros del listado; fechaDesde/fechaHasta aplican a la fecha de creación. */
export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: EstadoFactura;
    montoMinimo?: number;
}
