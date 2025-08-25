package co.franquicias.mongodb.adapter;

import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.mongodb.entity.FranquiciaData;
import co.franquicias.mongodb.repository.ReactiveFranquiciaRepository;
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
class FranquiciaAdapterTest {

    @Mock ReactiveFranquiciaRepository repo;
    @Mock ReactiveMongoTemplate template;

    ModelMapper modelMapper;
    FranquiciaAdapter adapter;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        adapter = new FranquiciaAdapter(repo, template, modelMapper);
    }

    // ===== Helpers =====
    private FranquiciaData data(String id, String nombre, Instant created, Instant updated) {
        FranquiciaData d = new FranquiciaData();
        d.setId(id);
        d.setNombre(nombre);
        d.setCreatedAt(created);
        d.setUpdatedAt(updated);
        return d;
    }

    private Franquicia entity(String id, String nombre, Instant created, Instant updated) {
        Franquicia f = new Franquicia();
        f.setId(id);
        f.setNombre(nombre);
        f.setCreatedAt(created);
        f.setUpdatedAt(updated);
        return f;
    }

    @Nested
    class CrearFranquicia {

        @Test
        @DisplayName("crearFranquicia: crea cuando no existe y mapea a dominio")
        void crear_ok() {
            String nombre = "Mi Franquicia";
            when(repo.existsByNombre(nombre)).thenReturn(Mono.just(false));
            when(repo.save(any(FranquiciaData.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(adapter.crearFranquicia(nombre))
                    .assertNext(f -> {
                        org.junit.jupiter.api.Assertions.assertNotNull(f.getId());
                        org.junit.jupiter.api.Assertions.assertFalse(f.getId().isBlank());
                        org.junit.jupiter.api.Assertions.assertEquals(nombre, f.getNombre());
                        org.junit.jupiter.api.Assertions.assertNotNull(f.getCreatedAt());
                        org.junit.jupiter.api.Assertions.assertNotNull(f.getUpdatedAt());
                    })
                    .verifyComplete();
            ArgumentCaptor<FranquiciaData> cap = ArgumentCaptor.forClass(FranquiciaData.class);
            verify(repo).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals(nombre, cap.getValue().getNombre());
        }

        @Test
        @DisplayName("crearFranquicia: falla con IllegalStateException si ya existe")
        void crear_duplicado() {
            String nombre = "Repetida";
            when(repo.existsByNombre(nombre)).thenReturn(Mono.just(true));

            StepVerifier.create(adapter.crearFranquicia(nombre))
                    .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                            ex.getMessage().contains("Franquicia ya existe"))
                    .verify();

            verify(repo, never()).save(any());
        }
    }

    @Nested
    class LecturasYEliminacion {

        @Test
        @DisplayName("obtenerPorId: delega en findById del repositorio base y mapea")
        void obtenerPorId() {
            Instant t = Instant.now();
            FranquiciaData d = data("id-1", "F1", t, t);
            when(repo.findById("id-1")).thenReturn(Mono.just(d));

            StepVerifier.create(adapter.obtenerPorId("id-1"))
                    .expectNextMatches(f -> f.getId().equals("id-1") && f.getNombre().equals("F1"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("obtenerFranquicias: delega en findAll y mapea")
        void obtenerFranquicias() {
            Instant t = Instant.now();
            when(repo.findAll()).thenReturn(Flux.just(
                    data("1", "A", t, t),
                    data("2", "B", t, t)
            ));

            StepVerifier.create(adapter.obtenerFranquicias())
                    .expectNextCount(2)
                    .verifyComplete();
        }

        @Test
        @DisplayName("obtenerPorNombre: usa repo.findByNombre y mapea")
        void obtenerPorNombre() {
            Instant t = Instant.now();
            when(repo.findByNombre("Uniq")).thenReturn(Mono.just(data("id-7", "Uniq", t, t)));

            StepVerifier.create(adapter.obtenerPorNombre("Uniq"))
                    .expectNextMatches(f -> f.getId().equals("id-7") && f.getNombre().equals("Uniq"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("eliminarPorId: retorna mensaje tras borrar")
        void eliminarPorId() {
            when(repo.deleteById("x")).thenReturn(Mono.empty());

            StepVerifier.create(adapter.eliminarPorId("x"))
                    .expectNext("Franquicia eliminada correctamente")
                    .verifyComplete();
        }
    }

    @Nested
    class Actualizar {

        @Test
        @DisplayName("actualizarFranquicia: merge no nulos (actualiza nombre, preserva createdAt)")
        void actualizar_ok() {
            Instant created = Instant.parse("2024-01-01T00:00:00Z");
            Instant before = Instant.parse("2024-02-01T00:00:00Z");

            FranquiciaData existente = data("abc", "Vieja", created, before);

            when(repo.findById("abc")).thenReturn(Mono.just(existente));
            when(repo.save(any(FranquiciaData.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            Franquicia cambios = entity(null, "Nueva", null, null);

            StepVerifier.create(adapter.actualizarFranquicia("abc", cambios))
                    .assertNext(f -> {
                        org.junit.jupiter.api.Assertions.assertEquals("abc", f.getId());
                        org.junit.jupiter.api.Assertions.assertEquals("Nueva", f.getNombre());
                        org.junit.jupiter.api.Assertions.assertEquals(created, f.getCreatedAt()); // preserva createdAt
                        org.junit.jupiter.api.Assertions.assertNotNull(f.getUpdatedAt());        // actualizado
                    })
                    .verifyComplete();

            ArgumentCaptor<FranquiciaData> cap = ArgumentCaptor.forClass(FranquiciaData.class);
            verify(repo).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals("Nueva", cap.getValue().getNombre());
            org.junit.jupiter.api.Assertions.assertEquals(created, cap.getValue().getCreatedAt());
        }

        @Test
        @DisplayName("actualizarFranquicia: mapea DuplicateKeyException a IllegalStateException")
        void actualizar_duplicateKey() {
            Instant created = Instant.parse("2024-01-01T00:00:00Z");
            FranquiciaData existente = data("abc", "Vieja", created, created);

            when(repo.findById("abc")).thenReturn(Mono.just(existente));
            when(repo.save(any(FranquiciaData.class)))
                    .thenReturn(Mono.error(new DuplicateKeyException("dup index nombre")));

            Franquicia cambios = entity(null, "NombreDuplicado", null, null);

            StepVerifier.create(adapter.actualizarFranquicia("abc", cambios))
                    .expectErrorMatches(ex -> ex instanceof IllegalStateException &&
                            ex.getMessage().contains("El nombre de la franquicia ya existe"))
                    .verify();
        }
    }
}
