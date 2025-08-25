package co.franquicias.model.sucursal;

import co.franquicias.model.producto.Producto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Sucursal {
    private String id;
    private String franquiciaId;
    private String nombre;
    private List<Producto> productos = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
