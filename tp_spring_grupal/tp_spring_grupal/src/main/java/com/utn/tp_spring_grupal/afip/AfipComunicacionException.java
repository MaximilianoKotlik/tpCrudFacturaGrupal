package com.utn.tp_spring_grupal.afip;

/**
 * Falla de red o de protocolo con ARCA (timeout, conexión rechazada, HTTP de error). Al pedir un CAE el
 * resultado queda INDETERMINADO: ARCA pudo haber autorizado el comprobante, hay que consultarlo.
 */
public class AfipComunicacionException extends RuntimeException {

    private final int estadoHttp;
    private final String cuerpo;

    public AfipComunicacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.estadoHttp = 0;
        this.cuerpo = null;
    }

    /** Respuesta HTTP de error; el cuerpo completo queda aparte para poder leer un SOAP Fault, fuera del mensaje. */
    public AfipComunicacionException(String mensaje, int estadoHttp, String cuerpo) {
        super(mensaje);
        this.estadoHttp = estadoHttp;
        this.cuerpo = cuerpo;
    }

    public int getEstadoHttp() {
        return estadoHttp;
    }

    public String getCuerpo() {
        return cuerpo;
    }
}
