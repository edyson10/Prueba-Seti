package co.franquicias.api;

import co.franquicias.api.dto.franquicia.CreateFranquiciaRequest;
import co.franquicias.api.dto.franquicia.UpdateFranquiciaRequest;
import co.franquicias.api.dto.producto.CreateProductoRequest;
import co.franquicias.api.dto.producto.UpdateProductoRequest;
import co.franquicias.api.dto.producto.UpdateStockRequest;
import co.franquicias.api.dto.sucursal.CreateSucursalRequest;
import co.franquicias.api.dto.sucursal.UpdateSucursalRequest;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RouterRestTest {

    @Mock
    Handler handler;

    WebTestClient client;

    AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        RouterRest routerRest = new RouterRest(handler);
        RouterFunction<ServerResponse> routes = routerRest.routerFunction();
        client = WebTestClient.bindToRouterFunction(routes).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) closeable.close();
    }

    // --------- Helpers para respuestas dummy ----------
    private static Mono<ServerResponse> okJson(Object body) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }
    private static Mono<ServerResponse> okEmpty() {
        return ServerResponse.ok().build();
    }
    private static Mono<ServerResponse> noContent() {
        return ServerResponse.noContent().build();
    }
    private static Mono<ServerResponse> createdJson(String location, Object body) {
        return ServerResponse.created(URI.create(location))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    // ==================== PRODUCTOS ====================

    @Test
    @DisplayName("GET /api/productos/search enruta a handler::searchProductosGlobal con query param")
    void productos_search() {
        when(handler.searchProductosGlobal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("ab", req.queryParam("nombreLike").orElse(""));
            return okJson(Map.of("status","ok"));
        });

        client.get().uri("/api/productos/search?nombreLike=ab")
                .exchange()
                .expectStatus().isOk();

        verify(handler, times(1)).searchProductosGlobal(any());
    }

    @Test
    @DisplayName("GET /api/productos/view enruta a handler::getAllProductosView")
    void productos_viewAll() {
        when(handler.getAllProductosView(any())).thenReturn(okEmpty());

        client.get().uri("/api/productos/view")
                .exchange()
                .expectStatus().isOk();

        verify(handler).getAllProductosView(any());
    }

    @Test
    @DisplayName("GET /api/productos/view/{productoId} enruta a handler::getProductoGlobalView con path variable")
    void productos_viewById() {
        when(handler.getProductoGlobalView(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("p1", req.pathVariable("productoId"));
            return okEmpty();
        });

        client.get().uri("/api/productos/view/p1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).getProductoGlobalView(any());
    }

    @Test
    @DisplayName("GET /api/productos enruta a handler::getAllProductos")
    void productos_getAll() {
        when(handler.getAllProductos(any())).thenReturn(okEmpty());

        client.get().uri("/api/productos")
                .exchange()
                .expectStatus().isOk();

        verify(handler).getAllProductos(any());
    }

    @Test
    @DisplayName("GET /api/productos/{productoId} enruta a handler::getProductoGlobal con path variable")
    void productos_getById() {
        when(handler.getProductoGlobal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("p1", req.pathVariable("productoId"));
            return okJson(Map.of("productoId","p1"));
        });

        client.get().uri("/api/productos/p1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).getProductoGlobal(any());
    }

    @Test
    @DisplayName("PATCH /api/productos/{productoId} enruta a handler::actualizarProducto")
    void productos_patch() {
        when(handler.actualizarProducto(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("p1", req.pathVariable("productoId"));
            return okJson(Map.of("status","patched"));
        });

        client.patch().uri("/api/productos/p1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateProductoRequest("Nombre", 9, "s1"))
                .exchange()
                .expectStatus().isOk();

        verify(handler).actualizarProducto(any());
    }

    // ==================== FRANQUICIAS ====================

    @Test
    @DisplayName("POST /api/franquicias enruta a handler::crearFranquicia (201 Created)")
    void franquicias_post() {
        when(handler.crearFranquicia(any()))
                .thenReturn(createdJson("/api/franquicias/f1", Map.of("id","f1")));

        client.post().uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateFranquiciaRequest("F1"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/franquicias/f1");

        verify(handler).crearFranquicia(any());
    }

    @Test
    @DisplayName("GET /api/franquicias enruta a handler::obtenerFranquicias")
    void franquicias_getAll() {
        when(handler.obtenerFranquicias(any())).thenReturn(okEmpty());

        client.get().uri("/api/franquicias?includeProductos=1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).obtenerFranquicias(any());
    }

    @Test
    @DisplayName("GET /api/franquicias/by-name enruta a handler::obtenerFranquiciaPorNombre")
    void franquicias_byName() {
        when(handler.obtenerFranquiciaPorNombre(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("F1", req.queryParam("nombre").orElse(""));
            return okJson(Map.of("nombre","F1"));
        });

        client.get().uri("/api/franquicias/by-name?nombre=F1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).obtenerFranquiciaPorNombre(any());
    }

    @Test
    @DisplayName("GET /api/franquicias/{franquiciaId} enruta a handler::obtenerFranquicia")
    void franquicias_getById() {
        when(handler.obtenerFranquicia(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            return okJson(Map.of("id","f1"));
        });

        client.get().uri("/api/franquicias/f1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).obtenerFranquicia(any());
    }

    @Test
    @DisplayName("DELETE /api/franquicias/{franquiciaId} enruta a handler::eliminarFranquicia")
    void franquicias_delete() {
        when(handler.eliminarFranquicia(any())).thenReturn(okJson(Map.of("message","ok")));

        client.delete().uri("/api/franquicias/f1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).eliminarFranquicia(any());
    }

    @Test
    @DisplayName("PATCH /api/franquicias/{franquiciaId} enruta a handler::actualizarFranquicia")
    void franquicias_patch() {
        when(handler.actualizarFranquicia(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            return okJson(Map.of("id","f1","nombre","NUEVA"));
        });

        client.patch().uri("/api/franquicias/f1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateFranquiciaRequest("NUEVA"))
                .exchange()
                .expectStatus().isOk();

        verify(handler).actualizarFranquicia(any());
    }

    // ==================== SUCURSALES ====================

    @Test
    @DisplayName("POST /api/franquicias/{fId}/sucursales enruta a handler::agregarSucursal")
    void sucursales_post() {
        when(handler.agregarSucursal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            return okJson(Map.of("id","s1"));
        });

        client.post().uri("/api/franquicias/f1/sucursales")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateSucursalRequest("S1"))
                .exchange()
                .expectStatus().isOk();

        verify(handler).agregarSucursal(any());
    }

    @Test
    @DisplayName("GET /api/franquicias/{fId}/sucursales enruta a handler::listarSucursalesDeFranquicia")
    void sucursales_listarPorFranquicia() {
        when(handler.listarSucursalesDeFranquicia(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            return okEmpty();
        });

        client.get().uri("/api/franquicias/f1/sucursales")
                .exchange()
                .expectStatus().isOk();

        verify(handler).listarSucursalesDeFranquicia(any());
    }

    @Test
    @DisplayName("GET /api/sucursales/{sucursalId} enruta a handler::obtenerSucursal")
    void sucursales_getById() {
        when(handler.obtenerSucursal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("s1", req.pathVariable("sucursalId"));
            return okJson(Map.of("id","s1"));
        });

        client.get().uri("/api/sucursales/s1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).obtenerSucursal(any());
    }

    @Test
    @DisplayName("DELETE /api/sucursales/{sucursalId} enruta a handler::eliminarSucursal")
    void sucursales_delete() {
        when(handler.eliminarSucursal(any())).thenReturn(okJson(Map.of("message","ok")));

        client.delete().uri("/api/sucursales/s1")
                .exchange()
                .expectStatus().isOk();

        verify(handler).eliminarSucursal(any());
    }

    @Test
    @DisplayName("PATCH /api/sucursales/{sucursalId} enruta a handler::actualizarSucursal")
    void sucursales_patch() {
        when(handler.actualizarSucursal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("s1", req.pathVariable("sucursalId"));
            return okJson(Map.of("id","s1","nombre","NUEVA"));
        });

        client.patch().uri("/api/sucursales/s1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateSucursalRequest("NUEVA","f1"))
                .exchange()
                .expectStatus().isOk();

        verify(handler).actualizarSucursal(any());
    }

    // ========== PRODUCTOS por sucursal / franquicia ==========

    @Test
    @DisplayName("POST /api/franquicias/{fId}/sucursales/{sId}/productos -> handler::agregarProducto")
    void productos_add() {
        when(handler.agregarProducto(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            Assertions.assertEquals("s1", req.pathVariable("sucursalId"));
            return okJson(Map.of("id","p1"));
        });

        client.post().uri("/api/franquicias/f1/sucursales/s1/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductoRequest("P1", 5))
                .exchange()
                .expectStatus().isOk();

        verify(handler).agregarProducto(any());
    }

    @Test
    @DisplayName("DELETE /api/franquicias/{fId}/sucursales/{sId}/productos/{pId} -> handler::eliminarProducto")
    void productos_delete() {
        when(handler.eliminarProducto(any())).thenReturn(noContent());

        client.delete().uri("/api/franquicias/f1/sucursales/s1/productos/p1")
                .exchange()
                .expectStatus().isNoContent();

        verify(handler).eliminarProducto(any());
    }

    @Test
    @DisplayName("PATCH /api/franquicias/{fId}/sucursales/{sId}/productos/{pId}/stock -> handler::actualizarStock")
    void productos_patchStock() {
        when(handler.actualizarStock(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            Assertions.assertEquals("s1", req.pathVariable("sucursalId"));
            Assertions.assertEquals("p1", req.pathVariable("productoId"));
            return okJson(Map.of("stock", 9));
        });

        client.patch().uri("/api/franquicias/f1/sucursales/s1/productos/p1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateStockRequest(9))
                .exchange()
                .expectStatus().isOk();

        verify(handler).actualizarStock(any());
    }

    @Test
    @DisplayName("GET /api/franquicias/{fId}/max-stock-por-sucursal -> handler::maxStockPorSucursal")
    void reporte_maxStockPorSucursal() {
        when(handler.maxStockPorSucursal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            return okJson(new java.util.HashMap<String,Object>() {{
                put("sucursalId", "s1");
                put("productoId", null); // permitir null
                put("stock", 0);
            }});
        });

        client.get().uri("/api/franquicias/f1/max-stock-por-sucursal")
                .exchange()
                .expectStatus().isOk();

        verify(handler).maxStockPorSucursal(any());
    }

    @Test
    @DisplayName("GET /api/franquicias/{fId}/sucursales/{sId}/productos -> handler::getProductosDeSucursal")
    void productos_porSucursal() {
        when(handler.getProductosDeSucursal(any())).thenAnswer(inv -> {
            ServerRequest req = inv.getArgument(0);
            Assertions.assertEquals("f1", req.pathVariable("franquiciaId"));
            Assertions.assertEquals("s1", req.pathVariable("sucursalId"));
            return okEmpty();
        });

        client.get().uri("/api/franquicias/f1/sucursales/s1/productos")
                .exchange()
                .expectStatus().isOk();

        verify(handler).getProductosDeSucursal(any());
    }
}
