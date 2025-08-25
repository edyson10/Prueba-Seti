package co.franquicias.mongodb.helper;

import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.*;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

/**
 * Operaciones comunes para adapters MongoDB con mapeo automático
 * entre Documento Mongo (D) y Modelo de Dominio (E).
 *
 * @param <E> Tipo del modelo de dominio (puro, sin dependencias)
 * @param <D> Tipo del documento MongoDB (entidad de infraestructura)
 * @param <I> Tipo del identificador
 */
public abstract class MongoDBAdapterOperations<E, D, I> {

    protected final ReactiveMongoRepository<D, I> repository;
    protected final ReactiveMongoTemplate mongoTemplate;
    protected final Class<D> documentClass;
    protected final Class<E> entityClass;
    protected final ModelMapper modelMapper;

    protected MongoDBAdapterOperations(
            ReactiveMongoRepository<D, I> repository,
            ReactiveMongoTemplate mongoTemplate,
            Class<D> documentClass,
            Class<E> entityClass,
            ModelMapper modelMapper
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate);
        this.documentClass = Objects.requireNonNull(documentClass);
        this.entityClass = Objects.requireNonNull(entityClass);
        this.modelMapper = Objects.requireNonNull(modelMapper);
        configureModelMapper();
    }

    /** Permite registrar TypeMaps/Converters específicos en el adapter concreto. */
    protected void configureModelMapper() { /* no-op por defecto */ }

    // ===================== Mapping =====================

    /** Convierte Modelo (E) -> Documento (D). */
    protected D toDocument(E entity) {
        return entity == null ? null : modelMapper.map(entity, documentClass);
    }

    /** Convierte Documento (D) -> Modelo (E). */
    protected E toEntity(D document) {
        return document == null ? null : modelMapper.map(document, entityClass);
    }

    /** Mapea cualquier objeto a un tipo objetivo (útil para subdocumentos). */
    protected <T> T map(Object source, Class<T> targetType) {
        return source == null ? null : modelMapper.map(source, targetType);
    }

    /** Mapea colección a lista destino de un tipo concreto (útil para subdocumentos). */
    protected <S, T> List<T> mapList(Collection<S> source, Class<T> targetElemType) {
        return source == null ? List.of() : source.stream().map(s -> modelMapper.map(s, targetElemType)).toList();
    }

    protected Mono<E> mapMonoDocToEntity(Mono<D> monoDoc) {
        return monoDoc.map(this::toEntity);
    }

    protected Flux<E> mapFluxDocToEntity(Flux<D> fluxDoc) {
        return fluxDoc.map(this::toEntity);
    }

    // ===================== CRUD simples (via ReactiveMongoRepository) =====================

    public Mono<E> save(E entity) {
        return Mono.just(entity)
                .map(this::toDocument)
                .flatMap(repository::save)
                .map(this::toEntity);
    }

    public Flux<E> saveAll(Flux<E> entities) {
        return repository.saveAll(entities.map(this::toDocument))
                .map(this::toEntity);
    }

    public Mono<E> findById(I id) {
        return repository.findById(id).map(this::toEntity);
    }

    public Flux<E> findAll() {
        return repository.findAll().map(this::toEntity);
    }

    public Mono<Void> deleteById(I id) {
        return repository.deleteById(id);
    }

    // ===================== Consultas/Updates con Template (devolviendo Modelos) =====================

    /** Encuentra muchos documentos por Query y devuelve modelos. */
    protected Flux<E> findByQuery(Query query) {
        return mongoTemplate.find(query, documentClass).map(this::toEntity);
    }

    /** Encuentra un documento por Query y devuelve modelo. */
    protected Mono<E> findOneByQuery(Query query) {
        return mongoTemplate.findOne(query, documentClass).map(this::toEntity);
    }

    /** findAndModify que retorna el documento modificado como Modelo (E). */
    protected Mono<E> findAndModifyReturningEntity(Query query, Update update) {
        return mongoTemplate.findAndModify(query, update, options().returnNew(true), documentClass)
                .map(this::toEntity);
    }

    /**
     * updateFirst que devuelve true si hubo match (útil para saber si se actualizó).
     * Puedes encadenar luego una lectura para devolver el Modelo completo.
     */
    protected Mono<Boolean> updateFirstMatched(Query query, Update update) {
        return mongoTemplate.updateFirst(query, update, documentClass)
                .map(result -> result.getMatchedCount() > 0);
    }

    // ===================== Actualización parcial (merge no nulos) =====================

    /**
     * Mezcla campos no nulos del Modelo (E) sobre el Documento actual (D) y persiste.
     * Útil para PATCH simples. Evita sobreescritura con nulls.
     */
    protected Mono<E> mergeNonNullAndSave(I id, E partialEntity) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Recurso no encontrado")))
                .flatMap(existingDoc -> {
                    D patchDoc = toDocument(partialEntity);
                    copyNonNullProperties(patchDoc, existingDoc);
                    return repository.save(existingDoc);
                })
                .map(this::toEntity);
    }

    protected void copyNonNullProperties(D source, D target) {
        copyNonNullProperties(source, target, Set.of("id", "version", "createdAt"));
    }

    /**
     * Copia propiedades "presentes" de source → target:
     * - Ignora null
     * - Optional: ignora Optional.empty(), usa Optional.get() si presente
     * - String: trim(); si queda vacío, no copia
     * - Collection / Map: si están vacíos, no copia
     * - Excluye los campos indicados en `excluded`
     */
    protected void copyNonNullProperties(D source, D target, Set<String> excluded) {
        if (source == null || target == null) {
            return;
        }
        try {
            Map<String, Object> values = buildSourceValues(source, excluded);
            applyValues(target, values);
        } catch (PropertyAccessException e) {
            throw new PropertyCopyException("Error en copyNonNullProperties", e);
        }
    }

    // ===================== Helpers privados para reducir complejidad =====================
    private Map<String, Object> buildSourceValues(D source, Set<String> excluded)
            throws PropertyAccessException {
        try {
            var sourceInfo = Introspector.getBeanInfo(source.getClass(), Object.class);
            Map<String, Object> values = new HashMap<>();

            for (PropertyDescriptor pd : sourceInfo.getPropertyDescriptors()) {
                var read = pd.getReadMethod();
                if (read == null) continue;

                String name = pd.getName();
                if (excluded != null && excluded.contains(name)) continue;

                Object raw = read.invoke(source);
                normalizeValue(raw).ifPresent(v -> values.put(name, v));
            }
            return values;
        } catch (Exception e) {
            throw new PropertyAccessException("Error construyendo valores de origen", e);
        }
    }

    private void applyValues(D target, Map<String, Object> values)
            throws PropertyAccessException {
        try {
            var targetInfo = Introspector.getBeanInfo(target.getClass(), Object.class);

            for (PropertyDescriptor pd : targetInfo.getPropertyDescriptors()) {
                var write = pd.getWriteMethod();
                if (write == null) continue;

                Object val = values.get(pd.getName());
                if (val != null) {
                    write.invoke(target, val);
                }
            }
        } catch (Exception e) {
            throw new PropertyAccessException("Error aplicando valores al destino", e);
        }
    }

    /**
     * Normaliza un valor de entrada aplicando reglas de “presencia”.
     * Devuelve Optional.empty() si NO debe copiarse.
     */
    private Optional<Object> normalizeValue(Object raw) {
        if (raw == null) return Optional.empty();

        Object val = unwrapOptional(raw);
        if (val == null) return Optional.empty();

        if (val instanceof CharSequence cs) {
            String trimmed = cs.toString().trim();
            return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
        }
        if (val instanceof Collection<?> c) {
            return c.isEmpty() ? Optional.empty() : Optional.of(c);
        }
        if (val instanceof Map<?, ?> m) {
            return m.isEmpty() ? Optional.empty() : Optional.of(m);
        }
        return Optional.of(val);
    }

    /** Desenvuelve Optional si aplica, respetando Optional.empty() como “no copiar”. */
    private Object unwrapOptional(Object raw) {
        if (raw instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        return raw;
    }

    // ===================== Excepciones dedicadas =====================

    /** Excepción checked para operaciones de introspección/reflexión. */
    public static class PropertyAccessException extends Exception {
        public PropertyAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Excepción unchecked para exponer fallos de copia al exterior del helper. */
    public static class PropertyCopyException extends RuntimeException {
        public PropertyCopyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
