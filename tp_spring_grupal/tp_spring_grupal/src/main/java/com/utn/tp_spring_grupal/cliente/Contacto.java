package com.utn.tp_spring_grupal.cliente;

import com.utn.tp_spring_grupal.common.EntityId;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table
@EqualsAndHashCode(callSuper = true)
@ToString 
@AllArgsConstructor 
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder 
public class Contacto extends EntityId {
    private String email;
    private String telefono;
    private String celular;
}
