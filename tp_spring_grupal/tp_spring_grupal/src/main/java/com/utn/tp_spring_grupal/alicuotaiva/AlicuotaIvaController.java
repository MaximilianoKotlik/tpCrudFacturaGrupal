package com.utn.tp_spring_grupal.alicuotaiva;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alicuotas-iva")
public class AlicuotaIvaController {

    private final AlicuotaIvaService service;

    public AlicuotaIvaController(AlicuotaIvaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AlicuotaIvaDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
