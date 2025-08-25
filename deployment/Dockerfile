FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app && \
    mkdir -p /cache /tmp && chown -R app:app /app /cache /tmp

# Ajusta la ruta si es multi-módulo (p.ej. app-service/build/libs/*.jar)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
USER app
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]

