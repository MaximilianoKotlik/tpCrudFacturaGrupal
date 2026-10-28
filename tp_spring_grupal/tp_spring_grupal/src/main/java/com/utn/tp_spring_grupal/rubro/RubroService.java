package com.utn.tp_spring_grupal.rubro;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RubroService {

    private final RubroRepository repository;

    public RubroService(RubroRepository repository) {
        this.repository = repository;
    }

    public List<RubroDTO> listar() {
        return repository.findAll().stream()
                .map(c -> new RubroDTO(c.getId(), c.getDenominacion()))
                .toList();
    }
}
