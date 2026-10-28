export const API_BASE_URL = 'http://localhost:8080/api';

export const RUTAS = {
  facturas: `${API_BASE_URL}/facturas`,
  clientes: `${API_BASE_URL}/clientes`,
  puntosVenta: `${API_BASE_URL}/puntos-venta`,
  tiposMoneda: `${API_BASE_URL}/tipos-moneda`,
  listasPrecio: `${API_BASE_URL}/listas-precio`,
  rubros: `${API_BASE_URL}/rubros`,
  marcas: `${API_BASE_URL}/marcas`,
  emisor: `${API_BASE_URL}/emisor`,
} as const;
