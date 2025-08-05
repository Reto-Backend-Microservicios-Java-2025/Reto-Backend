#!/bin/bash

# Script de configuración inicial para admin-service
echo "🔧 Configurando proyecto admin-service para deployment en Azure..."

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
COPY target/admin-service-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto 8080
EXPOSE 8080

# Configurar variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
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

# Hacer ejecutable el script de deploy
if [ -f "deploy-admin-service-script.sh" ]; then
    chmod +x deploy-admin-service-script.sh
    echo "✅ deploy-admin-service-script.sh es ejecutable"
fi

echo ""
echo "🎉 Configuración completada. Ahora puedes:"
echo "   1. Ejecutar: ./deploy-admin-service-script.sh (para deployment manual)"
echo "   2. Hacer push a GitHub (si configuraste GitHub Actions)"
echo ""
echo "📋 Archivos creados/configurados:"
echo "   - Dockerfile"
echo "   - .dockerignore"
echo "   - deploy-admin-service-script.sh (ejecutable)"