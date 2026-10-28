package com.utn.tp_spring_grupal.articulo;

import com.utn.tp_spring_grupal.common.Busqueda;
import com.utn.tp_spring_grupal.common.PageResponse;
import org.springframework.data.domain.Page;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArticuloService {

    private final ArticuloRepository articuloRepository;

    public ArticuloService(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    /** Catálogo de artículos (sin precio). orden: codigo (default) | denominacion; dir: asc (default) | desc. */
    public PageResponse<ArticuloDTO> buscar(String q, Long rubroId, Long marcaId, String orden, String dir,
                                            int page, int size) {
        String propiedad = "denominacion".equalsIgnoreCase(orden) ? "denominacion" : "codigo";
        Sort sort = Sort.by(Busqueda.direccion(dir), propiedad);

        Specification<Articulo> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (Busqueda.hayTexto(q)) {
                String patron = Busqueda.patron(q);
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("codigo")), patron),
                        cb.like(cb.lower(root.get("denominacion")), patron)));
            }
            if (rubroId != null) {
                ps.add(cb.equal(root.get("rubro").get("id"), rubroId));
            }
            if (marcaId != null) {
                ps.add(cb.equal(root.get("marca").get("id"), marcaId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<Articulo> pagina = articuloRepository.findAll(spec, Busqueda.pageable(page, size, sort));
        return PageResponse.de(pagina, a -> new ArticuloDTO(
                        a.getId(),
                        a.getCodigo(),
                        a.getDenominacion(),
                        a.getRubro() != null ? a.getRubro().getDenominacion() : null,
                        a.getMarca() != null ? a.getMarca().getDenominacion() : null,
                        a.getAlicuotaIva().getPorcentaje()));
    }
}
