package com.utn.tp_spring_grupal.factura;

import com.utn.tp_spring_grupal.common.EntityId;
import com.utn.tp_spring_grupal.listaprecio.ListaPrecioArticulo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString 
public class FacturaVentaDetalle extends EntityId {

    @ManyToOne
    @JoinColumn(nullable = false, name = "id_facturaventa")
    private FacturaVenta factura;

    @ManyToOne
    @JoinColumn(nullable = false, name = "id_listaprecioarticulo")
    private ListaPrecioArticulo listaPrecioArticulo;

    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeBonificacion = BigDecimal.ZERO;

    /** Snapshot de la alícuota aplicada: un cambio futuro en el artículo no altera facturas existentes. */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaPorcentaje = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private int alicuotaCodigoAfip = 3;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importeNeto;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importeIva;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importeSubtotal;
}