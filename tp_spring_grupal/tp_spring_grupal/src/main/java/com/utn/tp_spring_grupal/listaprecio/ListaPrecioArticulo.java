package com.utn.tp_spring_grupal.listaprecio;

import com.utn.tp_spring_grupal.articulo.Articulo;
import com.utn.tp_spring_grupal.common.AuditoriaApp;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@ToString
public class ListaPrecioArticulo extends AuditoriaApp {
    @ManyToOne
    @JoinColumn(nullable = false, name = "id_listaprecio")
    private ListaPrecio listaPrecio;

    /** Precio neto (sin IVA). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precioVenta;

    @ManyToOne
    @JoinColumn(nullable = false, name = "id_articulo")
    private Articulo articulo;
}
