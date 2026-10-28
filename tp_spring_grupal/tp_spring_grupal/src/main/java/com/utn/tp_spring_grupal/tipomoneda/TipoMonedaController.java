package com.utn.tp_spring_grupal.tipomoneda;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-moneda")
public class TipoMonedaController {

    private final TipoMonedaService service;

    public TipoMonedaController(TipoMonedaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TipoMonedaDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
