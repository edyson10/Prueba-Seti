package co.franquicias.mongodb.adapter;

import co.franquicias.model.sucursal.Sucursal;
import co.franquicias.mongodb.entity.SucursalData;
import co.franquicias.mongodb.repository.ReactiveFranquiciaRepository;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SucursalAdapterTest {

    @Mock ReactiveSucursalesRepository repo;
    @Mock ReactiveFranquiciaRepository repoFranquicia;
    @Mock ReactiveMongoTemplate template;

    ModelMapper modelMapper;
    SucursalAdapter adapter;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        adapter = new SucursalAdapter(repo, template, modelMapper, repoFranquicia);
    }

    private SucursalData data(String id, String franquiciaId, String nombre, Instant c, Instant u) {
        SucursalData d = new SucursalData();
        d.setId(id);
        d.setFranquiciaId(franquiciaId);
        d.setNombre(nombre);
        d.setCreatedAt(c);
        d.setUpdatedAt(u);
        return d;
    }
    private Sucursal entity(String id, String franquiciaId, String nombre, Instant c, Instant u) {
        Sucursal s = new Sucursal();
        s.setId(id);
        s.setFranquiciaId(franquiciaId);
        s.setNombre(nombre);
        s.setCreatedAt(c);
        s.setUpdatedAt(u);
        return s;
    }

    // ================== crear ==================
    @Nested
    class Crear {

        @Test
        @DisplayName("crear: OK cuando la franquicia existe y no hay duplicado")
        void crear_ok() {
            String franquiciaId = "F1";
            String nombre = "Sucursal Norte";

            when(repoFranquicia.existsById(franquiciaId)).thenReturn(Mono.just(true));
            when(repo.existsByFranquiciaIdAndNombre(franquiciaId, nombre)).thenReturn(Mono.just(false));
            when(repo.save(any(SucursalData.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(adapter.crear(franquiciaId, nombre))
                    .assertNext(s -> {
                        org.junit.jupiter.api.Assertions.assertNotNull(s.getId());
                        org.junit.jupiter.api.Assertions.assertFalse(s.getId().isBlank());
                        org.junit.jupiter.api.Assertions.assertEquals(franquiciaId, s.getFranquiciaId());
                        org.junit.jupiter.api.Assertions.assertEquals(nombre, s.getNombre());
                        org.junit.jupiter.api.Assertions.assertNotNull(s.getCreatedAt());
                        org.junit.jupiter.api.Assertions.assertNotNull(s.getUpdatedAt());
                    })
                    .verifyComplete();

            ArgumentCaptor<SucursalData> cap = ArgumentCaptor.forClass(SucursalData.class);
            verify(repo).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals(nombre, cap.getValue().getNombre());
            org.junit.jupiter.api.Assertions.assertEquals(franquiciaId, cap.getValue().getFranquiciaId());
        }

        @Test
        @DisplayName("crear: falla si la franquicia no existe")
        void crear_franquiciaNoExiste() {
            String franquiciaId = "FX";
            when(repoFranquicia.existsById(franquiciaId)).thenReturn(Mono.just(false));

            StepVerifier.create(adapter.crear(franquiciaId, "S1"))
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().contains("La franquicia no existe: " + franquiciaId))
                    .verify();

            verify(repo, never()).existsByFranquiciaIdAndNombre(anyString(), anyString());
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("crear: falla si ya existe sucursal con el mismo nombre en la franquicia")
        void crear_duplicado() {
            String franquiciaId = "F1";
            String nombre = "Sucursal A";

            when(repoFranquicia.existsById(franquiciaId)).thenReturn(Mono.just(true));
            when(repo.existsByFranquiciaIdAndNombre(franquiciaId, nombre)).thenReturn(Mono.just(true));

            StepVerifier.create(adapter.crear(franquiciaId, nombre))
                    .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                            ex.getMessage().contains("Sucursal ya existe en la franquicia"))
                    .verify();

            verify(repo, never()).save(any());
        }
    }

    // ================== consultas/eliminación ==================
    @Test
    @DisplayName("listarPorFranquicia: mapea documentos a dominio")
    void listarPorFranquicia() {
        Instant t = Instant.now();
        when(repo.findByFranquiciaId("F1")).thenReturn(Flux.just(
                data("s1","F1","A",t,t),
                data("s2","F1","B",t,t)
        ));

        StepVerifier.create(adapter.listarPorFranquicia("F1"))
                .expectNextMatches(s -> s.getId().equals("s1") && s.getNombre().equals("A"))
                .expectNextMatches(s -> s.getId().equals("s2") && s.getNombre().equals("B"))
                .verifyComplete();
    }

    @Test
    @DisplayName("obtenerPorId: delega en repo.findById y mapea")
    void obtenerPorId() {
        Instant t = Instant.now();
        when(repo.findById("s1")).thenReturn(Mono.just(data("s1","F1","Centro",t,t)));

        StepVerifier.create(adapter.obtenerPorId("s1"))
                .expectNextMatches(s -> s.getId().equals("s1") && s.getNombre().equals("Centro"))
                .verifyComplete();
    }

    @Test
    @DisplayName("eliminarPorId: retorna mensaje al completar")
    void eliminarPorId() {
        when(repo.deleteById("s1")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.eliminarPorId("s1"))
                .expectNext("Sucursal eliminada correctamente")
                .verifyComplete();
    }

    // ================== actualizar ==================
    @Nested
    class Actualizar {

        @Test
        @DisplayName("actualizarSucursal: OK (merge no nulos; preserva franquiciaId y createdAt)")
        void actualizar_ok() {
            String id = "s1";
            Instant created = Instant.parse("2024-01-01T00:00:00Z");
            SucursalData existente = data(id, "F1", "Vieja", created, created);

            when(repo.findById(id)).thenReturn(Mono.just(existente));
            when(repo.save(any(SucursalData.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            Sucursal cambios = entity(null, null, "Nueva", null, null);

            StepVerifier.create(adapter.actualizarSucursal(id, cambios))
                    .assertNext(s -> {
                        org.junit.jupiter.api.Assertions.assertEquals(id, s.getId());
                        org.junit.jupiter.api.Assertions.assertEquals("Nueva", s.getNombre());
                        org.junit.jupiter.api.Assertions.assertEquals("F1", s.getFranquiciaId()); // preserva
                        org.junit.jupiter.api.Assertions.assertEquals(created, s.getCreatedAt());
                        org.junit.jupiter.api.Assertions.assertNotNull(s.getUpdatedAt());
                    })
                    .verifyComplete();

            ArgumentCaptor<SucursalData> cap = ArgumentCaptor.forClass(SucursalData.class);
            verify(repo).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals("Nueva", cap.getValue().getNombre());
            org.junit.jupiter.api.Assertions.assertEquals("F1", cap.getValue().getFranquiciaId());
        }

        @Test
        @DisplayName("actualizarSucursal: valida franquicia destino si cambia y falla cuando no existe")
        void actualizar_franquiciaDestinoNoExiste() {
            String id = "s1";

            Sucursal cambios = entity(null, null, "Nombre", null, null);
            cambios.setFranquiciaId("F999");
            org.junit.jupiter.api.Assertions.assertEquals("F999", cambios.getFranquiciaId());

            when(repoFranquicia.existsById(anyString()))
                    .thenAnswer(inv -> {
                        String fid = inv.getArgument(0);
                        org.junit.jupiter.api.Assertions.assertEquals("F999", fid);
                        return Mono.just(false);
                    });

            when(repo.findById(anyString())).thenReturn(Mono.never());

            StepVerifier.create(adapter.actualizarSucursal(id, cambios))
                    .expectErrorMatches(ex -> ex instanceof IllegalArgumentException &&
                            ex.getMessage().contains("La franquicia destino no existe: F999"))
                    .verify();

            verify(repoFranquicia, times(1)).existsById("F999");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("actualizarSucursal: DuplicateKeyException → IllegalStateException")
        void actualizar_duplicateKey() {
            String id = "s1";
            Instant created = Instant.parse("2024-01-01T00:00:00Z");
            SucursalData existente = data(id, "F1", "Vieja", created, created);

            when(repo.findById(id)).thenReturn(Mono.just(existente));
            when(repo.save(any(SucursalData.class)))
                    .thenReturn(Mono.error(new DuplicateKeyException("dup nombre")));

            Sucursal cambios = entity(null, null, "Conflicto", null, null);

            StepVerifier.create(adapter.actualizarSucursal(id, cambios))
                    .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                            ex.getMessage().contains("Ya existe una sucursal con ese nombre en la franquicia"))
                    .verify();
        }
    }
}
