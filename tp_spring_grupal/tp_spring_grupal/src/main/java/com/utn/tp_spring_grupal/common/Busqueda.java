package com.utn.tp_spring_grupal.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Utilidades chicas compartidas por los explorers (búsqueda por texto y orden). */
public final class Busqueda {

    private Busqueda() {
    }

    public static boolean hayTexto(String q) {
        return q != null && !q.isBlank();
    }

    /** Patrón LIKE "contains" en minúsculas (comparar contra lower(columna)). */
    public static String patron(String q) {
        return "%" + q.trim().toLowerCase() + "%";
    }

    /** "desc" (sin importar mayúsculas) → DESC; cualquier otro valor → ASC. */
    public static Sort.Direction direccion(String dir) {
        return "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    public static final int TAMANIO_POR_DEFECTO = 10;
    public static final int TAMANIO_MAXIMO = 100;

    /**
     * Pageable seguro: page &lt; 0 → 0; size &lt; 1 → 10 (default); size &gt; 100 → 100.
     * Agrega el id como desempate para que el orden (y por lo tanto la paginación) sea estable.
     */
    public static Pageable pageable(int page, int size, Sort sort) {
        int pagina = Math.max(page, 0);
        int tamanio = size < 1 ? TAMANIO_POR_DEFECTO : Math.min(size, TAMANIO_MAXIMO);
        Sort orden = sort.getOrderFor("id") != null ? sort : sort.and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(pagina, tamanio, orden);
    }
}
