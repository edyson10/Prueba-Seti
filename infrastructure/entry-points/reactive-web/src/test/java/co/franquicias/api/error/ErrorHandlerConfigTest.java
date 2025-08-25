package co.franquicias.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerConfigTest {

    private AnnotationConfigApplicationContext ctx;

    @AfterEach
    void tearDown() {
        if (ctx != null) ctx.close();
    }

    @Test
    void createsGlobalErrorHandlerBean_and_usesSameObjectMapper() throws Exception {
        ctx = new AnnotationConfigApplicationContext(TestConfig.class, ErrorHandlerConfig.class);

        ErrorWebExceptionHandler handler = ctx.getBean(ErrorWebExceptionHandler.class);
        assertNotNull(handler, "Debe existir un ErrorWebExceptionHandler en el contexto");
        assertTrue(handler instanceof GlobalErrorHandler, "El handler debe ser GlobalErrorHandler");

        ObjectMapper omCtx = ctx.getBean(ObjectMapper.class);
        ObjectMapper omInHandler = (ObjectMapper) findFieldByType(handler, ObjectMapper.class);
        assertNotNull(omInHandler, "No se pudo extraer el ObjectMapper del GlobalErrorHandler (revisa el campo interno)");
        assertSame(omCtx, omInHandler, "El ObjectMapper del handler debe ser el mismo bean del contexto");
    }

    @Test
    void orderIsHighestPrecedence_whenAnotherHandlerExists() {
        ctx = new AnnotationConfigApplicationContext(TestConfigWithAnotherHandler.class, ErrorHandlerConfig.class);

        Map<String, ErrorWebExceptionHandler> beans = ctx.getBeansOfType(ErrorWebExceptionHandler.class);
        assertTrue(beans.size() >= 2, "Debe haber al menos dos handlers para probar el orden");

        List<ErrorWebExceptionHandler> list = new ArrayList<>(beans.values());
        AnnotationAwareOrderComparator.sort(list);

        assertTrue(list.get(0) instanceof GlobalErrorHandler,
                "GlobalErrorHandler debe tener la precedencia más alta");
    }

    /** Busca por reflexión el primer campo del tipo dado y devuelve su valor. */
    private static Object findFieldByType(Object target, Class<?> type) throws IllegalAccessException {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f.get(target);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    @Configuration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Configuration
    static class TestConfigWithAnotherHandler {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /** Handler “de relleno” con muy baja prioridad. */
        @Bean
        @org.springframework.core.annotation.Order(Ordered.LOWEST_PRECEDENCE)
        ErrorWebExceptionHandler fallbackHandler() {
            return (exchange, ex) -> Mono.error(ex);
        }
    }
}
