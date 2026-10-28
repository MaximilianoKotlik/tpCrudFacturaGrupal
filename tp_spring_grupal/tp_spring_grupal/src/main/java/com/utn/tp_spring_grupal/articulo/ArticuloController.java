package com.utn.tp_spring_grupal.articulo;

import com.utn.tp_spring_grupal.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articulos")
public class ArticuloController {

    private final ArticuloService service;

    public ArticuloController(ArticuloService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ArticuloDTO>> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long rubroId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.buscar(q, rubroId, marcaId, orden, dir, page, size));
    }
}
