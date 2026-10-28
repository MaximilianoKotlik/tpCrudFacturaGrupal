package com.utn.tp_spring_grupal.marca;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarcaService {

    private final MarcaRepository repository;

    public MarcaService(MarcaRepository repository) {
        this.repository = repository;
    }

    public List<MarcaDTO> listar() {
        return repository.findAll().stream()
                .map(c -> new MarcaDTO(c.getId(), c.getDenominacion()))
                .toList();
    }
}
