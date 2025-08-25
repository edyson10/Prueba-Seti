package co.franquicias.mongodb.config;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperConfig.class)
class MapperConfigTest {

    @Resource
    ApplicationContext ctx;

    @Resource
    ModelMapper modelMapper;

    public static class Src {
        private String nombre;
        private Integer edad;
        private String nota;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Integer getEdad() { return edad; }
        public void setEdad(Integer edad) { this.edad = edad; }
        public String getNota() { return nota; }
        public void setNota(String nota) { this.nota = nota; }
    }
    public static class Dst {
        private String nombre;
        private Integer edad;
        private String nota;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Integer getEdad() { return edad; }
        public void setEdad(Integer edad) { this.edad = edad; }
        public String getNota() { return nota; }
        public void setNota(String nota) { this.nota = nota; }
    }

    @Test
    @DisplayName("Se registra el bean ModelMapper en el contexto")
    void beanExiste() {
        assertNotNull(ctx);
        assertNotNull(modelMapper);
        assertSame(modelMapper, ctx.getBean(ModelMapper.class));
    }

    @Test
    @DisplayName("El ModelMapper está configurado con skipNullEnabled(true)")
    void skipNullEnabled() {
        assertTrue(modelMapper.getConfiguration().isSkipNullEnabled(),
                "skipNullEnabled debería estar en true");
    }

    @Test
    @DisplayName("Mapeo sobre objeto existente: no sobrescribe con null cuando skipNullEnabled=true")
    void noSobrescribeConNull() {
        Dst dst = new Dst();
        dst.setNombre("YA_EXISTE");
        dst.setEdad(20);
        dst.setNota("nota inicial");

        Src src = new Src();
        src.setNombre("NUEVO_NOMBRE");
        src.setEdad(null);
        src.setNota(null);

        modelMapper.map(src, dst);

        assertEquals("NUEVO_NOMBRE", dst.getNombre());
        assertEquals(20, dst.getEdad());
        assertEquals("nota inicial", dst.getNota());
    }
}
