package co.franquicias.api.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.WebHandler;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

class GlobalErrorHandlerTest {

    private WebTestClient client;

    private static WebTestClient buildClient(ObjectMapper mapper) {
        RouterFunction<ServerResponse> router = route()
                .GET("/e/iae", r -> { throw new IllegalArgumentException("param malo"); })
                .GET("/e/input", r -> { throw new ServerWebInputException("payload inválido"); })
                .GET("/e/rse404", r -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no se encontró"); })
                .GET("/e/state404", r -> { throw new IllegalStateException("La sucursal no existe"); })
                .GET("/e/no-such", r -> { throw new NoSuchElementException("missing"); })
                .GET("/e/dup", r -> { throw new DuplicateKeyException("duplicado"); })
                .GET("/e/div", r -> { throw new DataIntegrityViolationException("violación"); })
                .GET("/e/unsupported", r -> { throw new UnsupportedMediaTypeStatusException("tipo no soportado"); })
                .GET("/e/boom", r -> { throw new RuntimeException("boom"); })
                .GET("/e/method", r -> ServerResponse.ok().build())
                .build();

        WebHandler webHandler = org.springframework.web.reactive.function.server.RouterFunctions.toWebHandler(router);

        var errorHandler = new GlobalErrorHandler(mapper);
        WebHandler withErrors = new ExceptionHandlingWebHandler(webHandler, List.of(errorHandler));

        return WebTestClient.bindToWebHandler(withErrors).build();
    }

    @BeforeEach
    void setUp() {
        client = buildClient(new ObjectMapper());
    }

    @Test
    @DisplayName("IllegalArgumentException => 400 + mensaje original")
    void illegalArgument_is400() {
        client.get().uri("/e/iae")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("param malo");
    }

    @Test
    @DisplayName("ServerWebInputException => 400 + 'Solicitud inválida'")
    void serverWebInput_is400() {
        client.get().uri("/e/input")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Solicitud inválida");
    }

    @Test
    @DisplayName("ResponseStatusException 404 => 404 + mensaje")
    void rse_is404() {
        client.get().uri("/e/rse404")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("404 NOT_FOUND \"no se encontró\"");
    }

    @Test
    @DisplayName("IllegalStateException con 'no existe' => 404")
    void illegalState_withNoExiste_is404() {
        client.get().uri("/e/state404")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("La sucursal no existe");
    }

    @Test
    @DisplayName("NoSuchElementException => 404")
    void noSuchElement_is404() {
        client.get().uri("/e/no-such")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("missing");
    }

    @Test
    @DisplayName("DuplicateKeyException => 409")
    void duplicateKey_is409() {
        client.get().uri("/e/dup")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.message").isEqualTo("duplicado");
    }

    @Test
    @DisplayName("DataIntegrityViolationException => 409")
    void dataIntegrity_is409() {
        client.get().uri("/e/div")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.message").isEqualTo("violación");
    }

    @Test
    @DisplayName("Excepción no mapeada => 500 + mensaje original")
    void unknown_is500() {
        client.get().uri("/e/boom")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.message").isEqualTo("boom");
    }

    @Test
    @DisplayName("Si el ObjectMapper falla, usa JSON de fallback con data=null")
    void fallbackJson_whenMapperFails() {
        ObjectMapper failing = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("forced") {};
            }
        };
        WebTestClient local = buildClient(failing);

        local.get().uri("/e/iae")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("param malo")
                .jsonPath("$.data").value(nullValue());
    }
}
