package co.franquicias.mongodb.config;

import com.mongodb.reactivestreams.client.MongoClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ConectionConfig.class)
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/?serverSelectionTimeoutMS=5&connectTimeoutMS=5",
        "spring.data.mongodb.database=testdb"
})
class ConectionConfigSpringTest {

    @Resource ApplicationContext ctx;
    @Resource ReactiveMongoTemplate reactiveMongoTemplate;
    @Resource MongoClient mongoClient;

    @Test
    @DisplayName("Crea beans de MongoClient y ReactiveMongoTemplate")
    void beansExist() {
        assertNotNull(ctx);
        assertNotNull(mongoClient);
        assertNotNull(reactiveMongoTemplate);
    }

    @Test
    @DisplayName("ReactiveMongoTemplate usa el databaseName configurado")
    void templateHasConfiguredDatabaseName() throws Exception {
        String dbName = extractDatabaseName(reactiveMongoTemplate);
        assertEquals("testdb", dbName);
    }

    @Test
    @DisplayName("ReactiveMongoTemplate usa el mismo MongoClient singleton del contexto")
    void templateUsesSameMongoClientSingleton() throws Exception {
        MongoClient clientFromFactory = extractMongoClient(reactiveMongoTemplate);
        assertSame(mongoClient, clientFromFactory,
                "El MongoClient dentro del template debe ser el bean singleton del contexto");
    }

    // ====================== Helpers robustos ======================

    private static Object getFieldByNameOrType(Object target, String[] candidateNames, Class<?> assignableTo) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            for (String name : candidateNames) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) { /* try next */ }
            }
            if (assignableTo != null) {
                for (Field f : c.getDeclaredFields()) {
                    if (assignableTo.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        return f.get(target);
                    }
                }
            }
            c = c.getSuperclass();
        }
        throw new NoSuchFieldException("No se halló ninguno de " + Arrays.toString(candidateNames) +
                " ni un campo del tipo " + (assignableTo == null ? "<null>" : assignableTo.getName()));
    }

    private static Object invokeGetterIfPresent(Object target, String... methodNames) {
        for (String m : methodNames) {
            try {
                Method mm = target.getClass().getMethod(m);
                mm.setAccessible(true);
                return mm.invoke(target);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static Object getFieldIfPresent(Object target, String... fieldNames) {
        Class<?> c = target.getClass();
        while (c != null) {
            for (String name : fieldNames) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) { }
                catch (Exception e) { throw new RuntimeException(e); }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static String extractDatabaseName(ReactiveMongoTemplate tmpl) throws Exception {
        Object factory = getFieldByNameOrType(
                tmpl,
                new String[]{"databaseFactory", "mongoDatabaseFactory"},
                ReactiveMongoDatabaseFactory.class
        );

        Object viaMethod = invokeGetterIfPresent(factory, "getMongoDatabaseName", "getDatabaseName");
        if (viaMethod instanceof String s) {
            return s;
        }
        Object viaField = getFieldIfPresent(factory, "databaseName", "mongoDatabaseName");
        if (viaField instanceof String s) {
            return s;
        }
        fail("No se pudo extraer el nombre de base de datos desde la factory");
        return null;
    }

    private static MongoClient extractMongoClient(ReactiveMongoTemplate tmpl) throws Exception {
        Object factory = getFieldByNameOrType(
                tmpl,
                new String[]{"databaseFactory", "mongoDatabaseFactory"},
                ReactiveMongoDatabaseFactory.class
        );

        Class<?> c = factory.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (MongoClient.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (MongoClient) f.get(factory);
                }
            }
            c = c.getSuperclass();
        }
        fail("No se encontró un MongoClient dentro de la factory");
        return null;
    }
}
