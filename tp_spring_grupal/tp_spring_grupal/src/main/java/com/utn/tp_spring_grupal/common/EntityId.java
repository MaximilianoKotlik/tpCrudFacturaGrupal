package com.utn.tp_spring_grupal.common;



import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@SuperBuilder
@MappedSuperclass
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class EntityId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
}