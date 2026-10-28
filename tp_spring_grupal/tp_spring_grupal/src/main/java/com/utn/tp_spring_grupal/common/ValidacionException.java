package com.utn.tp_spring_grupal.common;

/** Request con datos inválidos (cantidad <= 0, bonificación fuera de rango, sin renglones). Se traduce a HTTP 400. */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
