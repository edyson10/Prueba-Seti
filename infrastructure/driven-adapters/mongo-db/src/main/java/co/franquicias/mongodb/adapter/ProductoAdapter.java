package co.franquicias.mongodb.adapter;

import co.franquicias.model.producto.Producto;
import co.franquicias.mongodb.entity.ProductoData;
import co.franquicias.mongodb.helper.MongoDBAdapterOperations;
import co.franquicias.mongodb.repository.ReactiveProductosRepository;
import co.franquicias.mongodb.repository.ReactiveSucursalesRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class ProductoAdapter
        extends MongoDBAdapterOperations<Producto, ProductoData, String> {

    private final ReactiveProductosRepository repo;
    private final ReactiveSucursalesRepository repoSucursales;

    public ProductoAdapter(ReactiveProductosRepository repo,
                           ReactiveMongoTemplate template,
                           ModelMapper mm,
                           ReactiveSucursalesRepository repoSucursales) {
        super(repo, template, ProductoData.class, Producto.class, mm);
        this.repo = repo;
        this.repoSucursales = repoSucursales;
    }

    public Mono<Producto> crear(String sucursalId, String nombre, int stock) {
        var now = Instant.now();
        var data = ProductoData.builder()
                .id(UUID.randomUUID().toString())
                .sucursalId(sucursalId)
                .nombre(nombre)
                .stock(stock)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return repoSucursales.existsById(sucursalId)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? repo.existsBySucursalIdAndNombre(sucursalId, nombre)
                        .flatMap(dup -> dup
                                ? Mono.error(new IllegalStateException("Producto ya existe en la sucursal"))
                                : repo.save(data))
                        : Mono.error(new IllegalArgumentException("La sucursal destino no existe: " + sucursalId))
                )
                .map(this::toEntity);
    }

    public Flux<Producto> listarPorSucursal(String sucursalId) {
        return mapFluxDocToEntity(repo.findBySucursalId(sucursalId));
    }

    public Flux<Producto> buscarPorNombreLike(String nombreLike) {
        String regex = ".*" + java.util.regex.Pattern.quote(nombreLike) + ".*";
        return mapFluxDocToEntity(repo.findByNombreRegex("(?i)" + regex));
    }

    public Mono<Producto> actualizarStock(String id, int stock) {
        var q = new Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id));
        var u = new org.springframework.data.mongodb.core.query.Update()
                .set("stock", stock)
                .set("updatedAt", Instant.now());

        return updateFirstMatched(q, u)
                .flatMap(matched -> Boolean.TRUE.equals(matched)
                        ? findById(id)
                        : Mono.error(new IllegalArgumentException("Producto no encontrado")));
    }

    public Mono<String> eliminarPorId(String id) {
        return repo.deleteById(id).thenReturn("Producto eliminado correctamente");
    }

    public Mono<Producto> actualizarProducto(String id, Producto cambios) {
        cambios.setUpdatedAt(Instant.now());
        return validarSucursal(cambios.getSucursalId())
                .then(mergeNonNullAndSave(id, cambios))
                .onErrorMap(DuplicateKeyException.class,
                        e -> new IllegalStateException("Ya existe un producto con ese nombre en la sucursal", e));
    }

    private Mono<Void> validarSucursal(String sucursalId) {
        return Mono.justOrEmpty(sucursalId)
                .flatMap(id -> repoSucursales.existsById(id)
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.<Void>empty()
                                : Mono.error(new IllegalArgumentException(
                                "La sucursal destino no existe: " + id))));
    }
}
