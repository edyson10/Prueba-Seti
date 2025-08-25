package co.franquicias.mongodb.facade;

import co.franquicias.model.OperacionesFranquiciaPort;
import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.model.producto.Producto;
import co.franquicias.model.sucursal.Sucursal;
import co.franquicias.mongodb.adapter.FranquiciaAdapter;
import co.franquicias.mongodb.adapter.ProductoAdapter;
import co.franquicias.mongodb.adapter.SucursalAdapter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FranquiciasMongoFacadeAdapterTest {

    @Mock FranquiciaAdapter franquiciaAdapter;
    @Mock SucursalAdapter sucursalAdapter;
    @Mock ProductoAdapter productoAdapter;

    OperacionesFranquiciaPort facade;

    @BeforeEach
    void setUp() {
        facade = new FranquiciasMongoFacadeAdapter(franquiciaAdapter, sucursalAdapter, productoAdapter);
    }

    private Franquicia franq(String id, String nombre) {
        Franquicia f = new Franquicia();
        f.setId(id);
        f.setNombre(nombre);
        f.setSucursales(null);
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

    // ================= Franquicia: crear/obtener/actualizar/eliminar =================

    @Test
    @DisplayName("crearFranquicia: delega en adapter")
    void crearFranquicia() {
        when(franquiciaAdapter.crearFranquicia("F1")).thenReturn(Mono.just(franq("f1","F1")));

        StepVerifier.create(facade.crearFranquicia("F1"))
                .expectNextMatches(f -> f.getId().equals("f1") && f.getNombre().equals("F1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("obtenerFranquiciaPorNombre: con hidratación de sucursales y productos")
    void obtenerFranquiciaPorNombre_hidratada() {
        Franquicia base = franq("f1","F1");
        Sucursal s1 = suc("s1","f1","S-A");
        Sucursal s2 = suc("s2","f1","S-B");
        Producto p1 = prod("p1","s1","A",5);
        Producto p2 = prod("p2","s1","B",3);
        Producto p3 = prod("p3","s2","C",7);

        when(franquiciaAdapter.obtenerPorNombre("F1")).thenReturn(Mono.just(base));
        when(sucursalAdapter.listarPorFranquicia("f1")).thenReturn(Flux.just(s1, s2));
        when(productoAdapter.listarPorSucursal("s1")).thenReturn(Flux.just(p1, p2));
        when(productoAdapter.listarPorSucursal("s2")).thenReturn(Flux.just(p3));

        StepVerifier.create(facade.obtenerFranquiciaPorNombre("F1"))
                .assertNext(f -> {
                    Assertions.assertEquals("f1", f.getId());
                    Assertions.assertNotNull(f.getSucursales());
                    Assertions.assertEquals(2, f.getSucursales().size());
                    var s1h = f.getSucursales().get(0);
                    var s2h = f.getSucursales().get(1);
                    Assertions.assertEquals(2, s1h.getProductos().size());
                    Assertions.assertEquals(1, s2h.getProductos().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("obtenerFranquiciaPorNombre: error si no existe")
    void obtenerFranquiciaPorNombre_noExiste() {
        when(franquiciaAdapter.obtenerPorNombre("X")).thenReturn(Mono.empty());

        StepVerifier.create(facade.obtenerFranquiciaPorNombre("X"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Franquicia no existe"))
                .verify();
    }

    @Test
    @DisplayName("obtenerFranquicias(verProductos=false): hidrata solo sucursales")
    void obtenerFranquicias_sinProductos() {
        Franquicia f1 = franq("f1","F1");
        Franquicia f2 = franq("f2","F2");
        when(franquiciaAdapter.obtenerFranquicias()).thenReturn(Flux.just(f1, f2));
        when(sucursalAdapter.listarPorFranquicia("f1")).thenReturn(Flux.just(suc("s1","f1","A")));
        when(sucursalAdapter.listarPorFranquicia("f2")).thenReturn(Flux.empty());

        StepVerifier.create(facade.obtenerFranquicias(false))
                .expectNextMatches(f -> f.getId().equals("f1") && f.getSucursales().size() == 1 &&
                        (f.getSucursales().get(0).getProductos() == null || f.getSucursales().get(0).getProductos().isEmpty()))
                .expectNextMatches(f -> f.getId().equals("f2") && f.getSucursales().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("obtenerFranquicias(verProductos=true): hidrata sucursales + productos")
    void obtenerFranquicias_conProductos() {
        Franquicia f1 = franq("f1","F1");
        when(franquiciaAdapter.obtenerFranquicias()).thenReturn(Flux.just(f1));
        when(sucursalAdapter.listarPorFranquicia("f1")).thenReturn(Flux.just(suc("s1","f1","A")));
        when(productoAdapter.listarPorSucursal("s1")).thenReturn(Flux.just(prod("p1","s1","P",2)));

        StepVerifier.create(facade.obtenerFranquicias(true))
                .assertNext(f -> {
                    Assertions.assertEquals("f1", f.getId());
                    Assertions.assertEquals(1, f.getSucursales().size());
                    Assertions.assertEquals(1, f.getSucursales().get(0).getProductos().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("obtenerFranquicia(id): hidrata y error si no existe")
    void obtenerFranquicia_porId() {
        Franquicia base = franq("f1","F1");
        when(franquiciaAdapter.obtenerPorId("f1")).thenReturn(Mono.just(base));
        when(sucursalAdapter.listarPorFranquicia("f1")).thenReturn(Flux.empty());

        StepVerifier.create(facade.obtenerFranquicia("f1"))
                .expectNextMatches(f -> f.getId().equals("f1") && f.getSucursales().isEmpty())
                .verifyComplete();

        when(franquiciaAdapter.obtenerPorId("x")).thenReturn(Mono.empty());
        StepVerifier.create(facade.obtenerFranquicia("x"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Franquicia no existe"))
                .verify();
    }

    @Test
    @DisplayName("eliminarFranquiciaPorId / actualizarFranquicia: delegan en adapter")
    void eliminarYActualizarFranquicia() {
        when(franquiciaAdapter.eliminarPorId("f1")).thenReturn(Mono.just("ok"));
        when(franquiciaAdapter.actualizarFranquicia(eq("f1"), any())).thenReturn(Mono.just(franq("f1","N")));

        StepVerifier.create(facade.eliminarFranquiciaPorId("f1"))
                .expectNext("ok").verifyComplete();
        StepVerifier.create(facade.actualizarFranquicia("f1", franq(null,"N")))
                .expectNextMatches(f -> f.getNombre().equals("N"))
                .verifyComplete();
    }

    // ================= Sucursales =================

    @Test
    @DisplayName("obtenerSucursalPorId: hidrata productos y error si no existe")
    void obtenerSucursalPorId() {
        Sucursal s = suc("s1","f1","S1");
        when(sucursalAdapter.obtenerPorId("s1")).thenReturn(Mono.just(s));
        when(productoAdapter.listarPorSucursal("s1")).thenReturn(Flux.just(prod("p1","s1","P",1)));

        StepVerifier.create(facade.obtenerSucursalPorId("s1"))
                .expectNextMatches(sh -> sh.getProductos() != null && sh.getProductos().size() == 1)
                .verifyComplete();

        when(sucursalAdapter.obtenerPorId("sx")).thenReturn(Mono.empty());
        StepVerifier.create(facade.obtenerSucursalPorId("sx"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Sucursal no existe"))
                .verify();
    }

    @Test
    @DisplayName("obtenerSucursalPorFranquiciaId: hidrata; si repo vacío valida existencia de franquicia")
    void obtenerSucursalPorFranquiciaId() {
        when(sucursalAdapter.listarPorFranquicia("f1")).thenReturn(Flux.just(suc("s1","f1","A")));
        when(productoAdapter.listarPorSucursal("s1")).thenReturn(Flux.just(prod("p1","s1","P",1)));

        StepVerifier.create(facade.obtenerSucursalPorFranquiciaId("f1"))
                .expectNextMatches(s -> s.getId().equals("s1") && s.getProductos().size() == 1)
                .verifyComplete();

        when(sucursalAdapter.listarPorFranquicia("f2")).thenReturn(Flux.empty());
        when(franquiciaAdapter.obtenerPorId("f2")).thenReturn(Mono.just(franq("f2","F2")));
        StepVerifier.create(facade.obtenerSucursalPorFranquiciaId("f2"))
                .verifyComplete();

        when(sucursalAdapter.listarPorFranquicia("fx")).thenReturn(Flux.empty());
        when(franquiciaAdapter.obtenerPorId("fx")).thenReturn(Mono.empty());
        StepVerifier.create(facade.obtenerSucursalPorFranquiciaId("fx"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Franquicia no encontrada"))
                .verify();
    }

    @Test
    @DisplayName("eliminarSucursalPorId / actualizarSucursal: delegan")
    void eliminarYActualizarSucursal() {
        when(sucursalAdapter.eliminarPorId("s1")).thenReturn(Mono.just("ok"));
        when(sucursalAdapter.actualizarSucursal(eq("s1"), any())).thenReturn(Mono.just(suc("s1","f1","N")));

        StepVerifier.create(facade.eliminarSucursalPorId("s1"))
                .expectNext("ok").verifyComplete();
        StepVerifier.create(facade.actualizarSucursal("s1", suc(null,null,"N")))
                .expectNextMatches(s -> s.getNombre().equals("N"))
                .verifyComplete();
    }

    // ================= Productos =================

    @Test
    @DisplayName("agregarProducto: sucursal debe existir y pertenecer a la franquicia")
    void agregarProducto() {
        when(sucursalAdapter.obtenerPorId("s1")).thenReturn(Mono.just(suc("s1","f1","S1")));
        when(productoAdapter.crear("s1","P",5)).thenReturn(Mono.just(prod("p1","s1","P",5)));

        StepVerifier.create(facade.agregarProducto("f1","s1","P",5))
                .expectNextMatches(p -> p.getId().equals("p1") && p.getStock()==5)
                .verifyComplete();

        when(sucursalAdapter.obtenerPorId("sx")).thenReturn(Mono.empty());
        StepVerifier.create(facade.agregarProducto("f1","sx","P",1))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Sucursal no existe"))
                .verify();

        // sucursal de otra franquicia
        when(sucursalAdapter.obtenerPorId("s2")).thenReturn(Mono.just(suc("s2","fX","S2")));
        StepVerifier.create(facade.agregarProducto("f1","s2","P",1))
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("Sucursal no pertenece a la franquicia"))
                .verify();
    }

    @Test
    @DisplayName("eliminarProducto / actualizarStock: validan sucursal y pertenencia")
    void eliminarYActualizarStock() {
        when(sucursalAdapter.obtenerPorId("s1")).thenReturn(Mono.just(suc("s1","f1","S1")));
        when(productoAdapter.eliminarPorId("p1")).thenReturn(Mono.just("ok"));
        when(productoAdapter.actualizarStock("p1", 9)).thenReturn(Mono.just(prod("p1","s1","P",9)));

        StepVerifier.create(facade.eliminarProducto("f1","s1","p1"))
                .verifyComplete();
        StepVerifier.create(facade.actualizarStock("f1","s1","p1",9))
                .expectNextMatches(p -> p.getStock()==9)
                .verifyComplete();

        when(sucursalAdapter.obtenerPorId("s2")).thenReturn(Mono.just(suc("s2","fX","S2")));
        StepVerifier.create(facade.eliminarProducto("f1","s2","p1"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("productosDeSucursal: valida sucursal y pertenencia")
    void productosDeSucursal() {
        when(sucursalAdapter.obtenerPorId("s1")).thenReturn(Mono.just(suc("s1","f1","S1")));
        when(productoAdapter.listarPorSucursal("s1")).thenReturn(Flux.just(
                prod("p1","s1","A",1), prod("p2","s1","B",2)));

        StepVerifier.create(facade.productosDeSucursal("f1","s1"))
                .expectNextCount(2)
                .verifyComplete();

        when(sucursalAdapter.obtenerPorId("sx")).thenReturn(Mono.empty());
        StepVerifier.create(facade.productosDeSucursal("f1","sx"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Sucursal no existe"))
                .verify();

        when(sucursalAdapter.obtenerPorId("s2")).thenReturn(Mono.just(suc("s2","fX","S2")));
        StepVerifier.create(facade.productosDeSucursal("f1","s2"))
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("Sucursal no pertenece a la franquicia"))
                .verify();
    }

    // ================= Vistas/Consultas compuestas =================

    @Test
    @DisplayName("todosLosProductos: delega en productoAdapter.findAll")
    void todosLosProductos() {
        when(productoAdapter.findAll()).thenReturn(Flux.just(prod("p1","s1","A",1)));
        StepVerifier.create(facade.todosLosProductos())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("actualizarProducto: delega")
    void actualizarProducto() {
        when(productoAdapter.actualizarProducto(eq("p1"), any()))
                .thenReturn(Mono.just(prod("p1","s1","N",2)));

        StepVerifier.create(facade.actualizarProducto("p1", prod(null,null,"N",2)))
                .expectNextMatches(p -> p.getNombre().equals("N"))
                .verifyComplete();
    }

    @Test
    @DisplayName("buscarProductos: trimea parámetro")
    void buscarProductos() {
        when(productoAdapter.buscarPorNombreLike("abc")).thenReturn(Flux.just(prod("p1","s1","abc",1)));
        when(productoAdapter.buscarPorNombreLike("")).thenReturn(Flux.empty());

        StepVerifier.create(facade.buscarProductos("  abc  "))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(facade.buscarProductos(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("productoGlobal: arma mapa combinando producto y sucursal")
    void productoGlobal() {
        Producto p = prod("p1","s1","A",9);
        Sucursal s = suc("s1","f1","S1");

        when(productoAdapter.findById("p1")).thenReturn(Mono.just(p));
        when(sucursalAdapter.obtenerPorId("s1")).thenReturn(Mono.just(s));

        StepVerifier.create(facade.productoGlobal("p1"))
                .assertNext(m -> {
                    Assertions.assertEquals("p1", m.get("productoId"));
                    Assertions.assertEquals("s1", m.get("sucursalId"));
                    Assertions.assertEquals("f1", m.get("franquiciaId"));
                    Assertions.assertEquals(9, m.get("stock"));
                })
                .verifyComplete();

        when(productoAdapter.findById("px")).thenReturn(Mono.empty());
        StepVerifier.create(facade.productoGlobal("px"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Producto no encontrado"))
                .verify();

        when(productoAdapter.findById("p2")).thenReturn(Mono.just(prod("p2","sX","B",1)));
        when(sucursalAdapter.obtenerPorId("sX")).thenReturn(Mono.empty());
        StepVerifier.create(facade.productoGlobal("p2"))
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("Sucursal del producto no existe"))
                .verify();
    }

    @Test
    @DisplayName("todosProductosViewRaw: vista combinada producto+sucursal")
    void todosProductosViewRaw() {
        when(productoAdapter.findAll()).thenReturn(Flux.just(
                prod("p1","s1","A",1),
                prod("p2","s2","B",2)
        ));
        when(sucursalAdapter.obtenerPorId("s1")).thenReturn(Mono.just(suc("s1","f1","S1")));
        when(sucursalAdapter.obtenerPorId("s2")).thenReturn(Mono.just(suc("s2","f2","S2")));

        StepVerifier.create(facade.todosProductosViewRaw())
                .expectNextMatches(o -> {
                    Map<?,?> m = (Map<?,?>) o;
                    return m.get("productoId").equals("p1") && m.get("sucursalId").equals("s1");
                })
                .expectNextMatches(o -> {
                    Map<?,?> m = (Map<?,?>) o;
                    return m.get("productoId").equals("p2") && m.get("sucursalId").equals("s2");
                })
                .verifyComplete();
    }

}
