package co.franquicias.mongodb.repository;

import co.franquicias.mongodb.entity.FranquiciaData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ReactiveFranquiciaRepository
        extends ReactiveMongoRepository<FranquiciaData, String> {

    Mono<Boolean> existsByNombre(String nombre);
    Mono<FranquiciaData> findByNombre(String nombre);
    Mono<Void> deleteById(String nombre);
}
