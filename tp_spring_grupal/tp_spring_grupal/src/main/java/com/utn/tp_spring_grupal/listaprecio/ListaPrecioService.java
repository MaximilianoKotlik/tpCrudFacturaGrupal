package com.utn.tp_spring_grupal.listaprecio;

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
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ListaPrecioService {

    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;

    public ListaPrecioService(ListaPrecioRepository listaPrecioRepository,
                              ListaPrecioArticuloRepository listaPrecioArticuloRepository) {
        this.listaPrecioRepository = listaPrecioRepository;
        this.listaPrecioArticuloRepository = listaPrecioArticuloRepository;
    }

    public List<ListaPrecioDTO> listar() {
        return listaPrecioRepository.findAll().stream()
                .map(l -> new ListaPrecioDTO(l.getId(), l.getCodigo(), l.getDenominacion()))
                .toList();
    }

    /**
     * Artículos de una lista con su precio. orden: codigo (default) | denominacion | precio; dir: asc (default) | desc.
     */
    public PageResponse<ListaPrecioArticuloDTO> buscarArticulos(Long listaPrecioId, String q, Long rubroId,
                                                                Long marcaId, String orden, String dir,
                                                                int page, int size) {
        String propiedad = switch (orden == null ? "" : orden.toLowerCase()) {
            case "denominacion" -> "articulo.denominacion";
            case "precio" -> "precioVenta";
            default -> "articulo.codigo";
        };
        Sort sort = Sort.by(Busqueda.direccion(dir), propiedad);

        Specification<ListaPrecioArticulo> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("listaPrecio").get("id"), listaPrecioId));
            if (Busqueda.hayTexto(q)) {
                String patron = Busqueda.patron(q);
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("articulo").get("codigo")), patron),
                        cb.like(cb.lower(root.get("articulo").get("denominacion")), patron)));
            }
            if (rubroId != null) {
                ps.add(cb.equal(root.get("articulo").get("rubro").get("id"), rubroId));
            }
            if (marcaId != null) {
                ps.add(cb.equal(root.get("articulo").get("marca").get("id"), marcaId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<ListaPrecioArticulo> pagina = listaPrecioArticuloRepository.findAll(spec,
                Busqueda.pageable(page, size, sort));
        List<Long> ids = pagina.getContent().stream().map(lpa -> lpa.getArticulo().getId()).toList();
        Map<Long, List<PrecioListaDTO>> precios = ids.isEmpty()
                ? Map.of()
                : PrecioListaDTO.porArticulo(listaPrecioArticuloRepository.findByArticuloIdIn(ids));
        return PageResponse.de(pagina, lpa -> new ListaPrecioArticuloDTO(
                        lpa.getArticulo().getId(),
                        lpa.getArticulo().getCodigo(),
                        lpa.getArticulo().getDenominacion(),
                        lpa.getArticulo().getRubro() != null ? lpa.getArticulo().getRubro().getDenominacion() : null,
                        lpa.getArticulo().getMarca() != null ? lpa.getArticulo().getMarca().getDenominacion() : null,
                        lpa.getPrecioVenta(),
                        lpa.getArticulo().getAlicuotaIva().getPorcentaje(),
                        precios.getOrDefault(lpa.getArticulo().getId(), List.of())));
    }
}
