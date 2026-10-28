import { TAMANIO_EXPLORER } from '../config/constantes';
import type { ClienteDTO, ClienteOrden } from '../models/cliente.dto';
import { ClienteService } from '../services/ClienteService';
import { abrirExplorer, type OrdenExplorer } from './explorerModal';

const servicio = new ClienteService();

const ORDENES: readonly OrdenExplorer[] = [
  { valor: 'den-asc', texto: 'Denominación A-Z', orden: 'denominacion', dir: 'asc' },
  { valor: 'den-desc', texto: 'Denominación Z-A', orden: 'denominacion', dir: 'desc' },
  { valor: 'cuit-asc', texto: 'CUIT/CUIL ascendente', orden: 'cuit', dir: 'asc' },
  { valor: 'cuit-desc', texto: 'CUIT/CUIL descendente', orden: 'cuit', dir: 'desc' },
];

/** Explorer de clientes: búsqueda por denominación o CUIT/CUIL, orden A-Z/Z-A y por CUIT. */
export function explorarClientes(): Promise<ClienteDTO | null> {
  return abrirExplorer<ClienteDTO>({
    titulo: 'Buscar cliente',
    placeholderBusqueda: 'Denominación o CUIT/CUIL…',
    columnas: [
      { titulo: 'Denominación', valor: (c) => c.denominacion },
      { titulo: 'CUIT/CUIL', valor: (c) => c.cuitCuil },
      { titulo: 'Condición IVA', valor: (c) => c.condicionIvaDenominacion },
    ],
    filtros: [],
    ordenes: ORDENES,
    buscar: (p) =>
      servicio.buscar({
        q: p.q,
        orden: p.orden.orden as ClienteOrden,
        dir: p.orden.dir,
        pagina: p.pagina,
        tamanio: TAMANIO_EXPLORER,
      }),
    mensajeVacio: 'No se encontraron clientes.',
  });
}
