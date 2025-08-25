package co.franquicias.api.dto.producto;

public record ProductoViewDTO(
        String productoId,
        String productoNombre,
        int stock,
        String franquiciaId,
        String franquiciaNombre,
        String sucursalId,
        String sucursalNombre
) {}
