import { TAMANIO_EXPLORER } from '../config/constantes';
import type { ArticuloListaDTO, ArticuloOrden, MarcaDTO, RubroDTO } from '../models/listaPrecio.dto';
import { ArticuloService } from '../services/ArticuloService';
import { formatearMoneda, formatearPorcentaje } from '../utils/formato';
import { abrirExplorer, type FiltroExplorer, type OrdenExplorer } from './explorerModal';

const servicio = new ArticuloService();

const ORDENES: readonly OrdenExplorer[] = [
  { valor: 'cod-asc', texto: 'Código ascendente', orden: 'codigo', dir: 'asc' },
  { valor: 'cod-desc', texto: 'Código descendente', orden: 'codigo', dir: 'desc' },
  { valor: 'den-asc', texto: 'Nombre A-Z', orden: 'denominacion', dir: 'asc' },
  { valor: 'den-desc', texto: 'Nombre Z-A', orden: 'denominacion', dir: 'desc' },
  { valor: 'precio-asc', texto: 'Precio menor a mayor', orden: 'precio', dir: 'asc' },
  { valor: 'precio-desc', texto: 'Precio mayor a menor', orden: 'precio', dir: 'desc' },
];

let filtrosCache: Promise<readonly FiltroExplorer[]> | null = null;

function opciones(items: readonly (RubroDTO | MarcaDTO)[]): FiltroExplorer['opciones'] {
  return [{ valor: '', texto: 'Todos' }, ...items.map((i) => ({ valor: String(i.id), texto: i.denominacion }))];
}

/** Rubros y marcas se piden una sola vez; si falla se reintenta en la próxima apertura. */
function obtenerFiltros(): Promise<readonly FiltroExplorer[]> {
  if (!filtrosCache) {
    filtrosCache = Promise.all([servicio.rubros(), servicio.marcas()]).then(([rubros, marcas]) => [
      { clave: 'rubroId', etiqueta: 'Rubro', opciones: opciones(rubros) },
      { clave: 'marcaId', etiqueta: 'Marca', opciones: opciones(marcas) },
    ]);
    filtrosCache.catch(() => {
      filtrosCache = null;
    });
  }
  return filtrosCache;
}

function aIdOpcional(valor: string | undefined): number | undefined {
  return valor ? Number(valor) : undefined;
}

/** Explorer de artículos de una lista de precios: búsqueda por código/denominación,
 *  filtros por rubro y marca, orden por código/nombre/precio. Muestra el precio neto de la lista
 *  y, si el emisor discrimina IVA, la alícuota del artículo. */
export async function explorarArticulos(listaPrecioId: number, discriminaIva: boolean): Promise<ArticuloListaDTO | null> {
  const filtros = await obtenerFiltros();
  return abrirExplorer<ArticuloListaDTO>({
    titulo: 'Buscar artículo',
    placeholderBusqueda: 'Código o denominación…',
    columnas: [
      { titulo: 'Código', valor: (a) => a.codigo },
      { titulo: 'Denominación', valor: (a) => a.denominacion },
      { titulo: 'Rubro', valor: (a) => a.rubro ?? '—' },
      { titulo: 'Marca', valor: (a) => a.marca ?? '—' },
      { titulo: 'Precio', valor: (a) => formatearMoneda(a.precioVenta), numerica: true },
      ...(discriminaIva
        ? [{ titulo: 'IVA', valor: (a: ArticuloListaDTO) => formatearPorcentaje(a.alicuotaPorcentaje), numerica: true }]
        : []),
    ],
    filtros,
    ordenes: ORDENES,
    buscar: (p) =>
      servicio.listarPorLista(listaPrecioId, {
        q: p.q,
        rubroId: aIdOpcional(p.filtros['rubroId']),
        marcaId: aIdOpcional(p.filtros['marcaId']),
        orden: p.orden.orden as ArticuloOrden,
        dir: p.orden.dir,
        pagina: p.pagina,
        tamanio: TAMANIO_EXPLORER,
      }),
    mensajeVacio: 'No se encontraron artículos en esta lista de precios.',
  });
}
