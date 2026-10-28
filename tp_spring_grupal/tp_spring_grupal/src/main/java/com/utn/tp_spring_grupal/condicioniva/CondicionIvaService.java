package com.utn.tp_spring_grupal.condicioniva;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CondicionIvaService {

    private final CondicionIvaRepository repository;

    public CondicionIvaService(CondicionIvaRepository repository) {
        this.repository = repository;
    }

    public List<CondicionIvaDTO> listar() {
        return repository.findAll().stream()
                .map(c -> new CondicionIvaDTO(c.getId(), c.getDenominacion(), c.getCodigoAfip()))
                .toList();
    }
}
