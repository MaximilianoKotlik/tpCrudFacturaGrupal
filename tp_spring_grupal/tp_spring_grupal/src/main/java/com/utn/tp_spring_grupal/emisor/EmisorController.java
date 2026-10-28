package com.utn.tp_spring_grupal.emisor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emisor")
public class EmisorController {

    /** Lo que el frontend necesita saber del emisor (p. ej. para mostrar u ocultar el IVA). */
    public record EmisorDTO(String condicionIva, boolean discriminaIva) {
    }

    private final EmisorProperties emisor;

    public EmisorController(EmisorProperties emisor) {
        this.emisor = emisor;
    }

    @GetMapping
    public ResponseEntity<EmisorDTO> obtener() {
        return ResponseEntity.ok(new EmisorDTO(emisor.getCondicionIvaEmisor().name(), emisor.discriminaIva()));
    }
}
