# Usar imagen base de Java 17
FROM openjdk:17-jdk-slim

# Instalar curl para health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Crear directorio de trabajo
WORKDIR /app

# Copiar el JAR al contenedor
COPY target/gateway-service-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto 8010
EXPOSE 8010

# Configurar variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PORT=8010

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8010/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -Dspring.profiles.active=azure -jar app.jar"]
