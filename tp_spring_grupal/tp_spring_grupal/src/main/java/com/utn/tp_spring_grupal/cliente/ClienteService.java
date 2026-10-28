package com.utn.tp_spring_grupal.cliente;

import com.utn.tp_spring_grupal.common.Busqueda;
import com.utn.tp_spring_grupal.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    /**
     * Búsqueda para el explorer de clientes. q: contains (sin distinguir mayúsculas) en denominación o CUIT/CUIL.
     * orden: denominacion (default) | cuit; dir: asc (default) | desc.
     */
    public PageResponse<ClienteDTO> buscar(String q, String orden, String dir, int page, int size) {
        String propiedad = "cuit".equalsIgnoreCase(orden) ? "cuitCuil" : "denominacion";
        Sort sort = Sort.by(Busqueda.direccion(dir), propiedad);

        Specification<Cliente> spec = (root, query, cb) -> {
            if (!Busqueda.hayTexto(q)) {
                return cb.conjunction();
            }
            String patron = Busqueda.patron(q);
            return cb.or(
                    cb.like(cb.lower(root.get("denominacion")), patron),
                    cb.like(cb.lower(root.get("cuitCuil")), patron));
        };

        Page<Cliente> pagina = clienteRepository.findAll(spec, Busqueda.pageable(page, size, sort));
        return PageResponse.de(pagina, c -> new ClienteDTO(c.getId(), c.getDenominacion(), c.getCuitCuil(),
                c.getCondicionIva().getId(), c.getCondicionIva().getDenominacion()));
    }
}
