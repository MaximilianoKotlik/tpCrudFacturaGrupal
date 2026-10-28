package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.cliente.Cliente;
import com.utn.tp_spring_grupal.common.AuditoriaApp;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecio;
import com.utn.tp_spring_grupal.puntoventa.PuntoVenta;
import com.utn.tp_spring_grupal.tipomoneda.TipoMoneda;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
@Table
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = { "detalles" })
@AllArgsConstructor
@ToString(exclude = { "detalles" })
public class FacturaVenta extends AuditoriaApp {
    // Null mientras la factura no fue emitida en AFIP.
    private Long numero;

    /** Null hasta que AFIP aprueba el comprobante. La fecha de creación es fechaAlta (AuditoriaApp). */
    private LocalDate fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_venta_id", nullable = false)
    private PuntoVenta puntoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecio listaPrecio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condicion_iva_id", nullable = false)
    private CondicionIva condicionIva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_moneda_id", nullable = false)
    private TipoMoneda tipoMoneda;

    // Cobro fuera de alcance: las columnas quedan pero no se exponen en los DTOs.
    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal importeCobrado = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal importeSaldo = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importeNeto = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importeTotal = BigDecimal.ZERO;

    /** Código AFIP del comprobante (1 A, 6 B, 11 C). Se fija al emitir; antes se calcula en lectura. */
    private Integer tipoComprobante;

    /** Cotizacion ARCA de la moneda al emitir (PES = 1). Null mientras no se emitio con exito. */
    @Column(precision = 19, scale = 6)
    private BigDecimal cotizacion;

    private String cae;
    private LocalDate caeFechaVencimiento;
    private String resultadoAfip;
    @Column(length = 2000)
    private String motivoRechazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFactura estado;

    private Date fechaAnulacion;
    private String observaciones;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaVentaDetalle> detalles;
}
