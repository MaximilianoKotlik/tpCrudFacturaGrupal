package com.utn.tp_spring_grupal.alicuotaiva;

import com.utn.tp_spring_grupal.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(AlicuotaIvaService.class)
class AlicuotaIvaServiceTest {

    @Autowired TestEntityManager em;
    @Autowired AlicuotaIvaService service;

    @Test
    void listaLasAlicuotasOrdenadasPorPorcentajeConElContratoDelDto() {
        TestData d = new TestData(em);
        d.alicuota(5, "21");
        d.alicuota(3, "0");
        d.alicuota(4, "10.5");
        em.flush();

        List<AlicuotaIvaDTO> r = service.listar();

        assertEquals(List.of(3, 4, 5), r.stream().map(AlicuotaIvaDTO::codigoAfip).toList());
        assertEquals(0, new BigDecimal("10.5").compareTo(r.get(1).porcentaje()));
        assertEquals("IVA 21 %", r.get(2).denominacion());
    }
}
