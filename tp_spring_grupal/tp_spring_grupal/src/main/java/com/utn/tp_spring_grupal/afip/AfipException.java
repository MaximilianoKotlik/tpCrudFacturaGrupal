package com.utn.tp_spring_grupal.afip;

/** ARCA respondió con un error o un rechazo. El mensaje está pensado para mostrarse tal cual al usuario. */
public class AfipException extends RuntimeException {

    public AfipException(String mensaje) {
        super(mensaje);
    }
}
