package com.utn.tp_spring_grupal.afip;

/** No se pudo obtener el ticket de acceso (WSAA). Falla antes de enviar nada al WSFE: no hay resultado indeterminado. */
public class AfipAutenticacionException extends RuntimeException {

    public AfipAutenticacionException(String mensaje) {
        super(mensaje);
    }

    public AfipAutenticacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
