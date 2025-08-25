package co.franquicias.usecase.franquicia;

import co.franquicias.model.OperacionesFranquiciaPort;
import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class FranquiciaUseCase {

    private static final Logger logger = Logger.getLogger(FranquiciaUseCase.class.getName());

    private final OperacionesFranquiciaPort repository;

    // ================= Franquicia =================
    public Mono<Franquicia> crearFranquicia(String nombre) {
        return validarNombre(nombre, "El nombre de la franquicia es obligatorio")
                .then(Mono.defer(() -> repository.crearFranquicia(nombre.trim())))
                .doOnSubscribe(s -> logger.info(() -> "[crearFranquicia] nombre=" + nombre))
                .doOnSuccess(f -> logger.info(() -> "[crearFranquicia] creada id=" + (f != null ? f.getId() : "null")))
                .doOnError(e -> logger.severe("[crearFranquicia] error: " + e.getMessage()));
    }

    public Mono<Franquicia> obtenerPorId(String id) {
        return repository.obtenerFranquicia(Objects.requireNonNull(id, "id requerido"))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Franquicia no encontrada")))
                .doOnSubscribe(s -> logger.info(() -> "[obtenerPorId] id=" + id))
                .doOnError(e -> logger.severe("[obtenerPorId] error: " + e.getMessage()));
    }

    public Mono<Franquicia> obtenerFranquiciaPorNombre(String nombre) {
        return validarNombre(nombre, "El nombre de la franquicia es obligatorio")
                .then(Mono.defer(() -> repository.obtenerFranquiciaPorNombre(nombre.trim())))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Franquicia no encontrada")))
                .doOnSubscribe(s -> logger.info(() -> "[obtenerFranquiciaPorNombre] nombre=" + nombre))
                .doOnError(e -> logger.severe("[obtenerFranquiciaPorNombre] error: " + e.getMessage()));
    }

    public Flux<Franquicia> obtenerFranquicias(boolean verProducto) {
        return repository.obtenerFranquicias(verProducto);
    }

    public Mono<String> eliminarFranquiciaPorId(String id) {
        return repository.eliminarFranquiciaPorId(Objects.requireNonNull(id, "id requerido"))
                .doOnSubscribe(s -> logger.info(() -> "[eliminarFranquiciaPorId] id=" + id))
                .doOnError(e -> logger.severe("[eliminarFranquiciaPorId] error: " + e.getMessage()));
    }

    public Mono<Franquicia> actualizarFranquicia(String franquiciaId, Franquicia cambios) {
        return Mono.defer(() -> {
            if (cambios.getNombre() != null) {
                String n = cambios.getNombre().trim();
                if (n.isBlank()) {
                    return Mono.error(new IllegalArgumentException("El nombre de la franquicia no puede estar vacío"));
                }
                cambios.setNombre(n);
            }
            return repository.actualizarFranquicia(franquiciaId, cambios)
                    .doOnSubscribe(s -> logger.info(() -> "[actualizarFranquicia] id=" + franquiciaId))
                    .doOnError(e -> logger.severe("[actualizarFranquicia] error: " + e.getMessage()));
        });
    }

    // ================= Sucursal =================
    public Mono<Sucursal> agregarSucursal(String franquiciaId, String nombreSucursal) {
        return validarNombre(nombreSucursal, "El nombre de la sucursal es obligatorio")
                .then(Mono.defer(() -> repository.agregarSucursal(franquiciaId, nombreSucursal.trim())))
                .doOnSubscribe(s -> logger.info(() -> "[agregarSucursal] fId=" + franquiciaId + ", nombre=" + nombreSucursal))
                .doOnError(e -> logger.severe("[agregarSucursal] error: " + e.getMessage()));
    }

    public Mono<Sucursal> obtenerSucursalPorId(String id) {
        return repository.obtenerSucursalPorId(Objects.requireNonNull(id, "id requerido"))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sucursal no encontrada")))
                .doOnSubscribe(s -> logger.info(() -> "[obtenerSucursalPorId] id=" + id))
                .doOnError(e -> logger.severe("[obtenerSucursalPorId] error: " + e.getMessage()));
    }

    public Flux<Sucursal> obtenerSucursalPorFranquiciaId(String franquiciaId) {
        return repository.obtenerSucursalPorFranquiciaId(franquiciaId);
    }


    public Mono<String> eliminarSucursalPorId(String id) {
        return repository.eliminarSucursalPorId(Objects.requireNonNull(id, "id requerido"))
                .doOnSubscribe(s -> logger.info(() -> "[eliminarSucursalPorId] id=" + id))
                .doOnError(e -> logger.severe("[eliminarSucursalPorId] error: " + e.getMessage()));
    }

    public Mono<Sucursal> actualizarSucursal(String id, Sucursal cambios) {
        return Mono.defer(() -> {
            if (cambios.getNombre() != null) {
                String n = cambios.getNombre().trim();
                if (n.isBlank()) {
                    return Mono.error(new IllegalArgumentException("El nombre de la sucursal no puede estar vacío"));
                }
                cambios.setNombre(n);
            }
            return repository.actualizarSucursal(id, cambios)
                    .doOnSubscribe(s -> logger.info(() -> "[actualizarSucursal] id=" + id))
                    .doOnError(e -> logger.severe("[actualizarSucursal] error: " + e.getMessage()));
        });
    }

    // ================= Producto =================

    public Mono<Producto> agregarProducto(String franquiciaId, String sucursalId, String nombreProducto, int stock) {
        return Mono.when(
                        validarNombre(nombreProducto, "El nombre del producto es obligatorio"),
                        validarStockNoNegativo(stock)
                )
                .then(Mono.defer(() -> repository.agregarProducto(franquiciaId, sucursalId, nombreProducto.trim(), stock)))
                .doOnSubscribe(s -> logger.info(() ->
                        "[agregarProducto] fId=" + franquiciaId + ", sId=" + sucursalId + ", nombre=" + nombreProducto + ", stock=" + stock))
                .doOnError(e -> logger.severe("[agregarProducto] error: " + e.getMessage()));
    }

    public Mono<Void> eliminarProducto(String franquiciaId, String sucursalId, String productoId) {
        return repository.eliminarProducto(franquiciaId, sucursalId, productoId)
                .doOnSubscribe(s -> logger.info(() ->
                        "[eliminarProducto] fId=" + franquiciaId + ", sId=" + sucursalId + ", pId=" + productoId))
                .doOnError(e -> logger.severe("[eliminarProducto] error: " + e.getMessage()));
    }

    public Mono<Producto> actualizarStock(String franquiciaId, String sucursalId, String productoId, int stock) {
        return validarStockNoNegativo(stock)
                .then(Mono.defer(() -> repository.actualizarStock(franquiciaId, sucursalId, productoId, stock)))
                .doOnSubscribe(s -> logger.info(() ->
                        "[actualizarStock] fId=" + franquiciaId + ", sId=" + sucursalId + ", pId=" + productoId + ", stock=" + stock))
                .doOnError(e -> logger.severe("[actualizarStock] error: " + e.getMessage()));
    }

    public Mono<Producto> actualizarProducto(String id, Producto cambios) {
        return Mono.defer(() -> {
            if (cambios.getNombre() != null) {
                String n = cambios.getNombre().trim();
                if (n.isBlank()) {
                    return Mono.error(new IllegalArgumentException("El nombre del producto no puede estar vacío"));
                }
                cambios.setNombre(n);
            }
            if (cambios.getStock() < 0) {
                return Mono.error(new IllegalArgumentException("Stock negativo no permitido"));
            }
            return repository.actualizarProducto(id, cambios)
                    .doOnSubscribe(s -> logger.info(() -> "[actualizarProducto] id=" + id))
                    .doOnError(e -> logger.severe("[actualizarProducto] error: " + e.getMessage()));
        });
    }

    public Flux<Map<String,Object>> maxStockPorSucursal(String franquiciaId) {
        return repository.maxStockPorSucursal(franquiciaId)
                .doOnSubscribe(s -> logger.info(() -> "[maxStockPorSucursal] fId=" + franquiciaId))
                .doOnError(e -> logger.severe("[maxStockPorSucursal] error: " + e.getMessage()));
    }

    public Flux<Producto> getAllProductos() {
        return repository.todosLosProductos()
                .doOnSubscribe(s -> logger.info(() -> "[getAllProductos]"))
                .doOnError(e -> logger.severe("[getAllProductos] error: " + e.getMessage()));
    }

    public Mono<Map<String,Object>> getProductoGlobal(String productoId) {
        return repository.productoGlobal(productoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Producto no encontrado")))
                .doOnSubscribe(s -> logger.info(() -> "[getProductoGlobal] pId=" + productoId))
                .doOnError(e -> logger.severe("[getProductoGlobal] error: " + e.getMessage()));
    }

    public Flux<Producto> searchProductosGlobal(String nombreLike) {
        Flux<Producto> query = repository.buscarProductos(nombreLike == null ? "" : nombreLike.trim());
        return query.switchIfEmpty(repository.todosLosProductos())
                .doOnSubscribe(s -> logger.info(() -> "[searchProductosGlobal] q='" + nombreLike + "'"))
                .doOnError(e -> logger.severe("[searchProductosGlobal] error: " + e.getMessage()));
    }

    public Flux<Object> getAllProductosViewRaw() {
        return repository.todosProductosViewRaw()
                .doOnSubscribe(s -> logger.info(() -> "[getAllProductosViewRaw]"))
                .doOnError(e -> logger.severe("[getAllProductosViewRaw] error: " + e.getMessage()));
    }

    public Mono<Map<String, Object>> getProductoGlobalViewRaw(String productoId) {
        return repository.productoGlobal(productoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Producto no encontrado")))
                .doOnSubscribe(s -> logger.info(() -> "[getProductoGlobalViewRaw] pId=" + productoId))
                .doOnError(e -> logger.severe("[getProductoGlobalViewRaw] error: " + e.getMessage()));
    }

    public Flux<Producto> getProductosDeSucursal(String franquiciaId, String sucursalId) {
        return repository.productosDeSucursal(franquiciaId, sucursalId)
                .doOnSubscribe(s -> logger.info(() -> "[getProductosDeSucursal] fId=" + franquiciaId + ", sId=" + sucursalId))
                .doOnError(e -> logger.severe("[getProductosDeSucursal] error: " + e.getMessage()));
    }

    private Mono<Void> validarNombre(String valor, String mensaje) {
        return Mono.justOrEmpty(valor)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(mensaje)))
                .then();
    }

    private Mono<Void> validarStockNoNegativo(int stock) {
        return Mono.just(stock)
                .filter(s -> s >= 0)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Stock negativo no permitido")))
                .then();
    }
}
