# Usar imagen base de Java 17
FROM openjdk:17-jdk-slim

# Instalar curl para health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Crear directorio de trabajo
WORKDIR /app

# Copiar el JAR al contenedor
COPY target/registry-service-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto 8090
EXPOSE 8090

# Configurar variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PORT=8090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
