package com.utn.tp_spring_grupal.puntoventa;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/puntos-venta")
public class PuntoVentaController {

    private final PuntoVentaService service;

    public PuntoVentaController(PuntoVentaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PuntoVentaDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
