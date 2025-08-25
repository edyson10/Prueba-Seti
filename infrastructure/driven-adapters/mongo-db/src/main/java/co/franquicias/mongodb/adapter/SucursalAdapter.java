package co.franquicias.mongodb.adapter;

import co.franquicias.model.sucursal.Sucursal;
import co.franquicias.mongodb.entity.SucursalData;
import co.franquicias.mongodb.helper.MongoDBAdapterOperations;
import co.franquicias.mongodb.repository.ReactiveFranquiciaRepository;
import co.franquicias.mongodb.repository.ReactiveSucursalesRepository;
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
public class SucursalAdapter
        extends MongoDBAdapterOperations<Sucursal, SucursalData, String> {

    private final ReactiveSucursalesRepository repo;
    private final ReactiveFranquiciaRepository repoFranquicia;

    public SucursalAdapter(ReactiveSucursalesRepository repo,
                           ReactiveMongoTemplate template,
                           ModelMapper mm,
                           ReactiveFranquiciaRepository repoFranquicia) {
        super(repo, template, SucursalData.class, Sucursal.class, mm);
        this.repo = repo;
        this.repoFranquicia = repoFranquicia;
    }

    public Mono<Sucursal> crear(String franquiciaId, String nombre) {
        var now = Instant.now();
        var data = SucursalData.builder()
                .id(UUID.randomUUID().toString())
                .franquiciaId(franquiciaId)
                .nombre(nombre)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return repoFranquicia.existsById(franquiciaId)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? repo.existsByFranquiciaIdAndNombre(franquiciaId, nombre)
                        .flatMap(dup -> dup
                                ? Mono.error(new IllegalStateException("Sucursal ya existe en la franquicia"))
                                : repo.save(data))
                        : Mono.error(new IllegalArgumentException("La franquicia no existe: " + franquiciaId))
                )
                .map(this::toEntity);
    }

    public Flux<Sucursal> listarPorFranquicia(String franquiciaId) {
        return mapFluxDocToEntity(repo.findByFranquiciaId(franquiciaId));
    }

    public Mono<Sucursal> obtenerPorId(String id) {
        return findById(id);
    }

    public Mono<String> eliminarPorId(String id) {
        return deleteById(id).thenReturn("Sucursal eliminada correctamente");
    }

    public Mono<Sucursal> actualizarSucursal(String id, Sucursal cambios) {
        cambios.setUpdatedAt(Instant.now());
        return validarFranquicia(cambios.getFranquiciaId())
                .then(mergeNonNullAndSave(id, cambios))
                .onErrorMap(DuplicateKeyException.class,
                        e -> new IllegalStateException("Ya existe una sucursal con ese nombre en la franquicia", e));
    }

    private Mono<Void> validarFranquicia(String franquiciaId) {
        return Mono.justOrEmpty(franquiciaId)
                .flatMap(id -> repoFranquicia.existsById(id)
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.<Void>empty()
                                : Mono.error(new IllegalArgumentException(
                                "La franquicia destino no existe: " + id))));
    }
}
