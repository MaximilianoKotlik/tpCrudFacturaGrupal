package com.utn.tp_spring_grupal.rubro;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rubros")
public class RubroController {

    private final RubroService service;

    public RubroController(RubroService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<RubroDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
