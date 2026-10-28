package com.utn.tp_spring_grupal.cliente;

import com.utn.tp_spring_grupal.common.AuditoriaApp;
import com.utn.tp_spring_grupal.condicioniva.CondicionIva;

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
@ToString 
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true)
public class Cliente extends AuditoriaApp {
    @Column(nullable = false)
    private String cuitCuil;

    @Column(nullable = false)
    private String denominacion;

    @OneToOne
    @JoinColumn(nullable = false, name = "id_contacto")
    private Contacto contacto;

    @OneToOne
    @JoinColumn(nullable = false, name = "id_domicilio")
    private Domicilio domicilio;

    /** Condición frente al IVA del receptor: define el tipo de comprobante (CondicionIVAReceptorId). */
    @ManyToOne
    @JoinColumn(nullable = false, name = "id_condicion_iva")
    private CondicionIva condicionIva;
}