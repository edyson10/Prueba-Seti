package co.franquicias.api;

import co.franquicias.api.dto.franquicia.CreateFranquiciaRequest;
import co.franquicias.api.dto.franquicia.UpdateFranquiciaRequest;
import co.franquicias.api.dto.producto.CreateProductoRequest;
import co.franquicias.api.dto.producto.ProductoViewDTO;
import co.franquicias.api.dto.producto.UpdateProductoRequest;
import co.franquicias.api.dto.producto.UpdateStockRequest;
import co.franquicias.api.dto.sucursal.CreateSucursalRequest;
import co.franquicias.api.dto.sucursal.UpdateSucursalRequest;
import co.franquicias.api.mapper.DtoMappers;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.usecase.franquicia.FranquiciaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Handler {

    private final FranquiciaUseCase useCase;

    // ---------- Franquicia ----------
    public Mono<ServerResponse> crearFranquicia(ServerRequest req) {
        return req.bodyToMono(CreateFranquiciaRequest.class)
                .flatMap(body -> useCase.crearFranquicia(body.nombre()))
                .flatMap(f -> ServerResponse
                        .created(URI.create("/api/franquicias/" + f.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(f))
                ;
    }

    public Mono<ServerResponse> obtenerFranquicias(ServerRequest req) {
        boolean verProducto = req.queryParam("includeProductos")
                .map(String::toLowerCase)
                .map(v -> v.equals("true") || v.equals("1") || v.equals("yes"))
                .orElse(false);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(useCase.obtenerFranquicias(verProducto), Franquicia.class);
    }

    public Mono<ServerResponse> obtenerFranquicia(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        return useCase.obtenerPorId(fId)
                .flatMap(f -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(f));
    }

    public Mono<ServerResponse> obtenerFranquiciaPorNombre(ServerRequest req) {
        String nombre = req.queryParam("nombre").orElse("");
        return useCase.obtenerFranquiciaPorNombre(nombre)
                .flatMap(f -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(f));
    }

    public Mono<ServerResponse> eliminarFranquicia(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        return useCase.eliminarFranquiciaPorId(fId)
                .flatMap(msg -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", msg)));
    }

    public Mono<ServerResponse> actualizarFranquicia(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        return req.bodyToMono(UpdateFranquiciaRequest.class)
                .map(b -> Franquicia.builder().nombre(b.nombre()).build())
                .flatMap(patch -> useCase.actualizarFranquicia(fId, patch))
                .flatMap(f -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(f));
    }

    // ---------- Sucursal ----------
    public Mono<ServerResponse> agregarSucursal(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        return req.bodyToMono(CreateSucursalRequest.class)
                .flatMap(b -> useCase.agregarSucursal(fId, b.nombre()))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s));
    }

    public Mono<ServerResponse> listarSucursalesDeFranquicia(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(useCase.obtenerSucursalPorFranquiciaId(fId), Sucursal.class);
    }

    public Mono<ServerResponse> obtenerSucursal(ServerRequest req) {
        String sId = req.pathVariable("sucursalId");
        return useCase.obtenerSucursalPorId(sId)
                .flatMap(suc -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(suc));
    }

    public Mono<ServerResponse> eliminarSucursal(ServerRequest req) {
        String sId = req.pathVariable("sucursalId");
        return useCase.eliminarSucursalPorId(sId)
                .flatMap(msg -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", msg)));
    }

    public Mono<ServerResponse> actualizarSucursal(ServerRequest req) {
        String sId = req.pathVariable("sucursalId");
        return req.bodyToMono(UpdateSucursalRequest.class)
                .map(b -> Sucursal.builder()
                        .nombre(b.nombre())
                        .franquiciaId(b.franquiciaId())
                        .build())
                .flatMap(patch -> useCase.actualizarSucursal(sId, patch))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s));
    }

    // ---------- Producto ----------
    public Mono<ServerResponse> agregarProducto(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        String sId = req.pathVariable("sucursalId");
        return req.bodyToMono(CreateProductoRequest.class)
                .flatMap(b -> useCase.agregarProducto(fId, sId, b.nombre(), b.stock()))
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p));
    }

    public Mono<ServerResponse> eliminarProducto(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        String sId = req.pathVariable("sucursalId");
        String pId = req.pathVariable("productoId");
        return useCase.eliminarProducto(fId, sId, pId)
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> actualizarStock(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        String sId = req.pathVariable("sucursalId");
        String pId = req.pathVariable("productoId");
        return req.bodyToMono(UpdateStockRequest.class)
                .flatMap(b -> useCase.actualizarStock(fId, sId, pId, b.stock()))
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p));
    }

    public Mono<ServerResponse> actualizarProducto(ServerRequest req) {
        String pId = req.pathVariable("productoId");
        return req.bodyToMono(UpdateProductoRequest.class)
                .map(b -> Producto.builder()
                        .nombre(b.nombre())
                        .stock(b.stock() != null ? b.stock() : 0)
                        .sucursalId(b.sucursalId())
                        .build())
                .flatMap(patch -> useCase.actualizarProducto(pId, patch))
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p));
    }

    // ---------- Reportes / consultas ----------
    public Mono<ServerResponse> maxStockPorSucursal(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(useCase.maxStockPorSucursal(fId), Map.class);
    }

    public Mono<ServerResponse> getAllProductos(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(useCase.getAllProductos(), Producto.class);
    }

    public Mono<ServerResponse> getProductoGlobal(ServerRequest req) {
        String pId = req.pathVariable("productoId");
        return useCase.getProductoGlobal(pId)
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p));
    }

    public Mono<ServerResponse> searchProductosGlobal(ServerRequest req) {
        String q = req.queryParam("nombreLike").orElse("");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(useCase.searchProductosGlobal(q), Producto.class);
    }

    public Mono<ServerResponse> getAllProductosView(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        useCase.getAllProductosViewRaw()
                                .map(o -> DtoMappers.toProductoViewDTO((Map<String,Object>) o)),
                        ProductoViewDTO.class
                );
    }

    public Mono<ServerResponse> getProductoGlobalView(ServerRequest req) {
        String pId = req.pathVariable("productoId");
        return useCase.getProductoGlobalViewRaw(pId)
                .map(DtoMappers::toProductoViewDTO)
                .flatMap(dto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(dto));
    }

    public Mono<ServerResponse> getProductosDeSucursal(ServerRequest req) {
        String fId = req.pathVariable("franquiciaId");
        String sId = req.pathVariable("sucursalId");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(useCase.getProductosDeSucursal(fId, sId), Producto.class);
    }
}
