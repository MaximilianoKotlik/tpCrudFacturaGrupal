package com.utn.tp_spring_grupal.afip;

import java.math.BigDecimal;

/**
 * Un Consumidor Final sin identificar (DocTipo 99) no puede superar un monto por comprobante. El tope vigente
 * (RG 5700/2025: $10.000.000, según fuentes periodísticas; confirmar en ARCA) es configurable en
 * {@code afip.consumidor-final-monto-maximo}.
 */
public final class ReglaConsumidorFinal {

    private ReglaConsumidorFinal() {
    }

    public static void validar(DocumentoReceptor documento, BigDecimal totalEnPesos, BigDecimal limite) {
        if (documento.tipo() == DocumentoReceptor.TIPO_CONSUMIDOR_FINAL && totalEnPesos.compareTo(limite) >= 0) {
            throw new IllegalArgumentException("Un Consumidor Final sin identificar no puede superar $"
                    + limite.toPlainString() + " por comprobante; cargá el DNI o CUIT del cliente.");
        }
    }
}
