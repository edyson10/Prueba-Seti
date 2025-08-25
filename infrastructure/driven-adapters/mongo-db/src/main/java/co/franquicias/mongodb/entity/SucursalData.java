package co.franquicias.mongodb.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("sucursales")
@CompoundIndexes({
        @CompoundIndex(name = "ux_sucursal_franquicia_nombre", def = "{ 'franquiciaId': 1, 'nombre': 1 }", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SucursalData {
    @Id
    private String id;

    @Indexed
    private String franquiciaId; // FK manual

    private String nombre;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
