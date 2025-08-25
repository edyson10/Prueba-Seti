package co.franquicias.mongodb.adapter;

import co.franquicias.model.franquicia.Franquicia;
import co.franquicias.mongodb.entity.FranquiciaData;
import co.franquicias.mongodb.helper.MongoDBAdapterOperations;
import co.franquicias.mongodb.repository.ReactiveFranquiciaRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class FranquiciaAdapter
        extends MongoDBAdapterOperations<Franquicia, FranquiciaData, String> {

    private final ReactiveFranquiciaRepository repo;

    public FranquiciaAdapter(ReactiveFranquiciaRepository repo,
                             ReactiveMongoTemplate template,
                             ModelMapper mm) {
        super(repo, template, FranquiciaData.class, Franquicia.class, mm);
        this.repo = repo;
    }

    public Mono<Franquicia> crearFranquicia(String nombre) {
        var data = FranquiciaData.builder()
                .id(UUID.randomUUID().toString())
                .nombre(nombre)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return repo.existsByNombre(nombre)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.error(new IllegalStateException("Franquicia ya existe"))
                        : repo.save(data))
                .map(this::toEntity);
    }

    public Mono<Franquicia> obtenerPorId(String id) {
        return findById(id);
    }

    public Flux<Franquicia> obtenerFranquicias() {
        return findAll();
    }

    public Mono<Franquicia> obtenerPorNombre(String nombre) {
        return repo.findByNombre(nombre).map(this::toEntity);
    }

    public Mono<String> eliminarPorId(String id) {
        return repo.deleteById(id).thenReturn("Franquicia eliminada correctamente");
    }

    public Mono<Franquicia> actualizarFranquicia(String franquiciaId, Franquicia cambios) {
        cambios.setUpdatedAt(Instant.now());
        return mergeNonNullAndSave(franquiciaId, cambios)
                .onErrorMap(DuplicateKeyException.class,
                        e -> new IllegalStateException("El nombre de la franquicia ya existe", e));
    }
}
