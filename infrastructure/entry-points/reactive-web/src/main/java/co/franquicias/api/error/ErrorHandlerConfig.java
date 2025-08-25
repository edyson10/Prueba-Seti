package co.franquicias.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

@Configuration
public class ErrorHandlerConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler globalErrorHandler(ObjectMapper objectMapper) {
        return new GlobalErrorHandler(objectMapper);
    }
}
