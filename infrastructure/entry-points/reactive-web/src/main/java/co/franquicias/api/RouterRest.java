package co.franquicias.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    private final Handler handler;

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return route()
                // Productos
                .GET   ("/api/productos/search",                handler::searchProductosGlobal)
                .GET   ("/api/productos/view",                  handler::getAllProductosView)
                .GET   ("/api/productos/view/{productoId}",     handler::getProductoGlobalView)
                .GET   ("/api/productos",                       handler::getAllProductos)
                .GET   ("/api/productos/{productoId}",          handler::getProductoGlobal)

                .PATCH ("/api/productos/{productoId}",          handler::actualizarProducto)

                // Franquicias
                .POST  ("/api/franquicias",                     handler::crearFranquicia)
                .GET   ("/api/franquicias",                     handler::obtenerFranquicias)

                .GET   ("/api/franquicias/by-name",             handler::obtenerFranquiciaPorNombre)
                .GET   ("/api/franquicias/{franquiciaId}",      handler::obtenerFranquicia)
                .DELETE("/api/franquicias/{franquiciaId}",      handler::eliminarFranquicia)
                .PATCH ("/api/franquicias/{franquiciaId}",      handler::actualizarFranquicia)

                // Sucursales
                .POST  ("/api/franquicias/{franquiciaId}/sucursales", handler::agregarSucursal)
                .GET("/api/franquicias/{franquiciaId}/sucursales",    handler::listarSucursalesDeFranquicia)

                // NUEVOS:
                .GET   ("/api/sucursales/{sucursalId}",         handler::obtenerSucursal)
                .DELETE("/api/sucursales/{sucursalId}",         handler::eliminarSucursal)
                .PATCH ("/api/sucursales/{sucursalId}",         handler::actualizarSucursal)

                // Productos por sucursal / franquicia
                .POST  ("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos", handler::agregarProducto)
                .DELETE("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}", handler::eliminarProducto)
                .PATCH ("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/stock", handler::actualizarStock)
                .GET   ("/api/franquicias/{franquiciaId}/max-stock-por-sucursal", handler::maxStockPorSucursal)
                .GET   ("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos", handler::getProductosDeSucursal)
                .build();
    }
}
