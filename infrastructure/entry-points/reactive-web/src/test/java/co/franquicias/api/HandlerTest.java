package co.franquicias.api;

import co.franquicias.api.dto.franquicia.CreateFranquiciaRequest;
import co.franquicias.api.dto.franquicia.UpdateFranquiciaRequest;
import co.franquicias.api.dto.producto.CreateProductoRequest;
import co.franquicias.api.dto.producto.UpdateProductoRequest;
import co.franquicias.api.dto.producto.UpdateStockRequest;
import co.franquicias.api.dto.sucursal.CreateSucursalRequest;
import co.franquicias.api.dto.sucursal.UpdateSucursalRequest;
import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import co.franquicias.usecase.franquicia.FranquiciaUseCase;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandlerTest {

    @Mock
    FranquiciaUseCase useCase;

    @InjectMocks
    Handler handler;

    WebTestClient client;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        RouterFunction<ServerResponse> router = buildRouter(handler);
        client = WebTestClient.bindToRouterFunction(router).build();
    }

    // ====================== Helpers dominio ======================
    private Franquicia f(String id, String nombre) {
        Franquicia ff = new Franquicia();
        ff.setId(id); ff.setNombre(nombre);
        return ff;
    }

    private Sucursal s(String id, String fid, String nombre) {
        Sucursal ss = new Sucursal();
        ss.setId(id); ss.setFranquiciaId(fid); ss.setNombre(nombre);
        return ss;
    }

    private Producto p(String id, String sid, String nombre, int stock) {
        Producto pp = new Producto();
        pp.setId(id); pp.setSucursalId(sid); pp.setNombre(nombre); pp.setStock(stock);
        return pp;
    }

    /** Router de prueba (orden explícito de rutas) */
    private static RouterFunction<ServerResponse> buildRouter(Handler h) {
        return RouterFunctions.route()
                // Franquicia
                .POST("/api/franquicias", h::crearFranquicia)
                .GET("/api/franquicias", h::obtenerFranquicias)
                .GET("/api/franquicias/by-nombre", h::obtenerFranquiciaPorNombre)
                .GET("/api/franquicias/{franquiciaId}", h::obtenerFranquicia)
                .DELETE("/api/franquicias/{franquiciaId}", h::eliminarFranquicia)
                .PUT("/api/franquicias/{franquiciaId}", h::actualizarFranquicia)
                // Sucursal
                .POST("/api/franquicias/{franquiciaId}/sucursales", h::agregarSucursal)
                .GET("/api/franquicias/{franquiciaId}/sucursales", h::listarSucursalesDeFranquicia)
                .GET("/api/sucursales/{sucursalId}", h::obtenerSucursal)
                .DELETE("/api/sucursales/{sucursalId}", h::eliminarSucursal)
                .PUT("/api/sucursales/{sucursalId}", h::actualizarSucursal)
                // Producto
                .POST("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos", h::agregarProducto)
                .DELETE("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}", h::eliminarProducto)
                .PUT("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/stock", h::actualizarStock)
                .PUT("/api/productos/{productoId}", h::actualizarProducto)
                // Reportes / consultas
                .GET("/api/franquicias/{franquiciaId}/reportes/max-stock", h::maxStockPorSucursal)
                .GET("/api/productos", h::getAllProductos)
                .GET("/api/productos/{productoId}/global", h::getProductoGlobal)
                .GET("/api/productos/search", h::searchProductosGlobal)
                .GET("/api/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos", h::getProductosDeSucursal)
                .build();
    }

    // ====================== Tests ======================

    @Test
    @DisplayName("POST /api/franquicias => 201 Created con Location y cuerpo")
    void crearFranquicia() {
        when(useCase.crearFranquicia("F1")).thenReturn(Mono.just(f("f1","F1")));

        client.post().uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateFranquiciaRequest("F1"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/franquicias/f1")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.nombre").isEqualTo("F1");

        verify(useCase).crearFranquicia("F1");
    }

    @Test
    @DisplayName("GET /api/franquicias?includeProductos=1 => delega con true")
    void obtenerFranquicias_includeProductos() {
        when(useCase.obtenerFranquicias(true)).thenReturn(Flux.just(f("f1","F1")));

        client.get().uri("/api/franquicias?includeProductos=1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("f1");

        verify(useCase).obtenerFranquicias(true);
    }

    @Test
    @DisplayName("GET /api/franquicias/{id} => 200 con franquicia")
    void obtenerFranquicia() {
        when(useCase.obtenerPorId("f1")).thenReturn(Mono.just(f("f1","F1")));

        client.get().uri("/api/franquicias/f1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1");
    }

    @Test
    @DisplayName("GET /api/franquicias/by-nombre?nombre=F1 => 200")
    void obtenerFranquiciaPorNombre() {
        when(useCase.obtenerFranquiciaPorNombre("F1")).thenReturn(Mono.just(f("f1","F1")));

        client.get().uri("/api/franquicias/by-nombre?nombre=F1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("F1");

        verify(useCase, times(1)).obtenerFranquiciaPorNombre("F1");
        verify(useCase, never()).obtenerPorId(anyString());
    }

    @Test
    @DisplayName("DELETE /api/franquicias/{id} => 200 con mensaje")
    void eliminarFranquicia() {
        when(useCase.eliminarFranquiciaPorId("f1")).thenReturn(Mono.just("ok"));

        client.delete().uri("/api/franquicias/f1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("ok");
    }

    @Test
    @DisplayName("PUT /api/franquicias/{id} => 200 con franquicia actualizada")
    void actualizarFranquicia() {
        when(useCase.actualizarFranquicia(eq("f1"), any(Franquicia.class)))
                .thenReturn(Mono.just(f("f1","Nueva")));

        client.put().uri("/api/franquicias/f1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateFranquiciaRequest("Nueva"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Nueva");
    }

    @Test
    @DisplayName("POST /api/franquicias/{fId}/sucursales => 200 con sucursal")
    void agregarSucursal() {
        when(useCase.agregarSucursal("f1","S1")).thenReturn(Mono.just(s("s1","f1","S1")));

        client.post().uri("/api/franquicias/f1/sucursales")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateSucursalRequest("S1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("s1")
                .jsonPath("$.franquiciaId").isEqualTo("f1");
    }

    @Test
    @DisplayName("GET /api/franquicias/{fId}/sucursales => 200 lista")
    void listarSucursales() {
        when(useCase.obtenerSucursalPorFranquiciaId("f1"))
                .thenReturn(Flux.just(s("s1","f1","S1")));

        client.get().uri("/api/franquicias/f1/sucursales")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("S1");
    }

    @Test
    @DisplayName("GET /api/sucursales/{id} => 200 sucursal")
    void obtenerSucursal() {
        when(useCase.obtenerSucursalPorId("s1")).thenReturn(Mono.just(s("s1","f1","S1")));

        client.get().uri("/api/sucursales/s1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("s1");
    }

    @Test
    @DisplayName("DELETE /api/sucursales/{id} => 200 con mensaje")
    void eliminarSucursal() {
        when(useCase.eliminarSucursalPorId("s1")).thenReturn(Mono.just("ok"));

        client.delete().uri("/api/sucursales/s1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("ok");
    }

    @Test
    @DisplayName("PUT /api/sucursales/{id} => 200 con sucursal actualizada")
    void actualizarSucursal() {
        when(useCase.actualizarSucursal(eq("s1"), any(Sucursal.class)))
                .thenReturn(Mono.just(s("s1","f1","Nueva")));

        client.put().uri("/api/sucursales/s1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateSucursalRequest("Nueva","f1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Nueva");
    }

    @Test
    @DisplayName("POST /api/.../productos => 200 producto")
    void agregarProducto() {
        when(useCase.agregarProducto("f1","s1","P1",5))
                .thenReturn(Mono.just(p("p1","s1","P1",5)));

        client.post().uri("/api/franquicias/f1/sucursales/s1/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductoRequest("P1",5))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("p1")
                .jsonPath("$.stock").isEqualTo(5);
    }

    @Test
    @DisplayName("DELETE /api/.../productos/{pId} => 204 No Content")
    void eliminarProducto() {
        when(useCase.eliminarProducto("f1","s1","p1")).thenReturn(Mono.empty());

        client.delete().uri("/api/franquicias/f1/sucursales/s1/productos/p1")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    @DisplayName("PUT /api/.../productos/{pId}/stock => 200 y devuelve producto")
    void actualizarStock() {
        when(useCase.actualizarStock("f1","s1","p1",9))
                .thenReturn(Mono.just(p("p1","s1","P",9)));

        client.put().uri("/api/franquicias/f1/sucursales/s1/productos/p1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateStockRequest(9))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.stock").isEqualTo(9);
    }

    @Test
    @DisplayName("PUT /api/productos/{pId} => mapea stock=0 cuando viene null/ausente")
    void actualizarProducto_stockDefaultCero() {
        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        when(useCase.actualizarProducto(eq("p1"), any(Producto.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

        client.put().uri("/api/productos/p1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateProductoRequest("Nuevo", null, "s1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Nuevo")
                .jsonPath("$.stock").isEqualTo(0)
                .jsonPath("$.sucursalId").isEqualTo("s1");

        verify(useCase).actualizarProducto(eq("p1"), captor.capture());
        Producto enviado = captor.getValue();
        Assertions.assertEquals(0, enviado.getStock());
        Assertions.assertEquals("Nuevo", enviado.getNombre());
        Assertions.assertEquals("s1", enviado.getSucursalId());
    }

    @Test
    @DisplayName("GET /api/franquicias/{fId}/reportes/max-stock => 200 con lista de mapas")
    void maxStockPorSucursal() {
        Map<String, Object> m1 = Map.of(
                "sucursalId", "s1",
                "productoId", "p2",
                "stock", 10
        );

        Map<String, Object> m2 = new java.util.HashMap<>();
        m2.put("sucursalId", "s2");
        m2.put("productoId", null);
        m2.put("stock", 0);

        when(useCase.maxStockPorSucursal("f1"))
                .thenReturn(Flux.just(m1, m2));

        client.get().uri("/api/franquicias/f1/reportes/max-stock")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].sucursalId").isEqualTo("s1")
                .jsonPath("$[0].productoId").isEqualTo("p2")
                .jsonPath("$[0].stock").isEqualTo(10)
                .jsonPath("$[1].productoId").value(org.hamcrest.Matchers.nullValue())
                .jsonPath("$[1].stock").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/productos => 200 lista")
    void getAllProductos() {
        when(useCase.getAllProductos()).thenReturn(Flux.just(p("p1","s1","A",1)));

        client.get().uri("/api/productos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("p1");
    }

    @Test
    @DisplayName("GET /api/productos/{pId}/global => 200 con mapa")
    void getProductoGlobal() {
        when(useCase.getProductoGlobal("p1"))
                .thenReturn(Mono.just(Map.of("productoId","p1","stock",3)));

        client.get().uri("/api/productos/p1/global")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.productoId").isEqualTo("p1")
                .jsonPath("$.stock").isEqualTo(3);
    }

    @Test
    @DisplayName("GET /api/productos/search?nombreLike=ab => 200 con lista")
    void searchProductosGlobal() {
        when(useCase.searchProductosGlobal("ab"))
                .thenReturn(Flux.just(p("p1","s1","ab",1)));

        client.get().uri("/api/productos/search?nombreLike=ab")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("ab");
    }

    @Test
    @DisplayName("GET /api/franquicias/{fId}/sucursales/{sId}/productos => 200 con lista")
    void getProductosDeSucursal() {
        when(useCase.getProductosDeSucursal("f1","s1"))
                .thenReturn(Flux.just(p("p1","s1","A",1)));

        client.get().uri("/api/franquicias/f1/sucursales/s1/productos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].sucursalId").isEqualTo("s1");
    }

    @Test
    @DisplayName("by-nombre NO debe ser capturado por /{franquiciaId}")
    void routingPriority() {
        when(useCase.obtenerFranquiciaPorNombre("F1")).thenReturn(Mono.just(f("f1","F1")));

        client.get().uri("/api/franquicias/by-nombre?nombre=F1")
                .exchange()
                .expectStatus().isOk();

        verify(useCase, times(1)).obtenerFranquiciaPorNombre("F1");
        verify(useCase, never()).obtenerPorId(anyString());
    }
}
