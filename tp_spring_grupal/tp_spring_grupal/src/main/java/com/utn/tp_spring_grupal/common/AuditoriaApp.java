package com.utn.tp_spring_grupal.common;

import com.utn.tp_spring_grupal.usuario.Usuario;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.util.Date;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor 
public abstract class AuditoriaApp extends EntityId {
    @Column(nullable = false)
    protected Date fechaAlta;

    protected Date fechaBaja;

    @Column(nullable = false)
    protected Date fechaModificacion;

    @ManyToOne
    @JoinColumn(nullable = false, name = "id_usuario_carga")
    protected Usuario usuarioCarga;

    @ManyToOne
    @JoinColumn(name = "id_usuario_baja")
    protected Usuario usuarioBaja;

    @ManyToOne
    @JoinColumn(nullable = false, name = "id_usuario_modificado")
    protected Usuario usuarioModificacion;
}