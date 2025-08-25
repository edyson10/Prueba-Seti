package co.franquicias.model.franquicia;

import co.franquicias.model.sucursal.Sucursal;
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
public class Franquicia {
    private String id;
    private String nombre;
    private List<Sucursal> sucursales = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}