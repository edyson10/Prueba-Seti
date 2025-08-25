package co.franquicias.mongodb.repository;

import co.franquicias.mongodb.entity.ProductoData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveProductosRepository extends ReactiveMongoRepository<ProductoData, String> {
    Flux<ProductoData> findBySucursalId(String sucursalId);
    Mono<Boolean> existsBySucursalIdAndNombre(String sucursalId, String nombre);
    Flux<ProductoData> findByNombreRegex(String nombreRegex);
}