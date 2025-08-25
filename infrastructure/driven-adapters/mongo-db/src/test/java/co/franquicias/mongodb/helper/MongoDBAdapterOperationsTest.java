package co.franquicias.mongodb.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.mongodb.client.result.UpdateResult;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoDBAdapterOperationsTest {

    // ===== Tipos de prueba =====
    static class TestEntity {
        private String id;
        private String name;
        private Integer age;
        private Optional<String> nickname;
        private List<String> tags;
        private Map<String, String> meta;
        private Long version;
        private Instant createdAt;

        public TestEntity() {}
        public TestEntity(String id, String name, Integer age, Optional<String> nickname,
                          List<String> tags, Map<String, String> meta, Long version, Instant createdAt) {
            this.id = id; this.name = name; this.age = age; this.nickname = nickname;
            this.tags = tags; this.meta = meta; this.version = version; this.createdAt = createdAt;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Optional<String> getNickname() { return nickname; }
        public void setNickname(Optional<String> nickname) { this.nickname = nickname; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Map<String, String> getMeta() { return meta; }
        public void setMeta(Map<String, String> meta) { this.meta = meta; }
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    static class TestDocument {
        private String id;
        private String name;
        private Integer age;
        private Optional<String> nickname;
        private List<String> tags;
        private Map<String, String> meta;
        private Long version;
        private Instant createdAt;

        public TestDocument() {}
        public TestDocument(String id, String name, Integer age, Optional<String> nickname,
                            List<String> tags, Map<String, String> meta, Long version, Instant createdAt) {
            this.id = id; this.name = name; this.age = age; this.nickname = nickname;
            this.tags = tags; this.meta = meta; this.version = version; this.createdAt = createdAt;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Optional<String> getNickname() { return nickname; }
        public void setNickname(Optional<String> nickname) { this.nickname = nickname; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Map<String, String> getMeta() { return meta; }
        public void setMeta(Map<String, String> meta) { this.meta = meta; }
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    /** Subclase concreta para exponer protegidos. */
    static class TestAdapter extends MongoDBAdapterOperations<TestEntity, TestDocument, String> {
        protected TestAdapter(ReactiveMongoRepository<TestDocument, String> repository,
                              ReactiveMongoTemplate mongoTemplate,
                              ModelMapper modelMapper) {
            super(repository, mongoTemplate, TestDocument.class, TestEntity.class, modelMapper);
        }
        // Exponer protegidos para test
        Flux<TestEntity> _findByQuery(Query q) { return super.findByQuery(q); }
        Mono<TestEntity> _findOneByQuery(Query q) { return super.findOneByQuery(q); }
        Mono<TestEntity> _findAndModify(Query q, Update u) { return super.findAndModifyReturningEntity(q, u); }
        Mono<Boolean> _updateFirst(Query q, Update u) { return super.updateFirstMatched(q, u); }

        // 🔧 AQUÍ EL CAMBIO: usar TestDocument (tipo concreto), no D
        void _copy(TestDocument src, TestDocument tgt) { super.copyNonNullProperties(src, tgt); }
        void _copy(TestDocument src, TestDocument tgt, Set<String> excluded) { super.copyNonNullProperties(src, tgt, excluded); }
    }

    // ===== Mocks & SUT =====
    @Mock ReactiveMongoRepository<TestDocument, String> repository;
    @Mock ReactiveMongoTemplate mongoTemplate;

    ModelMapper modelMapper;
    TestAdapter adapter;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        adapter = new TestAdapter(repository, mongoTemplate, modelMapper);
    }

    // ===== Helpers =====
    private TestEntity entity(String id, String name, Integer age, Optional<String> nick,
                              List<String> tags, Map<String, String> meta, Long ver, Instant created) {
        return new TestEntity(id, name, age, nick, tags, meta, ver, created);
    }
    private TestDocument doc(String id, String name, Integer age, Optional<String> nick,
                             List<String> tags, Map<String, String> meta, Long ver, Instant created) {
        return new TestDocument(id, name, age, nick, tags, meta, ver, created);
    }

    // ===== TESTS =====

    @Test
    @DisplayName("Mapeo E->D y D->E")
    void mapping() {
        Instant t0 = Instant.parse("2024-01-01T00:00:00Z");
        TestEntity e = entity("1","Alice",25, Optional.of("ali"), List.of("a"), Map.of("k","v"), 3L, t0);

        TestDocument d = adapter.toDocument(e);
        TestEntity e2 = adapter.toEntity(d);

        org.junit.jupiter.api.Assertions.assertEquals("1", d.getId());
        org.junit.jupiter.api.Assertions.assertEquals("Alice", d.getName());
        org.junit.jupiter.api.Assertions.assertEquals(Optional.of("ali"), d.getNickname());
        org.junit.jupiter.api.Assertions.assertEquals(t0, d.getCreatedAt());

        org.junit.jupiter.api.Assertions.assertEquals("1", e2.getId());
        org.junit.jupiter.api.Assertions.assertEquals("Alice", e2.getName());
        org.junit.jupiter.api.Assertions.assertEquals(t0, e2.getCreatedAt());
    }

    @Test
    @DisplayName("mapList, mapMonoDocToEntity, mapFluxDocToEntity")
    void mappingHelpers() {
        Instant now = Instant.now();
        List<TestDocument> docs = List.of(
                doc("1","A",10, Optional.empty(), List.of(), Map.of(), 1L, now),
                doc("2","B",20, Optional.of("b"), List.of("x"), Map.of("k","v"), 2L, now)
        );

        var asEntities = adapter.mapList(docs, TestEntity.class);
        org.junit.jupiter.api.Assertions.assertEquals(2, asEntities.size());

        StepVerifier.create(adapter.mapMonoDocToEntity(Mono.just(docs.get(0))))
                .expectNextMatches(e -> e.getName().equals("A") && e.getAge()==10)
                .verifyComplete();

        StepVerifier.create(adapter.mapFluxDocToEntity(Flux.fromIterable(docs)))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Nested
    class CrudOps {
        @Test
        @DisplayName("save() convierte E->D, repository.save y regresa E")
        void save() {
            Instant t0 = Instant.now();
            TestEntity e = entity("1","Bob",30, Optional.of("b"), List.of("t"), Map.of(), 1L, t0);
            TestDocument saved = doc("1","Bob",30, Optional.of("b"), List.of("t"), Map.of(), 1L, t0);

            when(repository.save(any(TestDocument.class))).thenReturn(Mono.just(saved));

            StepVerifier.create(adapter.save(e))
                    .expectNextMatches(out -> out.getId().equals("1") && out.getName().equals("Bob"))
                    .verifyComplete();

            ArgumentCaptor<TestDocument> cap = ArgumentCaptor.forClass(TestDocument.class);
            verify(repository).save(cap.capture());
            org.junit.jupiter.api.Assertions.assertEquals("Bob", cap.getValue().getName());
        }

        @Test
        @DisplayName("saveAll() mapea cada elemento")
        void saveAll() {
            Instant t0 = Instant.now();
            TestEntity e1 = entity("1","E1",10, Optional.empty(), List.of(), Map.of(), 1L, t0);
            TestEntity e2 = entity("2","E2",20, Optional.empty(), List.of(), Map.of(), 1L, t0);

            TestDocument d1 = doc("1","E1",10, Optional.empty(), List.of(), Map.of(), 1L, t0);
            TestDocument d2 = doc("2","E2",20, Optional.empty(), List.of(), Map.of(), 1L, t0);

            when(repository.saveAll(any(Flux.class))).thenReturn(Flux.just(d1, d2));

            StepVerifier.create(adapter.saveAll(Flux.just(e1, e2)))
                    .expectNextMatches(x -> x.getId().equals("1"))
                    .expectNextMatches(x -> x.getId().equals("2"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("findById/findAll/deleteById")
        void findAndDelete() {
            Instant t0 = Instant.now();
            TestDocument d = doc("1","Zed",40, Optional.empty(), List.of(), Map.of(), 1L, t0);

            when(repository.findById("1")).thenReturn(Mono.just(d));
            when(repository.findAll()).thenReturn(Flux.just(d));
            when(repository.deleteById("1")).thenReturn(Mono.empty());

            StepVerifier.create(adapter.findById("1"))
                    .expectNextMatches(e -> e.getName().equals("Zed"))
                    .verifyComplete();

            StepVerifier.create(adapter.findAll())
                    .expectNextCount(1)
                    .verifyComplete();

            StepVerifier.create(adapter.deleteById("1"))
                    .verifyComplete();
        }
    }

    @Nested
    class TemplateOps {
        @Test
        @DisplayName("findByQuery y findOneByQuery devuelven entidades mapeadas")
        void findByQueryAndFindOne() {
            Query q = new Query();
            Instant t0 = Instant.now();
            TestDocument d1 = doc("1","A",10, Optional.empty(), List.of(), Map.of(), 1L, t0);
            TestDocument d2 = doc("2","B",20, Optional.empty(), List.of(), Map.of(), 1L, t0);

            when(mongoTemplate.find(eq(q), eq(TestDocument.class))).thenReturn(Flux.just(d1, d2));
            when(mongoTemplate.findOne(eq(q), eq(TestDocument.class))).thenReturn(Mono.just(d2));

            StepVerifier.create(adapter._findByQuery(q))
                    .expectNextMatches(e -> e.getId().equals("1"))
                    .expectNextMatches(e -> e.getId().equals("2"))
                    .verifyComplete();

            StepVerifier.create(adapter._findOneByQuery(q))
                    .expectNextMatches(e -> e.getId().equals("2") && e.getName().equals("B"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("findAndModifyReturningEntity retorna entidad modificada")
        void findAndModifyReturningEntity() {
            Query q = new Query();
            Update u = new Update().set("name", "NEW");
            TestDocument modified = doc("1","NEW",33, Optional.of("n"), List.of(), Map.of(), 1L, Instant.now());

            when(mongoTemplate.findAndModify(
                    eq(q),
                    eq(u),
                    argThat((FindAndModifyOptions o) -> o.isReturnNew()), // <-- clave
                    eq(TestDocument.class)
            )).thenReturn(Mono.just(modified));

            StepVerifier.create(adapter._findAndModify(q, u))
                    .expectNextMatches(e -> e.getName().equals("NEW") && e.getAge() == 33)
                    .verifyComplete();
        }

        @Test
        @DisplayName("updateFirstMatched true cuando hay match; false cuando no")
        void updateFirstMatched() {
            Query q = new Query();
            Update u = new Update().set("age", 99);

            when(mongoTemplate.updateFirst(eq(q), eq(u), eq(TestDocument.class)))
                    .thenReturn(Mono.just(UpdateResult.acknowledged(1L, 1L, null)));

            StepVerifier.create(adapter._updateFirst(q, u))
                    .expectNext(true)
                    .verifyComplete();

            when(mongoTemplate.updateFirst(eq(q), eq(u), eq(TestDocument.class)))
                    .thenReturn(Mono.just(UpdateResult.acknowledged(0L, 0L, null)));

            StepVerifier.create(adapter._updateFirst(q, u))
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    @Nested
    class MergeAndCopy {
        @Test
        @DisplayName("mergeNonNullAndSave: copia sólo presentes, respeta exclusiones y trims")
        void mergeOk() {
            String id = "abc";
            Instant created = Instant.parse("2024-05-05T10:00:00Z");

            TestDocument existing = doc(
                    id, "Old Name", 30, Optional.of("oldnick"),
                    new ArrayList<>(List.of("a","b")),
                    new HashMap<>(Map.of("k","v")),
                    5L, created
            );

            TestEntity partial = entity(
                    "IGNORED", "  New Name  ", null, Optional.empty(),
                    new ArrayList<>(), new HashMap<>(), 99L, Instant.parse("2030-01-01T00:00:00Z")
            );

            when(repository.findById(id)).thenReturn(Mono.just(existing));
            when(repository.save(any(TestDocument.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(adapter.mergeNonNullAndSave(id, partial))
                    .expectNextMatches(updated ->
                            updated.getName().equals("New Name") &&
                                    updated.getAge().equals(30) &&
                                    updated.getNickname().equals(Optional.of("oldnick")) &&
                                    updated.getTags().equals(List.of("a","b")) &&
                                    updated.getMeta().equals(Map.of("k","v")) &&
                                    updated.getVersion().equals(5L) &&
                                    updated.getCreatedAt().equals(created)
                    )
                    .verifyComplete();

            ArgumentCaptor<TestDocument> cap = ArgumentCaptor.forClass(TestDocument.class);
            verify(repository).save(cap.capture());
            TestDocument saved = cap.getValue();
            org.junit.jupiter.api.Assertions.assertEquals("New Name", saved.getName());
            org.junit.jupiter.api.Assertions.assertEquals(30, saved.getAge());
            org.junit.jupiter.api.Assertions.assertEquals(Optional.of("oldnick"), saved.getNickname());
        }

        @Test
        @DisplayName("mergeNonNullAndSave: error si no existe")
        void mergeNotFound() {
            when(repository.findById("x")).thenReturn(Mono.empty());

            StepVerifier.create(adapter.mergeNonNullAndSave("x", new TestEntity()))
                    .expectErrorMatches(ex ->
                            ex instanceof IllegalArgumentException &&
                                    ex.getMessage().contains("Recurso no encontrado"))
                    .verify();
        }

        @Test
        @DisplayName("copyNonNullProperties directo: exclusiones + colecciones vacías + Optional.empty + trim")
        void copyDirect() {
            Instant t0 = Instant.parse("2024-01-01T00:00:00Z");
            TestDocument target = doc(
                    "id0", "keep", 18, Optional.of("nick"),
                    new ArrayList<>(List.of("t")), new HashMap<>(Map.of("x","y")),
                    1L, t0
            );
            TestDocument source = doc(
                    "idNEW", "  changed  ", null, Optional.empty(),
                    new ArrayList<>(), new HashMap<>(),
                    99L, Instant.parse("2030-01-01T00:00:00Z")
            );

            adapter._copy(source, target, Set.of("id","version","createdAt"));

            org.junit.jupiter.api.Assertions.assertEquals("id0", target.getId());
            org.junit.jupiter.api.Assertions.assertEquals(1L, target.getVersion());
            org.junit.jupiter.api.Assertions.assertEquals(t0, target.getCreatedAt());
            org.junit.jupiter.api.Assertions.assertEquals("changed", target.getName());
            org.junit.jupiter.api.Assertions.assertEquals(18, target.getAge());
            org.junit.jupiter.api.Assertions.assertEquals(Optional.of("nick"), target.getNickname());
            org.junit.jupiter.api.Assertions.assertEquals(List.of("t"), target.getTags());
            org.junit.jupiter.api.Assertions.assertEquals(Map.of("x","y"), target.getMeta());
        }
    }
}
