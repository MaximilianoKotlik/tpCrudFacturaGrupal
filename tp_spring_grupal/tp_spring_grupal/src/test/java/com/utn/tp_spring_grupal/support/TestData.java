package com.utn.tp_spring_grupal.support;

import com.utn.tp_spring_grupal.alicuotaiva.AlicuotaIva;
import com.utn.tp_spring_grupal.articulo.Articulo;
import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.cliente.Contacto;
import com.utn.tp_spring_grupal.cliente.Domicilio;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;
import com.utn.tp_spring_grupal.factura.EstadoFactura;
import com.utn.tp_spring_grupal.factura.FacturaVenta;
import com.utn.tp_spring_grupal.factura.FacturaVentaDetalle;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecio;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticulo;
import com.utn.tp_spring_grupal.marca.Marca;
import com.utn.tp_spring_grupal.puntoventa.PuntoVenta;
import com.utn.tp_spring_grupal.rubro.Rubro;
import com.utn.tp_spring_grupal.tipomoneda.TipoMoneda;
import com.utn.tp_spring_grupal.usuario.Usuario;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

/** Helper para persistir entidades de prueba con la auditoría obligatoria ya resuelta. */
public class TestData {

    private final TestEntityManager em;
    private final Date ahora = new Date();
    public final Usuario usuario;
    private AlicuotaIva alicuota21;
    private CondicionIva condicionRi;

    public TestData(TestEntityManager em) {
        this.em = em;
        this.usuario = em.persist(Usuario.builder().usuario("u").clave("c").nombre("n").apellido("a").build());
    }

    /** Alícuota 21 % (código AFIP 5), creada una sola vez. */
    public AlicuotaIva alicuota21() {
        if (alicuota21 == null) {
            alicuota21 = alicuota(5, "21");
        }
        return alicuota21;
    }

    public AlicuotaIva alicuota(int codigoAfip, String porcentaje) {
        return em.persist(AlicuotaIva.builder().codigoAfip(codigoAfip).porcentaje(new BigDecimal(porcentaje))
                .denominacion("IVA " + porcentaje + " %")
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    /** Cliente Responsable Inscripto. */
    public Cliente cliente(String denominacion, String cuit) {
        return cliente(denominacion, cuit, condicionIva());
    }

    public Cliente cliente(String denominacion, String cuit, CondicionIva condicion) {
        Contacto contacto = em.persist(Contacto.builder().email("x@x.com").build());
        Domicilio domicilio = em.persist(Domicilio.builder().nombreCalle("Calle").numeroCalle("1").build());
        return em.persist(Cliente.builder().denominacion(denominacion).cuitCuil(cuit)
                .contacto(contacto).domicilio(domicilio).condicionIva(condicion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    public Rubro rubro(String denominacion) {
        return em.persist(Rubro.builder().codigo(1).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    public Marca marca(String denominacion) {
        return em.persist(Marca.builder().codigo(1).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    /** Artículo con alícuota 21 %. */
    public Articulo articulo(String codigo, String denominacion, Rubro rubro, Marca marca) {
        return articulo(codigo, denominacion, rubro, marca, alicuota21());
    }

    public Articulo articulo(String codigo, String denominacion, Rubro rubro, Marca marca, AlicuotaIva alicuota) {
        return em.persist(Articulo.builder().codigo(codigo).denominacion(denominacion).rubro(rubro).marca(marca)
                .alicuotaIva(alicuota)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    public ListaPrecio lista(String codigo, String denominacion) {
        return em.persist(ListaPrecio.builder().codigo(codigo).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    public ListaPrecioArticulo precio(ListaPrecio lista, Articulo articulo, double precio) {
        return em.persist(ListaPrecioArticulo.builder().listaPrecio(lista).articulo(articulo)
                .precioVenta(BigDecimal.valueOf(precio))
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    /** Condición IVA Responsable Inscripto (código AFIP 1), creada una sola vez. */
    public CondicionIva condicionIva() {
        if (condicionRi == null) {
            condicionRi = condicionIva(1, "Responsable Inscripto");
        }
        return condicionRi;
    }

    public CondicionIva condicionIva(int codigoAfip, String denominacion) {
        return em.persist(CondicionIva.builder().codigoAfip(codigoAfip).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    public PuntoVenta puntoVenta() {
        return em.persist(PuntoVenta.builder().numero(1).descripcion("Centro")
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    public TipoMoneda moneda() {
        return em.persist(TipoMoneda.builder().codigoAfip("PES").denominacion("Pesos").simbolo("$")
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario).build());
    }

    /**
     * Factura con un único renglón por el total indicado (neto = total, sin IVA). La condición IVA es la del
     * cliente. fechaCreacion se guarda como fechaAlta (mediodía, zona del sistema).
     */
    public FacturaVenta factura(EstadoFactura estado, LocalDate fechaCreacion, double total, String motivoRechazo,
                                Cliente cliente, ListaPrecio lista, PuntoVenta pv, TipoMoneda moneda,
                                ListaPrecioArticulo lpa) {
        BigDecimal importe = BigDecimal.valueOf(total).setScale(2);
        Date alta = Date.from(fechaCreacion.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant());
        FacturaVenta f = FacturaVenta.builder()
                .estado(estado).importeNeto(importe).importeIva(BigDecimal.ZERO.setScale(2))
                .importeTotal(importe).importeSaldo(importe).motivoRechazo(motivoRechazo)
                .cliente(cliente).listaPrecio(lista).condicionIva(cliente.getCondicionIva()).puntoVenta(pv).tipoMoneda(moneda)
                .fechaAlta(alta).fechaModificacion(ahora).usuarioCarga(usuario).usuarioModificacion(usuario)
                .detalles(new ArrayList<>()).build();
        f.getDetalles().add(FacturaVentaDetalle.builder().factura(f).listaPrecioArticulo(lpa)
                .cantidad(1).precioUnitario(importe).importeNeto(importe).importeIva(BigDecimal.ZERO.setScale(2))
                .importeSubtotal(importe).build());
        return em.persist(f);
    }
}
