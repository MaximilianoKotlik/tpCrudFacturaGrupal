package com.utn.tp_spring_grupal.emisor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmisorControllerTest {

    @WebMvcTest(EmisorController.class)
    @Import(EmisorProperties.class)
    static class ResponsableInscripto {
        @Autowired MockMvc mvc;

        @Test
        void porDefectoElEmisorEsResponsableInscriptoYDiscriminaIva() throws Exception {
            mvc.perform(get("/api/emisor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.condicionIva").value("RESPONSABLE_INSCRIPTO"))
                    .andExpect(jsonPath("$.discriminaIva").value(true));
        }
    }

    @WebMvcTest(EmisorController.class)
    @Import(EmisorProperties.class)
    @TestPropertySource(properties = "afip.condicion-iva-emisor=MONOTRIBUTO")
    static class Monotributo {
        @Autowired MockMvc mvc;

        @Test
        void monotributoNoDiscriminaIva() throws Exception {
            mvc.perform(get("/api/emisor"))
                    .andExpect(jsonPath("$.condicionIva").value("MONOTRIBUTO"))
                    .andExpect(jsonPath("$.discriminaIva").value(false));
        }
    }
}
