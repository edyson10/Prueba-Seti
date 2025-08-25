package co.franquicias.mongodb.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "franquicias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FranquiciaData {

    @Id
    private String id;

    @Indexed(unique = true)
    private String nombre;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
