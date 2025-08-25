package co.franquicias.model;

import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface OperacionesFranquiciaPort {
    // franquicia
    Mono<Franquicia> crearFranquicia(String nombre);
    Mono<Franquicia> obtenerFranquicia(String id);
    Mono<Franquicia> obtenerFranquiciaPorNombre(String nombre);
    Mono<String> eliminarFranquiciaPorId(String id);
    Mono<Franquicia> actualizarFranquicia(String franquiciaId, Franquicia cambios);
    Flux<Franquicia> obtenerFranquicias(boolean verProductos);

    // sucursal
    Mono<Sucursal> agregarSucursal(String franquiciaId, String nombreSucursal);
    Mono<Sucursal> obtenerSucursalPorId(String id);
    Flux<Sucursal> obtenerSucursalPorFranquiciaId(String franquiciaId);
    Mono<String> eliminarSucursalPorId(String id);
    Mono<Sucursal> actualizarSucursal(String id, Sucursal cambios);

    Mono<Producto> agregarProducto(String franquiciaId, String sucursalId, String nombreProducto, int stock);
    Mono<Void>     eliminarProducto(String franquiciaId, String sucursalId, String productoId);
    Mono<Producto> actualizarStock(String franquiciaId, String sucursalId, String productoId, int stock);

    Flux<Producto> productosDeSucursal(String franquiciaId, String sucursalId);
    Flux<Producto> todosLosProductos();
    Flux<Producto> buscarProductos(String nombreLike);
    Mono<Producto> actualizarProducto(String id, Producto cambios);

    Mono<Map<String,Object>> productoGlobal(String productoId);
    Flux<Object> todosProductosViewRaw();
    Flux<Map<String, Object>> maxStockPorSucursal(String franquiciaId);
}
