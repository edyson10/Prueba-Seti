# Dockerfile
FROM eclipse-temurin:21-jre-alpine

# Directorio de trabajo
WORKDIR /app

# Crear usuario/grupo no root
RUN addgroup -S app && adduser -S app -G app

# Directorios útiles y permisos
RUN mkdir -p /app /cache /tmp && chown -R app:app /app /cache /tmp

# Copia el JAR (ajusta el path si tu jar no está en la raíz)
# Por ejemplo, si usas el scaffold de Bancolombia: app-service/build/libs/app-service-*.jar
# COPY app-service/build/libs/app-service-*.jar app.jar
# COPY *.jar app.jar
# copia el jar construido fuera del contenedor
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Ajustes JVM seguros en contenedores
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Puerto (informativo)
EXPOSE 8080

# Cambia a usuario no root
USER app

# Importante para Railway: respetar $PORT si viene seteado
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
