package com.utn.tp_spring_grupal.cliente;

import com.utn.tp_spring_grupal.common.PageResponse;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;
import com.utn.tp_spring_grupal.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(ClienteService.class)
class ClienteServiceTest {

    @Autowired TestEntityManager em;
    @Autowired ClienteService service;
    CondicionIva condicionRi;

    @BeforeEach
    void datos() {
        TestData d = new TestData(em);
        condicionRi = d.condicionIva();
        d.cliente("Juan PEREZ", "20-11111111-1");
        d.cliente("Ana Gomez", "27-22222222-2");
        d.cliente("Zulema Perez Hnos", "30-33333333-3");
        em.flush();
    }

    private List<String> nombres(PageResponse<ClienteDTO> l) {
        return l.contenido().stream().map(ClienteDTO::denominacion).toList();
    }

    @Test
    void sinFiltrosDevuelveTodosOrdenadosPorDenominacionAscendente() {
        assertEquals(List.of("Ana Gomez", "Juan PEREZ", "Zulema Perez Hnos"), nombres(service.buscar(null, null, null, 0, 10)));
    }

    @Test
    void busquedaPorDenominacionEsContainsYNoDistingueMayusculas() {
        assertEquals(List.of("Juan PEREZ", "Zulema Perez Hnos"), nombres(service.buscar("perez", null, null, 0, 10)));
    }

    @Test
    void busquedaPorCuitCuil() {
        assertEquals(List.of("Ana Gomez"), nombres(service.buscar("27-2222", null, null, 0, 10)));
    }

    @Test
    void ordenDescendentePorDenominacion() {
        assertEquals(List.of("Zulema Perez Hnos", "Juan PEREZ", "Ana Gomez"), nombres(service.buscar("", "denominacion", "desc", 0, 10)));
    }

    @Test
    void ordenPorCuit() {
        assertEquals(List.of("Zulema Perez Hnos", "Ana Gomez", "Juan PEREZ"), nombres(service.buscar(null, "cuit", "desc", 0, 10)));
        assertEquals(List.of("Juan PEREZ", "Ana Gomez", "Zulema Perez Hnos"), nombres(service.buscar(null, "cuit", "asc", 0, 10)));
    }

    @Test
    void devuelveElContratoDelDto() {
        ClienteDTO dto = service.buscar("ana", null, null, 0, 10).contenido().get(0);
        assertEquals("27-22222222-2", dto.cuitCuil());
        assertEquals(condicionRi.getId(), dto.condicionIvaId());
        assertEquals("Responsable Inscripto", dto.condicionIvaDenominacion());
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        assertEquals(0, service.buscar("inexistente", null, null, 0, 10).contenido().size());
        assertEquals(0, service.buscar("inexistente", null, null, 0, 10).totalElementos());
    }

    @Test
    void paginaConTotalesYUltimaPaginaParcial() {
        PageResponse<ClienteDTO> p0 = service.buscar(null, "denominacion", "asc", 0, 2);
        assertEquals(List.of("Ana Gomez", "Juan PEREZ"), nombres(p0));
        assertEquals(0, p0.pagina());
        assertEquals(2, p0.tamanio());
        assertEquals(3, p0.totalElementos());
        assertEquals(2, p0.totalPaginas());

        PageResponse<ClienteDTO> p1 = service.buscar(null, "denominacion", "asc", 1, 2);
        assertEquals(List.of("Zulema Perez Hnos"), nombres(p1));
        assertEquals(1, p1.pagina());
    }

    @Test
    void totalesRespetanElFiltroDeBusqueda() {
        PageResponse<ClienteDTO> p = service.buscar("perez", null, null, 0, 1);
        assertEquals(1, p.contenido().size());
        assertEquals(2, p.totalElementos());
        assertEquals(2, p.totalPaginas());
    }

    @Test
    void tamanioSeLimitaA100YLosValoresInvalidosCaenEnElDefault() {
        assertEquals(100, service.buscar(null, null, null, 0, 1000).tamanio());
        assertEquals(10, service.buscar(null, null, null, 0, 0).tamanio());
        assertEquals(0, service.buscar(null, null, null, -5, 10).pagina());
    }

    @Test
    void paginaFueraDeRangoDevuelveContenidoVacioConElTotalReal() {
        PageResponse<ClienteDTO> p = service.buscar(null, null, null, 9, 10);
        assertEquals(0, p.contenido().size());
        assertEquals(3, p.totalElementos());
    }
}
