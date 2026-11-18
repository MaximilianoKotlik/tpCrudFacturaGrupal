package com.utn.tp_spring_grupal.config;

import com.utn.tp_spring_grupal.afip.Cuit;
import com.utn.tp_spring_grupal.alicuotaiva.AlicuotaIva;
import com.utn.tp_spring_grupal.articulo.Articulo;
import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.cliente.Contacto;
import com.utn.tp_spring_grupal.cliente.Domicilio;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;
import com.utn.tp_spring_grupal.emisor.EmisorProperties;
import com.utn.tp_spring_grupal.factura.CalculadoraIva;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.Alicuota;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.ImportesRenglon;
import com.utn.tp_spring_grupal.factura.CalculadoraIva.Totales;
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
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Carga datos de prueba abundantes y coherentes la primera vez que arranca la app.
 * No consulta a AFIP: no hay facturas EMITIDA (se emiten desde la aplicación), así que ningún dato
 * lleva número ni CAE inventados.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final long DIA_MS = 24L * 60 * 60 * 1000;

    /** Alícuota por posición del artículo (i % 10): mayoría 21 %, con 10,5 / 27 / 0 / 5 intercaladas. */
    private static final String[] ALICUOTA_POR_POSICION = { "21", "21", "21", "10.5", "21", "21", "27", "21", "0", "5" };

    /** Condición IVA (índice en la lista creada) por posición del cliente (i % 7). */
    private static final int[] CONDICION_POR_POSICION = { 0, 0, 1, 2, 0, 3, 4 };

    private static final BigDecimal[] BONIFICACIONES = {
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("7.5") };

    private final EntityManager em;
    private final EmisorProperties emisor;

    private final Date ahora = new Date();
    private Usuario admin;

    public DataLoader(EntityManager em, EmisorProperties emisor) {
        this.em = em;
        this.emisor = emisor;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long count = em.createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class).getSingleResult();
        if (count > 0) {
            System.out.println("La base de datos ya contiene información. Omitiendo carga inicial.");
            return;
        }
        System.out.println("Inyectando datos de prueba...");

        admin = Usuario.builder().usuario("admin").clave("123456").nombre("Juan").apellido("Perez").build();
        em.persist(admin);

        // ---- puntos de venta, condiciones de IVA, alícuotas, monedas
        // Un único punto de venta: el de Web Services dado de alta en ARCA (afip.punto-venta).
        PuntoVenta puntoVentaWs = puntoVenta(emisor.getPuntoVenta(), "Punto de Venta Web Services",
                "Factura Electrónica", "Domicilio comercial");

        // El orden importa: CONDICION_POR_POSICION indexa esta lista.
        List<CondicionIva> condiciones = List.of(
                condicionIva(1, "IVA Responsable Inscripto"),
                condicionIva(6, "Responsable Monotributo"),
                condicionIva(5, "Consumidor Final"),
                condicionIva(4, "IVA Sujeto Exento"),
                condicionIva(15, "IVA No Alcanzado"));

        Map<String, AlicuotaIva> alicuotas = new HashMap<>();
        alicuotas.put("0", alicuota(3, "0", "IVA 0 %"));
        alicuotas.put("2.5", alicuota(9, "2.5", "IVA 2,5 %"));
        alicuotas.put("5", alicuota(8, "5", "IVA 5 %"));
        alicuotas.put("10.5", alicuota(4, "10.5", "IVA 10,5 %"));
        alicuotas.put("21", alicuota(5, "21", "IVA 21 %"));
        alicuotas.put("27", alicuota(6, "27", "IVA 27 %"));

        TipoMoneda pesos = moneda("PES", "Pesos Argentinos", "$");
        TipoMoneda dolar = moneda("DOL", "Dólar Estadounidense", "US$");
        moneda("060", "Euro", "€");

        // ---- clientes: { denominación, prefijo de CUIT }. La condición IVA sale de CONDICION_POR_POSICION.
        String[][] datosClientes = {
                { "Juan Pérez", "20" }, { "María González", "27" }, { "Distribuidora Norte S.A.", "30" },
                { "Ferretería El Tornillo", "30" }, { "Carlos Rodríguez", "20" }, { "Ana Martínez", "27" },
                { "Supermercado La Esquina", "30" }, { "Lucía Fernández", "27" }, { "Tecno Soluciones SRL", "30" },
                { "Pedro Sánchez", "20" }, { "Librería Don Quijote", "33" }, { "Estudio Contable Ramírez", "30" },
                { "Panadería La Espiga", "30" }, { "Roberto Díaz", "20" }, { "Constructora Del Sur S.A.", "30" },
                { "Laura Benítez", "27" }, { "Farmacia Central", "30" }, { "Martín Acosta", "20" },
                { "Transportes Rápidos SRL", "30" }, { "Silvia Romero", "27" }, { "Kiosco El Faro", "20" },
                { "Gustavo Herrera", "20" }, { "Colegio San Martín", "30" }, { "Verónica Molina", "27" },
                { "Agropecuaria Los Álamos", "30" }, { "Diego Castro", "20" }, { "Hotel Plaza Mayor", "30" },
                { "Florencia Ortiz", "27" },
        };
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < datosClientes.length; i++) {
            String cuit = Cuit.completar(datosClientes[i][1], 20_000_000 + i * 1_357_911);
            CondicionIva condicion = condiciones.get(CONDICION_POR_POSICION[i % CONDICION_POR_POSICION.length]);
            clientes.add(cliente(datosClientes[i][0], cuit, condicion, i));
        }

        // ---- rubros y marcas
        String[] nombresRubro = { "Electrónica", "Computación", "Oficina", "Hogar", "Herramientas" };
        List<Rubro> rubros = new ArrayList<>();
        for (int i = 0; i < nombresRubro.length; i++) {
            rubros.add(rubro((i + 1) * 10, nombresRubro[i]));
        }
        String[] nombresMarca = { "Logitech", "Samsung", "HP", "Epson", "Philips", "Bosch" };
        List<Marca> marcas = new ArrayList<>();
        for (int i = 0; i < nombresMarca.length; i++) {
            marcas.add(marca(101 + i, nombresMarca[i]));
        }

        // ---- artículos: { código, denominación, rubro, marca (-1 = sin marca), precio base neto }
        Object[][] datosArticulos = {
                { "ART-001", "Mouse Inalámbrico", 1, 0, 15000.0 },
                { "ART-002", "Monitor 24 Pulgadas", 1, 1, 150000.0 },
                { "ART-003", "Teclado Mecánico", 1, 0, 45000.0 },
                { "ART-004", "Auriculares Bluetooth", 0, 1, 38000.0 },
                { "ART-005", "Smart TV 43 Pulgadas", 0, 1, 420000.0 },
                { "ART-006", "Notebook 15 Pulgadas", 1, 2, 780000.0 },
                { "ART-007", "Impresora Multifunción", 1, 3, 210000.0 },
                { "ART-008", "Cartucho de Tinta Negra", 2, 3, 12500.0 },
                { "ART-009", "Resma A4 500 Hojas", 2, -1, 6800.0 },
                { "ART-010", "Lámpara LED 12W", 3, 4, 2900.0 },
                { "ART-011", "Parlante Portátil", 0, 0, 52000.0 },
                { "ART-012", "Webcam Full HD", 1, 0, 33000.0 },
                { "ART-013", "Disco Externo 1TB", 1, 2, 68000.0 },
                { "ART-014", "Pendrive 64GB", 1, 2, 7900.0 },
                { "ART-015", "Taladro Percutor", 4, 5, 89000.0 },
                { "ART-016", "Amoladora Angular", 4, 5, 76000.0 },
                { "ART-017", "Set de Destornilladores", 4, -1, 9800.0 },
                { "ART-018", "Cafetera Eléctrica", 3, 4, 58000.0 },
                { "ART-019", "Licuadora 600W", 3, 4, 41000.0 },
                { "ART-020", "Carpeta A4 Anillos", 2, -1, 3200.0 },
                { "ART-021", "Calculadora Científica", 2, -1, 14500.0 },
                { "ART-022", "Router WiFi 6", 1, 2, 64000.0 },
                { "ART-023", "Tablet 10 Pulgadas", 0, 1, 260000.0 },
                { "ART-024", "Aspiradora Robot", 3, 4, 330000.0 },
                { "ART-025", "Caja de Herramientas", 4, 5, 28000.0 },
                { "ART-026", "Cable HDMI 2m", 1, -1, 4500.0 },
                { "ART-027", "Mochila para Notebook", 2, -1, 24000.0 },
                { "ART-028", "Ventilador de Pie", 3, 4, 47000.0 },
                { "ART-029", "Microondas 20L", 3, 1, 125000.0 },
                { "ART-030", "Mouse Pad Gamer", 1, 0, 6500.0 },
                { "ART-031", "Auricular con Micrófono", 1, 0, 21000.0 },
                { "ART-032", "Tóner Láser Negro", 2, 2, 38000.0 },
                { "ART-033", "Agenda Anual", 2, -1, 5200.0 },
                { "ART-034", "Lámpara de Escritorio", 3, 4, 11800.0 },
                { "ART-035", "Sierra Circular", 4, 5, 134000.0 },
                { "ART-036", "Juego de Llaves Combinadas", 4, -1, 32000.0 },
                { "ART-037", "Monitor 27 Pulgadas", 0, 1, 230000.0 },
                { "ART-038", "Teclado Inalámbrico", 1, 0, 19000.0 },
                { "ART-039", "Estabilizador de Tensión", 0, -1, 27500.0 },
                { "ART-040", "Hervidor Eléctrico", 3, 4, 22000.0 },
                { "ART-041", "Perforadora de Papel", 2, -1, 3900.0 },
                { "ART-042", "Smartwatch", 0, 1, 95000.0 },
        };
        List<Articulo> articulos = new ArrayList<>();
        for (int i = 0; i < datosArticulos.length; i++) {
            Object[] d = datosArticulos[i];
            int marcaIdx = (int) d[3];
            AlicuotaIva alicuota = alicuotas.get(ALICUOTA_POR_POSICION[i % ALICUOTA_POR_POSICION.length]);
            articulos.add(articulo((String) d[0], (String) d[1], rubros.get((int) d[2]),
                    marcaIdx >= 0 ? marcas.get(marcaIdx) : null, alicuota));
        }

        // ---- listas de precio (netos, sin IVA): no todas con todos los artículos y con precios distintos
        ListaPrecio mayorista = listaPrecio("LP-MAY", "Lista Mayorista");
        ListaPrecio minorista = listaPrecio("LP-MIN", "Lista Minorista");
        ListaPrecio distribuidor = listaPrecio("LP-DIS", "Lista Distribuidor");

        Map<Integer, ListaPrecioArticulo> preciosMay = new HashMap<>();
        Map<Integer, ListaPrecioArticulo> preciosMin = new HashMap<>();
        Map<Integer, ListaPrecioArticulo> preciosDis = new HashMap<>();
        for (int i = 0; i < articulos.size(); i++) {
            double base = (double) datosArticulos[i][4];
            preciosMay.put(i, precio(mayorista, articulos.get(i), base, "0.80"));       // 42 artículos
            if (i % 5 != 0) {
                preciosMin.put(i, precio(minorista, articulos.get(i), base, "1.15"));   // 34 artículos
            }
            if (i < 15) {
                preciosDis.put(i, precio(distribuidor, articulos.get(i), base, "0.70")); // 15 artículos
            }
        }

        // ---- facturas: 25 PENDIENTE, 5 ERROR_AFIP y 4 PENDIENTE eliminadas (fechaBaja, no se listan); ninguna
        // EMITIDA ni ANULADA (solo se anula una emitida), sin número, CAE ni fecha de emisión
        String[] motivos = {
                "[10015] Si el campo DocTipo = 80, el DocNro debe ser un CUIT válido.",
                "Timeout al conectar con el WebService de AFIP.",
                "[10048] ImpTotal debe ser igual a la suma de ImpTotConc + ImpOpEx + ImpNeto + ImpTrib + ImpIVA.",
                "[10002] El punto de venta no está habilitado para emitir este tipo de comprobante.",
                "No se pudo obtener el ticket de acceso de ARCA (WSAA)."
        };
        String[] observaciones = {
                "Pendiente de emisión", null, "Presupuesto aprobado por el cliente", "Pedido de librería",
                "Venta mayorista", "Reposición trimestral", null, "Entrega en sucursal"
        };

        List<ListaPrecio> listas = List.of(mayorista, minorista, distribuidor);
        List<Map<Integer, ListaPrecioArticulo>> precios = List.of(preciosMay, preciosMin, preciosDis);
        Random azar = new Random(42);
        int totalFacturas = 34;
        for (int k = 0; k < totalFacturas; k++) {
            EstadoFactura estado = k >= 25 && k < 30 ? EstadoFactura.ERROR_AFIP : EstadoFactura.PENDIENTE;
            boolean eliminada = k >= 30;
            String motivo = estado == EstadoFactura.ERROR_AFIP ? motivos[k - 25] : null;
            int l = k % 3;
            Map<Integer, ListaPrecioArticulo> preciosLista = precios.get(l);
            List<Integer> disponibles = new ArrayList<>(new java.util.TreeSet<>(preciosLista.keySet()));

            Set<Integer> elegidos = new HashSet<>();
            List<Fila> filas = new ArrayList<>();
            int cantidadRenglones = 1 + k % 3;
            while (filas.size() < cantidadRenglones) {
                int articulo = disponibles.get(azar.nextInt(disponibles.size()));
                if (elegidos.add(articulo)) {
                    filas.add(new Fila(articulo, 1 + azar.nextInt(10), BONIFICACIONES[azar.nextInt(BONIFICACIONES.length)]));
                }
            }

            // fechaAlta (fecha de creación) repartida en 3 semanas
            Date fechaAlta = new Date(ahora.getTime() - (k % 21) * DIA_MS - (k * 37L % 600) * 60_000L);

            factura(estado, eliminada, fechaAlta, motivo, clientes.get((k * 5 + k / 3) % clientes.size()), listas.get(l),
                    preciosLista, puntoVentaWs, k % 8 == 7 ? dolar : pesos,
                    observaciones[k % observaciones.length], filas);
        }

        System.out.println("Datos de prueba cargados con éxito.");
    }

    // ------------------------------------------------------------------ facturas

    /** Renglón de factura: índice de artículo, cantidad entera y % de bonificación. */
    private record Fila(int articulo, int cantidad, BigDecimal bonificacion) {
    }

    /**
     * Crea una factura con importes calculados igual que el service (CalculadoraIva, alícuota del artículo
     * como snapshot, 0 % si el emisor no discrimina IVA). La condición IVA es la del cliente.
     */
    private void factura(EstadoFactura estado, boolean eliminada, Date fechaAlta, String motivoRechazo, Cliente cliente, ListaPrecio lista,
                         Map<Integer, ListaPrecioArticulo> precios, PuntoVenta pv, TipoMoneda moneda,
                         String observaciones, List<Fila> filas) {
        FacturaVenta f = FacturaVenta.builder()
                .numero(null)
                .fechaEmision(null)
                .puntoVenta(pv)
                .cliente(cliente)
                .listaPrecio(lista)
                .condicionIva(cliente.getCondicionIva())
                .tipoMoneda(moneda)
                .cae(null)
                .caeFechaVencimiento(null)
                .resultadoAfip(motivoRechazo != null ? "R" : null)
                .motivoRechazo(motivoRechazo)
                .estado(estado)
                .fechaBaja(eliminada ? ahora : null)
                .usuarioBaja(eliminada ? admin : null)
                .observaciones(observaciones)
                .fechaAlta(fechaAlta)
                .fechaModificacion(ahora)
                .usuarioCarga(admin)
                .usuarioModificacion(admin)
                .detalles(new ArrayList<>())
                .build();

        List<ImportesRenglon> importes = new ArrayList<>();
        for (Fila fi : filas) {
            ListaPrecioArticulo lpa = precios.get(fi.articulo());
            if (lpa == null) {
                throw new IllegalStateException("El artículo " + fi.articulo() + " no está en la lista " + lista.getDenominacion());
            }
            AlicuotaIva alicuotaArticulo = lpa.getArticulo().getAlicuotaIva();
            Alicuota alicuota = CalculadoraIva.alicuotaAplicable(emisor.discriminaIva(),
                    alicuotaArticulo.getCodigoAfip(), alicuotaArticulo.getPorcentaje());
            ImportesRenglon importe = CalculadoraIva.calcularRenglon(lpa.getPrecioVenta(), fi.cantidad(),
                    fi.bonificacion(), alicuota.porcentaje());
            importes.add(importe);
            f.getDetalles().add(FacturaVentaDetalle.builder()
                    .factura(f)
                    .listaPrecioArticulo(lpa)
                    .descripcion(lpa.getArticulo().getDenominacion())
                    .cantidad(fi.cantidad())
                    .precioUnitario(lpa.getPrecioVenta())
                    .porcentajeBonificacion(fi.bonificacion())
                    .alicuotaPorcentaje(alicuota.porcentaje())
                    .alicuotaCodigoAfip(alicuota.codigoAfip())
                    .importeNeto(importe.neto())
                    .importeIva(importe.iva())
                    .importeSubtotal(importe.subtotal())
                    .build());
        }
        Totales totales = CalculadoraIva.totalizar(importes);
        f.setImporteNeto(totales.neto());
        f.setImporteIva(totales.iva());
        f.setImporteTotal(totales.total());
        f.setImporteCobrado(BigDecimal.ZERO.setScale(2));
        f.setImporteSaldo(totales.total());
        em.persist(f);
    }

    // ----------------------------------------------------------------- catálogos

    private PuntoVenta puntoVenta(int numero, String descripcion, String tipoEmision, String domicilio) {
        PuntoVenta pv = PuntoVenta.builder()
                .numero(numero).descripcion(descripcion).tipoEmision(tipoEmision).domicilioComercial(domicilio)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(pv);
        return pv;
    }

    private CondicionIva condicionIva(int codigoAfip, String denominacion) {
        CondicionIva c = CondicionIva.builder()
                .codigoAfip(codigoAfip).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(c);
        return c;
    }

    private AlicuotaIva alicuota(int codigoAfip, String porcentaje, String denominacion) {
        AlicuotaIva a = AlicuotaIva.builder()
                .codigoAfip(codigoAfip).porcentaje(new BigDecimal(porcentaje).setScale(2)).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(a);
        return a;
    }

    private TipoMoneda moneda(String codigoAfip, String denominacion, String simbolo) {
        TipoMoneda m = TipoMoneda.builder()
                .codigoAfip(codigoAfip).denominacion(denominacion).simbolo(simbolo)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(m);
        return m;
    }

    private Cliente cliente(String denominacion, String cuitCuil, CondicionIva condicionIva, int n) {
        Contacto contacto = new Contacto();
        contacto.setEmail("cliente" + (n + 1) + "@mail.com");
        contacto.setTelefono("351-555-" + String.format("%04d", 1000 + n));
        em.persist(contacto);

        Domicilio domicilio = Domicilio.builder().nombreCalle("Belgrano").numeroCalle(String.valueOf(100 + n * 10)).build();
        em.persist(domicilio);

        Cliente c = Cliente.builder()
                .cuitCuil(cuitCuil).denominacion(denominacion).contacto(contacto).domicilio(domicilio)
                .condicionIva(condicionIva)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(c);
        return c;
    }

    private Rubro rubro(int codigo, String denominacion) {
        Rubro r = new Rubro();
        r.setCodigo(codigo);
        r.setDenominacion(denominacion);
        r.setFechaAlta(ahora);
        r.setFechaModificacion(ahora);
        r.setUsuarioCarga(admin);
        r.setUsuarioModificacion(admin);
        em.persist(r);
        return r;
    }

    private Marca marca(int codigo, String denominacion) {
        Marca m = Marca.builder()
                .codigo(codigo).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(m);
        return m;
    }

    private Articulo articulo(String codigo, String denominacion, Rubro rubro, Marca marca, AlicuotaIva alicuota) {
        Articulo a = Articulo.builder()
                .codigo(codigo).denominacion(denominacion).rubro(rubro).marca(marca).alicuotaIva(alicuota)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(a);
        return a;
    }

    private ListaPrecio listaPrecio(String codigo, String denominacion) {
        ListaPrecio l = ListaPrecio.builder()
                .codigo(codigo).denominacion(denominacion)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(l);
        return l;
    }

    /** Precio neto de la lista = precio base * factor, a 2 decimales. */
    private ListaPrecioArticulo precio(ListaPrecio lista, Articulo articulo, double precioBase, String factor) {
        BigDecimal precioVenta = BigDecimal.valueOf(precioBase).multiply(new BigDecimal(factor))
                .setScale(2, RoundingMode.HALF_UP);
        ListaPrecioArticulo p = ListaPrecioArticulo.builder()
                .listaPrecio(lista).articulo(articulo).precioVenta(precioVenta)
                .fechaAlta(ahora).fechaModificacion(ahora).usuarioCarga(admin).usuarioModificacion(admin)
                .build();
        em.persist(p);
        return p;
    }
}
