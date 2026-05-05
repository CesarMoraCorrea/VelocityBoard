package com.example.VelocityBoard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tableros")
public class Tablero {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private String propietarioId;

    @Builder.Default
    private boolean eliminado = false;

    @Builder.Default
    private Date creadoEn = new Date();
}
