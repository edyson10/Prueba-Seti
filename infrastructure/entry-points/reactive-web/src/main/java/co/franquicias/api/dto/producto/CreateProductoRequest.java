package co.franquicias.api.dto.producto;

public record CreateProductoRequest(
        String nombre,
        int stock
) { }
