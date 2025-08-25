package co.franquicias.model.producto;

import lombok.*;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    private String id;
    private String sucursalId;
    private String nombre;
    private int stock;
    private Instant createdAt;
    private Instant updatedAt;
}