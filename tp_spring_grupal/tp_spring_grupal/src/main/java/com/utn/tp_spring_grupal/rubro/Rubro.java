package com.utn.tp_spring_grupal.rubro;

import com.utn.tp_spring_grupal.common.AuditoriaApp;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor 
@SuperBuilder
@Getter
@Setter
@ToString
public class Rubro extends AuditoriaApp {
    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    private Integer codigo;
}
