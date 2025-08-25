package co.franquicias.usecase.franquicia;

import co.franquicias.model.OperacionesFranquiciaPort;
import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FranquiciaUseCaseTest {

    @Mock OperacionesFranquiciaPort port;

    FranquiciaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FranquiciaUseCase(port);
    }

    private Franquicia franq(String id, String nombre) {
        Franquicia f = new Franquicia();
        f.setId(id);
        f.setNombre(nombre);
        return f;
    }
    private Sucursal suc(String id, String fid, String nombre) {
        Sucursal s = new Sucursal();
        s.setId(id);
        s.setFranquiciaId(fid);
        s.setNombre(nombre);
        return s;
    }
    private Producto prod(String id, String sid, String nombre, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setSucursalId(sid);
        p.setNombre(nombre);
        p.setStock(stock);
        return p;
    }

    // ================= Franquicia =================

    @Test
    @DisplayName("crearFranquicia: trimea y delega")
    void crearFranquicia_ok() {
        when(port.crearFranquicia("F1")).thenReturn(Mono.just(franq("f1", "F1")));

        StepVerifier.create(useCase.crearFranquicia("  F1  "))
                .expectNextMatches(f -> f.getId().equals("f1") && f.getNombre().equals("F1"))
                .verifyComplete();

        verify(port).crearFranquicia("F1");
    }

    @Test
    @DisplayName("crearFranquicia: nombre vacío → error y no delega")
    void crearFranquicia_nombreVacio() {
        StepVerifier.create(useCase.crearFranquicia("   "))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(port, never()).crearFranquicia(anyString());
    }

    @Test
    @DisplayName("obtenerPorId: ok y null id → NPE")
    void obtenerPorId_ok_y_null() {
        when(port.obtenerFranquicia("f1")).thenReturn(Mono.just(franq("f1", "F1")));

        StepVerifier.create(useCase.obtenerPorId("f1"))
                .expectNextMatches(f -> f.getId().equals("f1"))
                .verifyComplete();

        assertThrows(NullPointerException.class, () -> useCase.obtenerPorId(null));
    }

    @Test
    @DisplayName("obtenerFranquiciaPorNombre: trimea, ok y no encontrada")
    void obtenerFranquiciaPorNombre() {
        when(port.obtenerFranquiciaPorNombre("F1")).thenReturn(Mono.just(franq("f1", "F1")));

        StepVerifier.create(useCase.obtenerFranquiciaPorNombre("  F1 "))
                .expectNextMatches(f -> f.getId().equals("f1"))
                .verifyComplete();

        when(port.obtenerFranquiciaPorNombre("X")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.obtenerFranquiciaPorNombre("X"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Franquicia no encontrada"))
                .verify();
    }

    @Test
    @DisplayName("obtenerFranquicias: delega con flag")
    void obtenerFranquicias() {
        when(port.obtenerFranquicias(true)).thenReturn(Flux.just(franq("f1","F1")));
        StepVerifier.create(useCase.obtenerFranquicias(true))
                .expectNextCount(1).verifyComplete();
        verify(port).obtenerFranquicias(true);
    }

    @Test
    @DisplayName("eliminarFranquiciaPorId: ok y null id → NPE")
    void eliminarFranquiciaPorId() {
        when(port.eliminarFranquiciaPorId("f1")).thenReturn(Mono.just("ok"));
        StepVerifier.create(useCase.eliminarFranquiciaPorId("f1"))
                .expectNext("ok").verifyComplete();
        assertThrows(NullPointerException.class, () -> useCase.eliminarFranquiciaPorId(null));
    }

    @Test
    @DisplayName("actualizarFranquicia: trimea nombre; vacío → error")
    void actualizarFranquicia() {
        Franquicia cambios = franq(null, "  Nuevo  ");
        when(port.actualizarFranquicia(eq("f1"), any(Franquicia.class)))
                .thenAnswer(inv -> Mono.just(franq("f1", ((Franquicia) inv.getArgument(1)).getNombre())));

        StepVerifier.create(useCase.actualizarFranquicia("f1", cambios))
                .expectNextMatches(f -> f.getNombre().equals("Nuevo"))
                .verifyComplete();

        Franquicia inval = franq(null, "   ");
        StepVerifier.create(useCase.actualizarFranquicia("f1", inval))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("no puede estar vacío"))
                .verify();
        verify(port, times(1)).actualizarFranquicia(anyString(), any()); // solo el 1er caso
    }

    // ================= Sucursal =================

    @Test
    @DisplayName("agregarSucursal: nombre obligatorio, trimea y delega")
    void agregarSucursal() {
        when(port.agregarSucursal("f1", "S1")).thenReturn(Mono.just(suc("s1","f1","S1")));

        StepVerifier.create(useCase.agregarSucursal("f1", "  S1  "))
                .expectNextMatches(s -> s.getId().equals("s1") && s.getNombre().equals("S1"))
                .verifyComplete();

        verify(port).agregarSucursal("f1", "S1");

        StepVerifier.create(useCase.agregarSucursal("f1", "   "))
                .expectError(IllegalArgumentException.class)
                .verify();
        verify(port, times(1)).agregarSucursal(anyString(), anyString());
    }

    @Test
    @DisplayName("obtenerSucursalPorId: ok y no encontrada")
    void obtenerSucursalPorId() {
        when(port.obtenerSucursalPorId("s1")).thenReturn(Mono.just(suc("s1","f1","S1")));
        StepVerifier.create(useCase.obtenerSucursalPorId("s1"))
                .expectNextMatches(s -> s.getId().equals("s1"))
                .verifyComplete();

        when(port.obtenerSucursalPorId("sx")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.obtenerSucursalPorId("sx"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Sucursal no encontrada"))
                .verify();

        assertThrows(NullPointerException.class, () -> useCase.obtenerSucursalPorId(null));
    }

    @Test
    @DisplayName("obtenerSucursalPorFranquiciaId: delega")
    void obtenerSucursalPorFranquiciaId() {
        when(port.obtenerSucursalPorFranquiciaId("f1")).thenReturn(Flux.just(suc("s1","f1","S1")));
        StepVerifier.create(useCase.obtenerSucursalPorFranquiciaId("f1"))
                .expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("eliminarSucursalPorId: ok y null id → NPE")
    void eliminarSucursalPorId() {
        when(port.eliminarSucursalPorId("s1")).thenReturn(Mono.just("ok"));
        StepVerifier.create(useCase.eliminarSucursalPorId("s1"))
                .expectNext("ok").verifyComplete();
        assertThrows(NullPointerException.class, () -> useCase.eliminarSucursalPorId(null));
    }

    @Test
    @DisplayName("actualizarSucursal: trimea nombre; vacío → error")
    void actualizarSucursal() {
        when(port.actualizarSucursal(eq("s1"), any(Sucursal.class)))
                .thenAnswer(inv -> Mono.just(suc("s1","f1", ((Sucursal)inv.getArgument(1)).getNombre())));

        Sucursal cambios = suc(null,null,"  Nueva  ");
        StepVerifier.create(useCase.actualizarSucursal("s1", cambios))
                .expectNextMatches(s -> s.getNombre().equals("Nueva"))
                .verifyComplete();

        Sucursal inval = suc(null,null,"   ");
        StepVerifier.create(useCase.actualizarSucursal("s1", inval))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("no puede estar vacío"))
                .verify();
    }

    // ================= Producto =================

    @Test
    @DisplayName("agregarProducto: nombre obligatorio + stock >=0, trimea y delega")
    void agregarProducto() {
        when(port.agregarProducto("f1","s1","P",5)).thenReturn(Mono.just(prod("p1","s1","P",5)));

        StepVerifier.create(useCase.agregarProducto("f1","s1","  P  ",5))
                .expectNextMatches(p -> p.getId().equals("p1") && p.getNombre().equals("P") && p.getStock()==5)
                .verifyComplete();

        StepVerifier.create(useCase.agregarProducto("f1","s1","   ",1))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(useCase.agregarProducto("f1","s1","X",-1))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Stock negativo"))
                .verify();
    }

    @Test
    @DisplayName("eliminarProducto: delega y completa")
    void eliminarProducto() {
        when(port.eliminarProducto("f1","s1","p1")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.eliminarProducto("f1","s1","p1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("actualizarStock: valida stock >= 0")
    void actualizarStock() {
        when(port.actualizarStock("f1","s1","p1",9))
                .thenReturn(Mono.just(prod("p1","s1","A",9)));

        StepVerifier.create(useCase.actualizarStock("f1","s1","p1",9))
                .expectNextMatches(p -> p.getStock()==9)
                .verifyComplete();

        StepVerifier.create(useCase.actualizarStock("f1","s1","p1",-1))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("actualizarProducto: trimea nombre; nombre vacío o stock negativo → error")
    void actualizarProducto() {
        when(port.actualizarProducto(eq("p1"), any(Producto.class)))
                .thenAnswer(inv -> Mono.just((Producto) inv.getArgument(1)));

        Producto cambios = prod(null,"s1","  Nuevo  ",2);
        StepVerifier.create(useCase.actualizarProducto("p1", cambios))
                .expectNextMatches(p -> p.getNombre().equals("Nuevo") && p.getStock()==2)
                .verifyComplete();

        Producto invalNombre = prod(null,"s1","   ",2);
        StepVerifier.create(useCase.actualizarProducto("p1", invalNombre))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("no puede estar vacío"))
                .verify();

        Producto invalStock = prod(null,"s1","X",-1);
        StepVerifier.create(useCase.actualizarProducto("p1", invalStock))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Stock negativo"))
                .verify();
    }

    // ================= Consultas / Vistas =================

    @Test
    @DisplayName("maxStockPorSucursal: delega")
    void maxStockPorSucursal() {
        when(port.maxStockPorSucursal("f1")).thenReturn(Flux.just(
                Map.of("sucursalId","s1","stock",10),
                Map.of("sucursalId","s2","stock",0)
        ));
        StepVerifier.create(useCase.maxStockPorSucursal("f1"))
                .expectNextCount(2).verifyComplete();
    }

    @Test
    @DisplayName("getAllProductos: delega")
    void getAllProductos() {
        when(port.todosLosProductos()).thenReturn(Flux.just(prod("p1","s1","A",1)));
        StepVerifier.create(useCase.getAllProductos())
                .expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("getProductoGlobal: ok y no encontrado")
    void getProductoGlobal() {
        when(port.productoGlobal("p1")).thenReturn(Mono.just(Map.of("productoId","p1")));
        StepVerifier.create(useCase.getProductoGlobal("p1"))
                .expectNextMatches(m -> m.get("productoId").equals("p1"))
                .verifyComplete();

        when(port.productoGlobal("px")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.getProductoGlobal("px"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Producto no encontrado"))
                .verify();
    }

    @Test
    @DisplayName("searchProductosGlobal: usa resultado de búsqueda o fallback a todos")
    void searchProductosGlobal() {
        when(port.buscarProductos("abc")).thenReturn(Flux.just(prod("p1","s1","abc",1)));
        when(port.buscarProductos("")).thenReturn(Flux.empty());
        when(port.todosLosProductos()).thenReturn(Flux.just(prod("p2","s1","X",2)));

        StepVerifier.create(useCase.searchProductosGlobal("  abc  "))
                .expectNextMatches(p -> p.getNombre().equals("abc"))
                .verifyComplete();

        StepVerifier.create(useCase.searchProductosGlobal(null))
                .expectNextMatches(p -> p.getId().equals("p2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllProductosViewRaw: delega")
    void getAllProductosViewRaw() {
        when(port.todosProductosViewRaw()).thenReturn(Flux.just(Map.of("productoId","p1")));
        StepVerifier.create(useCase.getAllProductosViewRaw())
                .expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("getProductoGlobalViewRaw: ok y no encontrado")
    void getProductoGlobalViewRaw() {
        when(port.productoGlobal("p1")).thenReturn(Mono.just(Map.of("productoId","p1")));
        StepVerifier.create(useCase.getProductoGlobalViewRaw("p1"))
                .expectNextMatches(m -> m.get("productoId").equals("p1"))
                .verifyComplete();

        when(port.productoGlobal("px")).thenReturn(Mono.empty());
        StepVerifier.create(useCase.getProductoGlobalViewRaw("px"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Producto no encontrado"))
                .verify();
    }

    @Test
    @DisplayName("getProductosDeSucursal: delega")
    void getProductosDeSucursal() {
        when(port.productosDeSucursal("f1","s1")).thenReturn(Flux.just(prod("p1","s1","A",1)));
        StepVerifier.create(useCase.getProductosDeSucursal("f1","s1"))
                .expectNextCount(1).verifyComplete();
    }
}
