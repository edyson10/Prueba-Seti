package co.franquicias.mongodb.adapter;

import co.franquicias.model.producto.Producto;
import co.franquicias.mongodb.entity.ProductoData;
import co.franquicias.mongodb.repository.ReactiveProductosRepository;
import co.franquicias.mongodb.repository.ReactiveSucursalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.mongodb.client.result.UpdateResult;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoAdapterTest {

    @Mock ReactiveProductosRepository repo;
    @Mock ReactiveSucursalesRepository repoSucursales;
    @Mock ReactiveMongoTemplate template;

    ProductoAdapter adapter;
    ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        adapter = new ProductoAdapter(repo, template, modelMapper, repoSucursales);
    }

    // ---- Helpers simples (ajusta a tus builders reales si los usas) ----
    private ProductoData data(String id, String sucursalId, String nombre, int stock, Instant c, Instant u) {
        ProductoData d = new ProductoData();
        d.setId(id);
        d.setSucursalId(sucursalId);
        d.setNombre(nombre);
        d.setStock(stock);
        d.setCreatedAt(c);
        d.setUpdatedAt(u);
        return d;
    }
    private Producto entity(String id, String sucursalId, String nombre, int stock, Instant c, Instant u) {
        Producto p = new Producto();
        p.setId(id);
        p.setSucursalId(sucursalId);
        p.setNombre(nombre);
        p.setStock(stock);
        p.setCreatedAt(c);
        p.setUpdatedAt(u);
        return p;
    }

    // -------------------- crear --------------------
    @Nested
    class Crear {

        @Test
        @DisplayName("crear: OK cuando la sucursal existe y no hay duplicado")
        void crear_ok() {
            String sucursalId = "S1";
            String nombre = "Coca Cola";
            int stock = 10;

            when(repoSucursales.existsById(sucursalId)).thenReturn(Mono.just(true));
            when(repo.existsBySucursalIdAndNombre(sucursalId, nombre)).thenReturn(Mono.just(false));
            when(repo.save(any(ProductoData.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(adapter.crear(sucursalId, nombre, stock))
                    .assertNext(p -> {
                        org.junit.jupiter.api.Assertions.assertNotNull(p.getId());
                        org.junit.jupiter.api.Assertions.assertFalse(p.getId().isBlank());
                        org.junit.jupiter.api.Assertions.assertEquals(sucursalId, p.getSucursalId());
                        org.junit.jupiter.api.Assertions.assertEquals(nombre, p.getNombre());
                        org.junit.jupiter.api.Assertions.assertEquals(stock, p.getStock());
                        org.junit.jupiter.api.Assertions.assertNotNull(p.getCreatedAt());
                        org.junit.jupiter.api.Assertions.assertNotNull(p.getUpdatedAt());
                    })
                    .verifyComplete();

            ArgumentCaptor<ProductoData> cap = ArgumentCaptor.forClass(ProductoData.class);
            verify(repo).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals(nombre, cap.getValue().getNombre());
            org.junit.jupiter.api.Assertions.assertEquals(stock, cap.getValue().getStock());
        }

        @Test
        @DisplayName("crear: falla si la sucursal no existe")
        void crear_sucursalNoExiste() {
            String sucursalId = "SX";
            when(repoSucursales.existsById(sucursalId)).thenReturn(Mono.just(false));

            StepVerifier.create(adapter.crear(sucursalId, "X", 1))
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().contains("La sucursal destino no existe: " + sucursalId))
                    .verify();

            verify(repo, never()).existsBySucursalIdAndNombre(anyString(), anyString());
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("crear: falla si ya existe producto con el mismo nombre en la sucursal")
        void crear_duplicado() {
            String sucursalId = "S1";
            String nombre = "Agua";
            when(repoSucursales.existsById(sucursalId)).thenReturn(Mono.just(true));
            when(repo.existsBySucursalIdAndNombre(sucursalId, nombre)).thenReturn(Mono.just(true));

            StepVerifier.create(adapter.crear(sucursalId, nombre, 1))
                    .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                            ex.getMessage().contains("Producto ya existe en la sucursal"))
                    .verify();

            verify(repo, never()).save(any());
        }
    }

    // -------------------- listarPorSucursal --------------------
    @Test
    @DisplayName("listarPorSucursal: mapea los documentos a dominio")
    void listarPorSucursal() {
        Instant t = Instant.now();
        when(repo.findBySucursalId("S1")).thenReturn(Flux.just(
                data("p1","S1","Pan",5,t,t),
                data("p2","S1","Leche",8,t,t)
        ));

        StepVerifier.create(adapter.listarPorSucursal("S1"))
                .expectNextMatches(p -> p.getId().equals("p1") && p.getNombre().equals("Pan"))
                .expectNextMatches(p -> p.getId().equals("p2") && p.getNombre().equals("Leche"))
                .verifyComplete();
    }

    // -------------------- buscarPorNombreLike --------------------
    @Test
    @DisplayName("buscarPorNombreLike: usa regex (?i).*<quote(nombreLike)>. * y mapea")
    void buscarPorNombreLike() {
        Instant t = Instant.now();
        when(repo.findByNombreRegex(anyString()))
                .thenReturn(Flux.just(data("p1","S1","Cola Zero",3,t,t)));

        StepVerifier.create(adapter.buscarPorNombreLike("Cola (Zero)"))
                .expectNextMatches(p -> p.getNombre().equals("Cola Zero"))
                .verifyComplete();

        ArgumentCaptor<String> patternCap = ArgumentCaptor.forClass(String.class);
        verify(repo).findByNombreRegex(patternCap.capture());
        String pattern = patternCap.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(pattern.startsWith("(?i).*"));
        org.junit.jupiter.api.Assertions.assertTrue(pattern.endsWith(".*"));
        org.junit.jupiter.api.Assertions.assertTrue(pattern.contains(java.util.regex.Pattern.quote("Cola (Zero)")));
    }

    // -------------------- actualizarStock --------------------
    @Nested
    class ActualizarStock {

        @Test
        @DisplayName("actualizarStock: si hay match, luego lee por id y retorna entidad")
        void actualizarStock_ok() {
            String id = "p1";
            Instant t = Instant.now();
            ProductoData mod = data(id,"S1","Pan",15,t,t);

            when(template.updateFirst(any(Query.class), any(Update.class), eq(ProductoData.class)))
                    .thenReturn(Mono.just(UpdateResult.acknowledged(1L, 1L, null)));
            when(repo.findById(id)).thenReturn(Mono.just(mod));

            StepVerifier.create(adapter.actualizarStock(id, 15))
                    .expectNextMatches(p -> p.getId().equals(id) && p.getStock() == 15)
                    .verifyComplete();
        }

        @Test
        @DisplayName("actualizarStock: si no hay match, error 'Producto no encontrado'")
        void actualizarStock_noMatch() {
            when(template.updateFirst(any(Query.class), any(Update.class), eq(ProductoData.class)))
                    .thenReturn(Mono.just(UpdateResult.acknowledged(0L, 0L, null)));

            StepVerifier.create(adapter.actualizarStock("nope", 10))
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().contains("Producto no encontrado"))
                    .verify();

            verify(repo, never()).findById(anyString());
        }
    }

    // -------------------- eliminarPorId --------------------
    @Test
    @DisplayName("eliminarPorId: retorna mensaje al completar")
    void eliminarPorId() {
        when(repo.deleteById("p1")).thenReturn(Mono.empty());
        StepVerifier.create(adapter.eliminarPorId("p1"))
                .expectNext("Producto eliminado correctamente")
                .verifyComplete();
    }

    // -------------------- actualizarProducto --------------------
    @Nested
    class ActualizarProducto {

        @Test
        @DisplayName("actualizarProducto: OK (valida sucursal si viene, merge y mapea DuplicateKeyException)")
        void actualizar_ok() {
            String id = "p1";
            Instant created = Instant.parse("2024-01-01T00:00:00Z");
            ProductoData existente = data(id,"S1","Viejo",5,created,created);

            when(repo.findById(id)).thenReturn(Mono.just(existente));
            when(repo.save(any(ProductoData.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            Producto cambios = entity(null, null, "Nuevo", 0, null, null); // sólo nombre
            StepVerifier.create(adapter.actualizarProducto(id, cambios))
                    .assertNext(p -> {
                        org.junit.jupiter.api.Assertions.assertEquals(id, p.getId());
                        org.junit.jupiter.api.Assertions.assertEquals("Nuevo", p.getNombre());
                        org.junit.jupiter.api.Assertions.assertEquals("S1", p.getSucursalId()); // preserva
                        org.junit.jupiter.api.Assertions.assertEquals(created, p.getCreatedAt());
                        org.junit.jupiter.api.Assertions.assertNotNull(p.getUpdatedAt());
                    })
                    .verifyComplete();

            ArgumentCaptor<ProductoData> cap = ArgumentCaptor.forClass(ProductoData.class);
            verify(repo).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals("Nuevo", cap.getValue().getNombre());
        }

        @Test
        @DisplayName("actualizarProducto: valida sucursal destino si se cambia y falla cuando no existe")
        void actualizar_sucursalDestinoNoExiste() {
            String id = "p1";

            Producto cambios = entity(null, null, "Nombre", 0, null, null);
            cambios.setSucursalId("S999");

            org.junit.jupiter.api.Assertions.assertEquals("S999", cambios.getSucursalId());

            when(repoSucursales.existsById(anyString()))
                    .thenAnswer(inv -> {
                        String sid = inv.getArgument(0);
                        org.junit.jupiter.api.Assertions.assertEquals("S999", sid);
                        return Mono.just(false);
                    });

            when(repo.findById(anyString()))
                    .thenReturn(Mono.never());

            StepVerifier.create(adapter.actualizarProducto(id, cambios))
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().contains("La sucursal destino no existe: S999"))
                    .verify();

            verify(repoSucursales, times(1)).existsById("S999");
            verify(repo, never()).save(any());
        }


        @Test
        @DisplayName("actualizarProducto: DuplicateKeyException → IllegalStateException con mensaje claro")
        void actualizar_duplicateKey() {
            String id = "p1";
            Instant created = Instant.parse("2024-01-01T00:00:00Z");
            ProductoData existente = data(id,"S1","Viejo",5,created,created);

            when(repo.findById(id)).thenReturn(Mono.just(existente));
            when(repo.save(any(ProductoData.class)))
                    .thenReturn(Mono.error(new DuplicateKeyException("dup nombre en sucursal")));

            Producto cambios = entity(null, null, "NuevoConflicto", 0, null, null);

            StepVerifier.create(adapter.actualizarProducto(id, cambios))
                    .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                            ex.getMessage().contains("Ya existe un producto con ese nombre en la sucursal"))
                    .verify();
        }
    }
}
