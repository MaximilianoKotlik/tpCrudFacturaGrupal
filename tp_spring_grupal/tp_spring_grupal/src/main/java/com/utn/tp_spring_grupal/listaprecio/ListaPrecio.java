package com.utn.tp_spring_grupal.listaprecio;

import com.utn.tp_spring_grupal.common.AuditoriaApp;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;

@Entity
@Table
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ListaPrecio extends AuditoriaApp {
    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;
}