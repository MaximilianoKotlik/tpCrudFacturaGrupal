package com.utn.tp_spring_grupal.cliente;

import com.utn.tp_spring_grupal.common.EntityId;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.ToString;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor 
@ToString 
public class Domicilio extends EntityId {
    private String nombreCalle;
    private String numeroCalle;
}
