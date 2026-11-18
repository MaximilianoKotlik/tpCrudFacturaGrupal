package com.utn.tp_spring_grupal.condicioniva;

import com.utn.tp_spring_grupal.common.AuditoriaApp;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;

@Entity
@Table
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString 
@AllArgsConstructor 
public class CondicionIva extends AuditoriaApp {
    /** Código AFIP de Consumidor Final: la única condición posible para una factura sin cliente. */
    public static final int CODIGO_CONSUMIDOR_FINAL = 5;

    @Column(nullable = false)
    private int codigoAfip;

    @Column(nullable = false)
    private String denominacion;
}
