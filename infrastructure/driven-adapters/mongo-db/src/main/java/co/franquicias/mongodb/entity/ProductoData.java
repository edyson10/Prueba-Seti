package co.franquicias.mongodb.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("productos")
@CompoundIndexes({
        @CompoundIndex(name = "ux_producto_sucursal_nombre", def = "{ 'sucursalId': 1, 'nombre': 1 }", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoData {
    @Id
    private String id;

    @Indexed
    private String sucursalId;

    private String nombre;
    private int stock;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
