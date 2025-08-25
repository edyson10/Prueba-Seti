package co.franquicias.mongodb.facade;

import co.franquicias.model.OperacionesFranquiciaPort;
import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import co.franquicias.mongodb.adapter.FranquiciaAdapter;
import co.franquicias.mongodb.adapter.ProductoAdapter;
import co.franquicias.mongodb.adapter.SucursalAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class FranquiciasMongoFacadeAdapter implements OperacionesFranquiciaPort {

    private final FranquiciaAdapter franquiciaAdapter;
    private final SucursalAdapter   sucursalAdapter;
    private final ProductoAdapter   productoAdapter;

    // ================== HELPERS DE HIDRATACIÓN ==================

    /** Sucursal -> Sucursal con productos embebidos. */
    private Mono<Sucursal> hydrateSucursalWithProductos(Sucursal s) {
        return productoAdapter.listarPorSucursal(s.getId())
                .collectList()
                .map(prods -> s.toBuilder().productos(prods).build());
    }

    /**
     * Franquicia -> Franquicia con sucursales embebidas; si includeProductos=true,
     * cada sucursal viene con productos embebidos.
     */
    private Mono<Franquicia> hydrateFranquicia(String franquiciaId, Franquicia f, boolean includeProductos) {
        Flux<Sucursal> sucs = sucursalAdapter.listarPorFranquicia(franquiciaId);
        Flux<Sucursal> sucsHydrated = includeProductos
                ? sucs.flatMap(this::hydrateSucursalWithProductos)
                : sucs;

        return sucsHydrated.collectList()
                .map(list -> f.toBuilder().sucursales(list).build());
    }

    // ================== OPERACIONES ==================

    @Override
    public Mono<Franquicia> crearFranquicia(String nombre) {
        return franquiciaAdapter.crearFranquicia(nombre);
    }

    @Override
    public Mono<Franquicia> obtenerFranquiciaPorNombre(String nombre) {
        // Ahora devuelve franquicia + sucursales + productos
        return franquiciaAdapter.obtenerPorNombre(nombre)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Franquicia no existe")))
                .flatMap(f -> hydrateFranquicia(f.getId(), f, true));
    }

    @Override
    public Flux<Franquicia> obtenerFranquicias(boolean verProductos) {
        return franquiciaAdapter.obtenerFranquicias()
                .flatMap(f -> hydrateFranquicia(f.getId(), f, verProductos));
    }

    @Override
    public Mono<String> eliminarFranquiciaPorId(String id) {
        return franquiciaAdapter.eliminarPorId(id);
    }

    @Override
    public Mono<Franquicia> actualizarFranquicia(String franquiciaId, Franquicia cambios) {
        return franquiciaAdapter.actualizarFranquicia(franquiciaId, cambios);
    }

    @Override
    public Mono<Franquicia> obtenerFranquicia(String id) {
        return franquiciaAdapter.obtenerPorId(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Franquicia no existe")))
                .flatMap(f -> hydrateFranquicia(f.getId(), f, true));
    }

    @Override
    public Mono<Sucursal> agregarSucursal(String franquiciaId, String nombre) {
        return franquiciaAdapter.obtenerPorId(franquiciaId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Franquicia no existe")))
                .then(sucursalAdapter.crear(franquiciaId, nombre));
    }

    @Override
    public Mono<Sucursal> obtenerSucursalPorId(String id) {
        return sucursalAdapter.obtenerPorId(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sucursal no existe")))
                .flatMap(this::hydrateSucursalWithProductos);
    }

    @Override
    public Flux<Sucursal> obtenerSucursalPorFranquiciaId(String franquiciaId) {
        return sucursalAdapter.listarPorFranquicia(franquiciaId)
                .switchIfEmpty(
                        Flux.defer(() ->
                                franquiciaAdapter.obtenerPorId(franquiciaId)
                                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Franquicia no encontrada")))
                                        .thenMany(Flux.<Sucursal>empty())
                        )
                )
                .flatMap(this::hydrateSucursalWithProductos)
                .doOnSubscribe(s -> log.info("[obtenerSucursalPorFranquiciaId] fId={}", franquiciaId))
                .doOnComplete(() -> log.info("[obtenerSucursalPorFranquiciaId] fId={} completado", franquiciaId))
                .doOnError(e -> log.error("[obtenerSucursalPorFranquiciaId] fId={} error: {}", franquiciaId, e.toString()));
    }

    @Override
    public Mono<String> eliminarSucursalPorId(String id) {
        return sucursalAdapter.eliminarPorId(id);
    }

    @Override
    public Mono<Sucursal> actualizarSucursal(String id, Sucursal cambios) {
        return sucursalAdapter.actualizarSucursal(id, cambios);
    }

    @Override
    public Mono<Producto> agregarProducto(String franquiciaId, String sucursalId, String nombre, int stock) {
        return sucursalAdapter.obtenerPorId(sucursalId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sucursal no existe")))
                .flatMap(s -> Objects.equals(franquiciaId, s.getFranquiciaId())
                        ? productoAdapter.crear(sucursalId, nombre, stock)
                        : Mono.error(new IllegalStateException("Sucursal no pertenece a la franquicia")));
    }

    @Override
    public Mono<Void> eliminarProducto(String franquiciaId, String sucursalId, String productoId) {
        return sucursalAdapter.obtenerPorId(sucursalId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sucursal no existe")))
                .flatMap(s -> Objects.equals(franquiciaId, s.getFranquiciaId())
                        ? productoAdapter.eliminarPorId(productoId).then()
                        : Mono.error(new IllegalStateException("Sucursal no pertenece a la franquicia")));
    }

    @Override
    public Mono<Producto> actualizarStock(String franquiciaId, String sucursalId, String productoId, int stock) {
        return sucursalAdapter.obtenerPorId(sucursalId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sucursal no existe")))
                .flatMap(s -> Objects.equals(franquiciaId, s.getFranquiciaId())
                        ? productoAdapter.actualizarStock(productoId, stock)
                        : Mono.error(new IllegalStateException("Sucursal no pertenece a la franquicia")));
    }

    @Override
    public Flux<Producto> productosDeSucursal(String franquiciaId, String sucursalId) {
        return sucursalAdapter.obtenerPorId(sucursalId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sucursal no existe")))
                .flatMapMany(s -> {
                    if (!Objects.equals(franquiciaId, s.getFranquiciaId())) {
                        return Flux.error(new IllegalStateException("Sucursal no pertenece a la franquicia"));
                    }
                    return productoAdapter.listarPorSucursal(sucursalId);
                });
    }

    @Override
    public Flux<Producto> todosLosProductos() {
        return productoAdapter.findAll();
    }

    @Override
    public Mono<Producto> actualizarProducto(String id, Producto cambios) {
        return productoAdapter.actualizarProducto(id, cambios);
    }

    @Override
    public Flux<Producto> buscarProductos(String nombreLike) {
        return productoAdapter.buscarPorNombreLike(nombreLike == null ? "" : nombreLike.trim());
    }

    @Override
    public Mono<Map<String, Object>> productoGlobal(String productoId) {
        return productoAdapter.findById(productoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Producto no encontrado")))
                .zipWhen(p -> sucursalAdapter.obtenerPorId(p.getSucursalId())
                        .switchIfEmpty(Mono.error(new IllegalStateException("Sucursal del producto no existe"))))
                .map(t -> Map.of(
                        "productoId",      t.getT1().getId(),
                        "productoNombre",  t.getT1().getNombre(),
                        "stock",           t.getT1().getStock(),
                        "sucursalId",      t.getT2().getId(),
                        "sucursalNombre",  t.getT2().getNombre(),
                        "franquiciaId",    t.getT2().getFranquiciaId()
                ));
    }

    @Override
    public Flux<Object> todosProductosViewRaw() {
        return productoAdapter.findAll()
                .flatMap(p -> sucursalAdapter.obtenerPorId(p.getSucursalId())
                        .map(s -> Map.<String, Object>of(
                                "productoId",      p.getId(),
                                "productoNombre",  p.getNombre(),
                                "stock",           p.getStock(),
                                "sucursalId",      s.getId(),
                                "sucursalNombre",  s.getNombre(),
                                "franquiciaId",    s.getFranquiciaId()
                        )))
                .cast(Object.class);
    }

    @Override
    public Flux<Map<String, Object>> maxStockPorSucursal(String franquiciaId) {
        return sucursalAdapter.listarPorFranquicia(franquiciaId)
                .flatMap(suc ->
                        productoAdapter.listarPorSucursal(suc.getId())
                                .sort(java.util.Comparator.comparingInt(Producto::getStock).reversed())
                                .next()
                                .map(prod -> Map.<String,Object>of(
                                        "sucursalId",     suc.getId(),
                                        "sucursalNombre", suc.getNombre(),
                                        "productoId",     prod.getId(),
                                        "productoNombre", prod.getNombre(),
                                        "stock",          prod.getStock()
                                ))
                                .defaultIfEmpty(Map.of(
                                        "sucursalId",     suc.getId(),
                                        "sucursalNombre", suc.getNombre(),
                                        "productoId",     null,
                                        "productoNombre", null,
                                        "stock",          0
                                ))
                );
    }
}
