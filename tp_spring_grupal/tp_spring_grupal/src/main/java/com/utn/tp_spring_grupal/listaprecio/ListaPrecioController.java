package com.utn.tp_spring_grupal.listaprecio;

import com.utn.tp_spring_grupal.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/listas-precio")
public class ListaPrecioController {

    private final ListaPrecioService service;

    public ListaPrecioController(ListaPrecioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ListaPrecioDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}/articulos")
    public ResponseEntity<PageResponse<ListaPrecioArticuloDTO>> articulos(
            @PathVariable Long id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long rubroId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.buscarArticulos(id, q, rubroId, marcaId, orden, dir, page, size));
    }
}
