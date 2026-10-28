package com.utn.tp_spring_grupal.puntoventa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PuntoVentaService {

    private final PuntoVentaRepository repository;

    public PuntoVentaService(PuntoVentaRepository repository) {
        this.repository = repository;
    }

    public List<PuntoVentaDTO> listar() {
        return repository.findAll().stream()
                .map(c -> new PuntoVentaDTO(c.getId(), c.getDescripcion()))
                .toList();
    }
}
