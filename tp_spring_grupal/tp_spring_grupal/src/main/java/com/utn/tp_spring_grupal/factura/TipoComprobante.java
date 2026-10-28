package com.utn.tp_spring_grupal.factura;

/** Tipos de comprobante soportados (CbteTipo de AFIP). */
public enum TipoComprobante {
    A(1, "A"),
    B(6, "B"),
    C(11, "C");

    private final int codigoAfip;
    private final String letra;

    TipoComprobante(int codigoAfip, String letra) {
        this.codigoAfip = codigoAfip;
        this.letra = letra;
    }

    public int getCodigoAfip() {
        return codigoAfip;
    }

    public String getLetra() {
        return letra;
    }

    public static TipoComprobante deCodigoAfip(int codigo) {
        for (TipoComprobante t : values()) {
            if (t.codigoAfip == codigo) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de comprobante no soportado: " + codigo);
    }
}
