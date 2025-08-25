package co.franquicias.config;

import co.franquicias.model.OperacionesFranquiciaPort;
import co.franquicias.usecase.franquicia.FranquiciaUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UseCasesConfigTest {

    @Test
    void componentScan_annotation_is_correct() {
        ComponentScan scan = UseCasesConfig.class.getAnnotation(ComponentScan.class);
        assertNotNull(scan, "@ComponentScan no está presente");

        assertArrayEquals(new String[]{"co.franquicias.usecase"}, scan.basePackages(),
                "basePackages incorrectos en @ComponentScan");

        assertFalse(scan.useDefaultFilters(), "useDefaultFilters debería ser false");

        assertEquals(1, scan.includeFilters().length, "Debe haber un includeFilter");
        ComponentScan.Filter f = scan.includeFilters()[0];
        assertEquals(FilterType.REGEX, f.type(), "El filtro debe ser de tipo REGEX");
        assertArrayEquals(new String[]{"^.+UseCase$"}, f.pattern(),
                "El patrón REGEX debe ser ^.+UseCase$");
    }

    @Test
    void context_registers_real_UseCase_when_dependencies_provided() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(UseCasesConfig.class, SupportConfig.class);
            ctx.refresh();

            FranquiciaUseCase useCase = ctx.getBean(FranquiciaUseCase.class);
            assertNotNull(useCase, "No se registró FranquiciaUseCase mediante el escaneo");

            boolean anyUseCase =
                    Arrays.stream(ctx.getBeanDefinitionNames())
                            .map(ctx::getType)
                            .filter(t -> t != null)
                            .anyMatch(t -> t.getSimpleName().endsWith("UseCase"));
            assertTrue(anyUseCase, "No se encontró ningún bean cuyo tipo termine en 'UseCase'");
        }
    }

    @Configuration
    static class SupportConfig {
        @Bean
        OperacionesFranquiciaPort operacionesFranquiciaPort() {
            return Mockito.mock(OperacionesFranquiciaPort.class);
        }
    }
}
