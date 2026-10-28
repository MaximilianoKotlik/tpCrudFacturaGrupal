package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.emisor.CondicionIvaEmisor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz emisor x condición IVA del receptor (códigos AFIP: 1 RI, 4 Exento, 5 CF, 6 Monotributo, 15 No alcanzado). */
class TipoComprobanteResolverTest {

    @ParameterizedTest(name = "emisor RI y receptor {0} -> {1}")
    @CsvSource({
            "1,A",
            "6,A",
            "4,B",
            "5,B",
            "15,B"
    })
    void emisorResponsableInscripto(int receptor, String letra) {
        TipoComprobante t = TipoComprobanteResolver.resolver(CondicionIvaEmisor.RESPONSABLE_INSCRIPTO, receptor);
        assertEquals(letra, t.getLetra());
    }

    @ParameterizedTest(name = "emisor {0} y receptor {1} -> C")
    @CsvSource({
            "MONOTRIBUTO,1", "MONOTRIBUTO,4", "MONOTRIBUTO,5", "MONOTRIBUTO,6", "MONOTRIBUTO,15",
            "EXENTO,1", "EXENTO,4", "EXENTO,5", "EXENTO,6", "EXENTO,15"
    })
    void emisorQueNoDiscriminaSiempreEmiteC(CondicionIvaEmisor emisor, int receptor) {
        assertEquals("C", TipoComprobanteResolver.resolver(emisor, receptor).getLetra());
    }

    @ParameterizedTest(name = "emisor RI y receptor {0} -> A (FEParamGetCondicionIvaReceptor ClaseCmp=A)")
    @CsvSource({"13", "16"})
    void riAMonotributistaSocialYPromovidoEmiteA(int receptor) {
        assertEquals("A", TipoComprobanteResolver.resolver(CondicionIvaEmisor.RESPONSABLE_INSCRIPTO, receptor).getLetra());
    }

    @ParameterizedTest(name = "emisor RI y receptor {0} -> B (FEParamGetCondicionIvaReceptor ClaseCmp=B)")
    @CsvSource({"4", "5", "7", "8", "9", "10", "15"})
    void riAReceptoresDeClaseBEmiteB(int receptor) {
        assertEquals("B", TipoComprobanteResolver.resolver(CondicionIvaEmisor.RESPONSABLE_INSCRIPTO, receptor).getLetra());
    }

    @Test
    void codigosAfipDeLosComprobantes() {
        assertEquals(1, TipoComprobante.A.getCodigoAfip());
        assertEquals(6, TipoComprobante.B.getCodigoAfip());
        assertEquals(11, TipoComprobante.C.getCodigoAfip());
        assertEquals(TipoComprobante.B, TipoComprobante.deCodigoAfip(6));
    }

    @Test
    void soloElResponsableInscriptoDiscriminaIva() {
        assertTrue(CondicionIvaEmisor.RESPONSABLE_INSCRIPTO.discriminaIva());
        assertFalse(CondicionIvaEmisor.MONOTRIBUTO.discriminaIva());
        assertFalse(CondicionIvaEmisor.EXENTO.discriminaIva());
    }
}
