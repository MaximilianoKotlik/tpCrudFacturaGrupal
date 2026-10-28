package com.utn.tp_spring_grupal.condicioniva;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/condiciones-iva")
public class CondicionIvaController {

    private final CondicionIvaService service;

    public CondicionIvaController(CondicionIvaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CondicionIvaDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
