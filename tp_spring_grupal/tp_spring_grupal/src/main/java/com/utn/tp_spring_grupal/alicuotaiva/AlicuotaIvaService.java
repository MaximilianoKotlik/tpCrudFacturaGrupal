package com.utn.tp_spring_grupal.alicuotaiva;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AlicuotaIvaService {

    private final AlicuotaIvaRepository repository;

    public AlicuotaIvaService(AlicuotaIvaRepository repository) {
        this.repository = repository;
    }

    public List<AlicuotaIvaDTO> listar() {
        return repository.findAll(Sort.by("porcentaje")).stream()
                .map(a -> new AlicuotaIvaDTO(a.getId(), a.getCodigoAfip(), a.getPorcentaje(), a.getDenominacion()))
                .toList();
    }
}
