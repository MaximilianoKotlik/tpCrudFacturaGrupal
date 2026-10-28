/** Página devuelta por el servidor (common/PageResponse). `pagina` es 0-based. */
export type PageResponse<T> = {
  readonly contenido: readonly T[];
  readonly pagina: number;
  readonly tamanio: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
};

/** Metadatos de una página, sin el contenido (lo que necesita el paginador). */
export type InfoPagina = Omit<PageResponse<unknown>, 'contenido'>;
