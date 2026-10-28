package com.utn.tp_spring_grupal.tipomoneda;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TipoMonedaService {

    private final TipoMonedaRepository repository;

    public TipoMonedaService(TipoMonedaRepository repository) {
        this.repository = repository;
    }

    public List<TipoMonedaDTO> listar() {
        return repository.findAll().stream()
                .map(c -> new TipoMonedaDTO(c.getId(), c.getDenominacion(), c.getSimbolo()))
                .toList();
    }
}
