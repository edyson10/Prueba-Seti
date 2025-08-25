package co.franquicias.api.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebHandler;

import java.net.URI;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

class ResponseEnvelopeFilterTest {

    WebTestClient client;

    @BeforeEach
    void setUp() {
        RouterFunction<ServerResponse> router = route()
                .GET("/ok", req ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("id","p1"))
                )
                .GET("/created", req ->
                        ServerResponse.created(URI.create("/r/1"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("id","p1"))
                )
                .GET("/already", req ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue("{\"status\":200,\"message\":\"OK\",\"data\":{\"x\":1}}")
                )
                .GET("/text", req ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_PLAIN)
                                .bodyValue("hello")
                )
                .GET("/bad", req ->
                        ServerResponse.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error","oops"))
                )
                .GET("/nocontent", req -> ServerResponse.noContent().build())
                .GET("/problem200", req ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .bodyValue(Map.of("type","about:blank","title","problem","status",200))
                )
                .GET("/skip", req ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("raw", true))
                )
                .build();

        WebHandler webHandler = org.springframework.web.reactive.function.server.RouterFunctions.toWebHandler(router);

        this.client = WebTestClient
                .bindToWebHandler(webHandler)
                .webFilter(new ResponseEnvelopeFilter())
                .configureClient()
                .build();
    }

    @Test
    @DisplayName("200 + application/json => envuelve con {status,message,data}")
    void ok_shouldWrapJson() {
        client.get().uri("/ok")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("OK")
                .jsonPath("$.data.id").isEqualTo("p1");
    }

    @Test
    @DisplayName("201 + application/json => envuelve con message='Created'")
    void created_shouldWrapJson() {
        client.get().uri("/created")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("Created")
                .jsonPath("$.data.id").isEqualTo("p1");
    }

    @Test
    @DisplayName("Si ya está en {status,...,data:...} NO re-envuelve")
    void alreadyEnveloped_passThrough() {
        client.get().uri("/already")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                // Se mantiene el top-level status y data, sin doble anidamiento
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.x").isEqualTo(1)
                .jsonPath("$.data.status").doesNotExist();
    }

    @Test
    @DisplayName("Si content-type no es JSON (text/plain), NO envuelve")
    void nonJson_passThrough() {
        client.get().uri("/text")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class)
                .isEqualTo("hello");
    }

    @Test
    @DisplayName("Si status no es 2xx (400), NO envuelve")
    void error4xx_passThrough() {
        client.get().uri("/bad")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.error").isEqualTo("oops")
                .jsonPath("$.status").doesNotExist(); // no es el envelope del filtro
    }

    @Test
    @DisplayName("204 No Content no escribe body → filtro no actúa")
    void noContent_passThrough() {
        client.get().uri("/nocontent")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    @DisplayName("application/problem+json + 200 => también envuelve")
    void problemJson_200_shouldWrap() {
        client.get().uri("/problem200")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("OK")
                .jsonPath("$.data.title").isEqualTo("problem");
    }

    @Test
    @DisplayName("Header X-Envelope-Skip:true => NO envuelve")
    void headerSkip_shouldNotWrap() {
        client.get().uri("/skip")
                .header("X-Envelope-Skip", "true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.raw").isEqualTo(true)
                .jsonPath("$.status").doesNotExist();
    }

    @Test
    @DisplayName("Header X-Envelope-Disable:true => NO envuelve")
    void headerDisable_shouldNotWrap() {
        client.get().uri("/skip")
                .header("X-Envelope-Disable", "true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.raw").isEqualTo(true)
                .jsonPath("$.status").doesNotExist();
    }
}
