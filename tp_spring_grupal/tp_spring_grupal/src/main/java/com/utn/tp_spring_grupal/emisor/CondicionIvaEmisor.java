package com.utn.tp_spring_grupal.emisor;

/** Condición frente al IVA de quien emite las facturas (configurable con afip.condicion-iva-emisor). */
public enum CondicionIvaEmisor {
    RESPONSABLE_INSCRIPTO,
    MONOTRIBUTO,
    EXENTO;

    /** Solo el Responsable Inscripto discrimina IVA (comprobantes A / B). */
    public boolean discriminaIva() {
        return this == RESPONSABLE_INSCRIPTO;
    }
}
