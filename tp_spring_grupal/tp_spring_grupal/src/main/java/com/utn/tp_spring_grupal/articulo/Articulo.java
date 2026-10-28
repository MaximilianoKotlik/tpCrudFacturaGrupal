package com.utn.tp_spring_grupal.articulo;

import com.utn.tp_spring_grupal.alicuotaiva.AlicuotaIva;
import com.utn.tp_spring_grupal.common.AuditoriaApp;
import com.utn.tp_spring_grupal.marca.Marca;
import com.utn.tp_spring_grupal.rubro.Rubro;

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
@AllArgsConstructor 
@Getter
@Setter
@ToString 
@EqualsAndHashCode(callSuper=true) 
public class Articulo extends AuditoriaApp {
    @ManyToOne
    @JoinColumn(name = "id_rubro")
    private Rubro rubro;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;

    @ManyToOne
    @JoinColumn(name = "id_marca")
    private Marca marca;

    /** La alícuota de IVA es propiedad del producto/servicio. */
    @ManyToOne
    @JoinColumn(nullable = false, name = "id_alicuota_iva")
    private AlicuotaIva alicuotaIva;
}