package com.utn.tp_spring_grupal.factura;

/** Ciclo de vida de una factura. PENDIENTE y ERROR_AFIP son los únicos estados editables/emitibles. */
public enum EstadoFactura {
    PENDIENTE,
    EMITIDA,
    ERROR_AFIP,
    ANULADA;

    /** Texto para mostrar al usuario dentro de una frase ("la factura está ..."). */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE -> "pendiente";
            case EMITIDA -> "emitida";
            case ERROR_AFIP -> "con error de ARCA";
            case ANULADA -> "anulada";
        };
    }

    public boolean esEditable() {
        return this == PENDIENTE || this == ERROR_AFIP;
    }
}
