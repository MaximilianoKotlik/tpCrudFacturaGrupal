package com.utn.tp_spring_grupal.common;

/** Violación de una regla de negocio (p. ej. editar una factura emitida). Se traduce a HTTP 409. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
