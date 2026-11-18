package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.emisor.CondicionIvaEmisor;

/**
 * Tipo de comprobante = f(condición del emisor, condición IVA del receptor). Clase pura.
 * Emisor RI: A si el receptor es RI (1) o Monotributo (6); B para el resto (CF, Exento, No alcanzado...).
 * Emisor Monotributo o Exento: siempre C.
 */
public final class TipoComprobanteResolver {

    /** Receptores de clase A según FEParamGetCondicionIvaReceptor: RI (1), Monotributo (6), Mono. Social (13), Mono. Promovido (16). */
    static final java.util.Set<Integer> RECEPTORES_CLASE_A = java.util.Set.of(1, 6, 13, 16);

    private TipoComprobanteResolver() {
    }

    public static TipoComprobante resolver(CondicionIvaEmisor emisor, int codigoAfipCondicionIvaReceptor) {
        if (!emisor.discriminaIva()) {
            return TipoComprobante.C;
        }
        boolean receptorDiscriminado = RECEPTORES_CLASE_A.contains(codigoAfipCondicionIvaReceptor);
        return receptorDiscriminado ? TipoComprobante.A : TipoComprobante.B;
    }
}
