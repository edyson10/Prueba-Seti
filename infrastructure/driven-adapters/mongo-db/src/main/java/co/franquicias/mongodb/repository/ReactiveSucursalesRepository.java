package co.franquicias.mongodb.repository;

import co.franquicias.mongodb.entity.SucursalData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveSucursalesRepository extends ReactiveMongoRepository<SucursalData, String> {
    Flux<SucursalData> findByFranquiciaId(String franquiciaId);
    Mono<Boolean> existsByFranquiciaIdAndNombre(String franquiciaId, String nombre);
}