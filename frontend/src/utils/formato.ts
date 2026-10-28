const monedaAR = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function formatearMoneda(valor: number): string {
  if (!Number.isFinite(valor)) return monedaAR.format(0);
  return monedaAR.format(valor);
}

/** '2026-10-12' o '2026-10-12T00:00:00...' → '12/10/2026'.
 *  Se parsea el string (no new Date) para evitar corrimientos por zona horaria. */
export function formatearFechaISO(iso: string): string {
  const fecha = iso.slice(0, 10);
  const partes = fecha.split('-');
  if (partes.length !== 3) return iso;
  const [anio, mes, dia] = partes as [string, string, string];
  return `${dia}/${mes}/${anio}`;
}

const porcentajeAR = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });

/** 10.5 → '10,5 %'. */
export function formatearPorcentaje(valor: number): string {
  return `${porcentajeAR.format(valor)} %`;
}

export function aNumero(valor: string, defecto = 0): number {
  if (valor.trim() === '') return defecto;
  const n = Number(valor);
  return Number.isFinite(n) ? n : defecto;
}

/** "N° 123" o, mientras no tiene número (todavía no fue emitida), "de <cliente> por <total>". Nunca el id interno. */
export function etiquetaFactura(f: {
  readonly numeroFactura: number | null;
  readonly clienteDenominacion: string;
  readonly importeTotal: number;
}): string {
  return f.numeroFactura === null
    ? `de ${f.clienteDenominacion} por ${formatearMoneda(f.importeTotal)}`
    : `N° ${f.numeroFactura}`;
}
