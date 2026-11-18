import type { ErrorRespuestaDTO } from '../models/error.dto';

/** Error HTTP con el mensaje que devolvió el servidor (si lo hubo). */
export class HttpError extends Error {
  readonly status: number;
  readonly cuerpo: string;

  constructor(status: number, cuerpo: string, message: string) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.cuerpo = cuerpo;
  }
}

const MENSAJE_GENERICO = 'Ocurrió un problema inesperado. Intentá de nuevo en unos minutos.';

/** Texto para el usuario según el código HTTP, cuando el servidor no mandó un mensaje propio. */
function mensajePorEstado(status: number): string {
  if (status === 404) return 'No se encontró lo que se pidió. Es posible que ya no exista.';
  if (status === 401 || status === 403) return 'No tenés permiso para realizar esta acción.';
  if (status >= 500) return 'El sistema tuvo un problema. Intentá de nuevo en unos minutos.';
  return MENSAJE_GENERICO;
}

/** Solo se muestra el mensaje que el sistema preparó para el usuario; nunca páginas HTML ni texto técnico. */
function extraerMensaje(texto: string, status: number): string {
  try {
    const json = JSON.parse(texto.trim()) as Partial<ErrorRespuestaDTO> & { message?: unknown };
    const mensaje = typeof json.mensaje === 'string' ? json.mensaje.trim() : '';
    if (mensaje) return mensaje.length > 600 ? `${mensaje.slice(0, 600)}…` : mensaje;
  } catch {
    // No es JSON: se usa el mensaje según el código.
  }
  return mensajePorEstado(status);
}

/** Wrapper de fetch con manejo uniforme: nunca silencia errores. */
export async function peticion<T>(url: string, opciones?: RequestInit): Promise<T> {
  let respuesta: Response;
  try {
    respuesta = await fetch(url, opciones);
  } catch {
    throw new HttpError(0, '', 'No se pudo conectar con el sistema. Verificá tu conexión e intentá de nuevo.');
  }
  if (respuesta.status === 204) return undefined as T;
  const texto = await respuesta.text();
  if (!respuesta.ok) {
    throw new HttpError(respuesta.status, texto, extraerMensaje(texto, respuesta.status));
  }
  if (texto.trim().length === 0) return undefined as T;
  return JSON.parse(texto) as T;
}

/** Igual que `peticion` pero devuelve el cuerpo binario (p. ej. un PDF). */
export async function peticionArchivo(url: string): Promise<Blob> {
  let respuesta: Response;
  try {
    respuesta = await fetch(url);
  } catch {
    throw new HttpError(0, '', 'No se pudo conectar con el sistema. Verificá tu conexión e intentá de nuevo.');
  }
  if (!respuesta.ok) {
    const texto = await respuesta.text();
    throw new HttpError(respuesta.status, texto, extraerMensaje(texto, respuesta.status));
  }
  return respuesta.blob();
}

/** Mensaje legible para mostrar en un toast, sea cual sea el error. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpError) return error.message;
  return MENSAJE_GENERICO;
}

/** Arma "?a=1&b=2" omitiendo los valores vacíos o indefinidos ('' si no queda ninguno). */
export function construirQuery(params: Readonly<Record<string, string | number | undefined>>): string {
  const sp = new URLSearchParams();
  for (const [clave, valor] of Object.entries(params)) {
    if (valor === undefined || valor === '') continue;
    sp.append(clave, String(valor));
  }
  const texto = sp.toString();
  return texto ? `?${texto}` : '';
}
