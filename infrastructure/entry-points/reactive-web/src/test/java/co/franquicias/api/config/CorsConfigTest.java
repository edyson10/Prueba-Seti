package co.franquicias.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

class CorsConfigTest {

    WebTestClient clientAllowed;
    WebTestClient clientOnlyCors;

    @BeforeEach
    void setUp() {
        CorsWebFilter corsFilter = new CorsConfig()
                .corsWebFilter("http://localhost:3000,http://foo.com");

        RouterFunction<ServerResponse> router = route()
                .GET("/test", req -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("ok", true)))
                .build();

        clientOnlyCors = WebTestClient.bindToRouterFunction(router)
                .webFilter(corsFilter)
                .build();

        clientAllowed = clientOnlyCors;
    }

    @Test
    @DisplayName("Preflight OPTIONS rechazado: método PUT no está en allowedMethods")
    void cors_preflight_rejected_method() {
        clientOnlyCors.method(HttpMethod.OPTIONS).uri("/test")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "PUT") // no permitido en la config
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Preflight OPTIONS rechazado: Origin NO permitido")
    void cors_preflight_rejected_origin() {
        clientOnlyCors.method(HttpMethod.OPTIONS).uri("/test")
                .header("Origin", "http://evil.com") // no está en la lista
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isForbidden();
    }
}
