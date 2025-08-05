#!/bin/bash

# Script de configuración inicial para gateway-service
echo "🔧 Configurando proyecto gateway-service para deployment en Azure..."

# Crear el Dockerfile si no existe
if [ ! -f "Dockerfile" ]; then
    echo "📝 Creando Dockerfile..."
    cat > Dockerfile << 'EOF'
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
EOF
    echo "✅ Dockerfile creado"
else
    echo "✅ Dockerfile ya existe"
fi

# Crear .dockerignore si no existe
if [ ! -f ".dockerignore" ]; then
    echo "📝 Creando .dockerignore..."
    cat > .dockerignore << 'EOF'
.git
.gitignore
README.md
Dockerfile
.dockerignore
maven-wrapper.jar
.mvn/
src/
*.iml
.idea/
target/classes/
target/test-classes/
target/maven-archiver/
target/maven-status/
EOF
    echo "✅ .dockerignore creado"
else
    echo "✅ .dockerignore ya existe"
fi

# Crear application-azure.yml si no existe
if [ ! -f "src/main/resources/application-azure.yml" ]; then
    echo "📝 Creando application-azure.yml..."
    mkdir -p src/main/resources/
    cat > src/main/resources/application-azure.yml << 'EOF'
spring:
  application:
    name: gateway-service
  boot:
    admin:
      client:
        url: https://admin-service-app.azurewebsites.net
  main:
    web-application-type: reactive
  cloud:
    gateway:
      server:
        webflux:
          global-cors:
            cors-configurations:
              '[/**]':
                allowedOriginPatterns:
                  - "https://*.azurewebsites.net"
                  - "http://localhost:*"
                  - "https://localhost:*"
                allowedOrigins:
                  - "https://admin-service-app.azurewebsites.net"
                  - "https://registry-service-app.azurewebsites.net"
                  - "https://product-service-app-1754370901.azurewebsites.net"
                  - "https://gateway-service-app.azurewebsites.net"
                allowedMethods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - OPTIONS
                  - PATCH
                allowedHeaders: "*"
                allowCredentials: true
                maxAge: 3600
          routes:
            - id: product-service
              uri: lb://product-service
              predicates:
                - Path=/product-service/**
              filters:
                - StripPrefix=1
            - id: customer-service
              uri: lb://customer-service
              predicates:
                - Path=/customer-service/**
              filters:
                - StripPrefix=1
            - id: iam-service
              uri: lb://iam-service
              predicates:
                - Path=/iam-service/**
              filters:
                - StripPrefix=1
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true

server:
  port: 8010
  forward-headers-strategy: native

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: "ALWAYS"

eureka:
  client:
    service-url:
      defaultZone: https://registry-service-app.azurewebsites.net/
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: false
    hostname: ${spring.application.name}

logging:
  level:
    org.springframework.security: INFO
    org.springframework.security.web: INFO
    org.springframework.cloud.gateway: INFO
    reactor.netty: INFO

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    config-url: /v3/api-docs/swagger-config
    urls:
      - name: Product Service
        url: /product-service/v3/api-docs
      - name: Customer Service
        url: /customer-service/v3/api-docs
      - name: IAM Service
        url: /iam-service/v3/api-docs
EOF
    echo "✅ application-azure.yml creado"
else
    echo "✅ application-azure.yml ya existe"
fi

# Hacer ejecutable el script de deploy
if [ -f "deploy-gateway-service-script.sh" ]; then
    chmod +x deploy-gateway-service-script.sh
    echo "✅ deploy-gateway-service-script.sh es ejecutable"
fi

echo ""
echo "🎉 Configuración completada. Ahora puedes:"
echo "   1. Ejecutar: ./deploy-gateway-service-script.sh (para deployment manual)"
echo "   2. Hacer push a GitHub (si configuraste GitHub Actions)"
echo ""
echo "📋 Archivos creados/configurados:"
echo "   - Dockerfile"
echo "   - .dockerignore"
echo "   - application-azure.yml"
echo "   - deploy-gateway-service-script.sh (ejecutable)"
echo ""
echo "⚠️  IMPORTANTE: Antes del deployment, asegúrate de:"
echo "   1. Tener registry-service y admin-service funcionando en Azure"
echo "   2. Verificar que product-service esté registrado en Eureka"
echo "   3. Actualizar las URLs de CORS si cambias nombres de servicios"