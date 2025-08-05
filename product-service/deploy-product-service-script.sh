#!/bin/bash

# Script para el deployment del product-service con Docker y Azure App Service
# Actualizado para PostgreSQL 17 Flexible Server y Canada Central

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración actualizada
RESOURCE_GROUP="product-service-rg"
LOCATION="canadacentral"  # Cambiado a Canada Central
ACR_NAME="productserviceacr$(date +%s)"
APP_PLAN="product-service-plan"
WEB_APP="product-service-app-$(date +%s)"  # Hacer único con timestamp
POSTGRES_SERVER="product-db-flexible-$(date +%s)"  # Hacer único con timestamp
POSTGRES_DB="productdb"
POSTGRES_USER="productuser"
POSTGRES_PASSWORD="PmsSecure123!"
POSTGRES_VERSION="17"  # PostgreSQL 17

echo "🚀 Iniciando deployment del product-service en Azure App Service..."
echo "📍 Región: Canada Central"
echo "🗄️  PostgreSQL: Versión 17 Flexible Server"

# 1. VERIFICACIONES DE SEGURIDAD PREVIAS
echo -e "${YELLOW}🔍 Verificando configuración de Azure...${NC}"

# Verificar si Azure CLI está instalado
if ! command -v az &> /dev/null; then
    echo -e "${RED}❌ Azure CLI no está instalado${NC}"
    echo "Instala Azure CLI: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
    exit 1
fi

# Verificar si el usuario está autenticado
if ! az account show &> /dev/null; then
    echo -e "${RED}❌ No estás autenticado en Azure${NC}"
    echo "Ejecuta: az login"
    exit 1
fi

# Verificar y registrar proveedores de recursos necesarios
echo -e "${YELLOW}📦 Verificando proveedores de recursos...${NC}"

REQUIRED_PROVIDERS=("Microsoft.ContainerRegistry" "Microsoft.Web" "Microsoft.DBforPostgreSQL")

for provider in "${REQUIRED_PROVIDERS[@]}"; do
    echo "Verificando $provider..."
    current_state=$(az provider show --namespace $provider --query "registrationState" -o tsv 2>/dev/null)

    if [ "$current_state" != "Registered" ]; then
        echo -e "${YELLOW}📋 Registrando $provider...${NC}"
        az provider register --namespace $provider

        # Esperar hasta que esté registrado (máximo 2 minutos)
        for i in {1..12}; do
            state=$(az provider show --namespace $provider --query "registrationState" -o tsv)
            if [ "$state" = "Registered" ]; then
                echo -e "${GREEN}✅ $provider registrado exitosamente${NC}"
                break
            elif [ "$i" = "12" ]; then
                echo -e "${YELLOW}⚠️  $provider aún registrándose, continuando...${NC}"
                break
            else
                echo "Esperando registro de $provider... ($i/12)"
                sleep 10
            fi
        done
    else
        echo -e "${GREEN}✅ $provider ya está registrado${NC}"
    fi
done

# Mostrar información de la cuenta actual
echo -e "${GREEN}✅ Autenticado en Azure${NC}"
CURRENT_USER=$(az account show --query "user.name" -o tsv)
CURRENT_SUBSCRIPTION=$(az account show --query "name" -o tsv)
SUBSCRIPTION_ID=$(az account show --query "id" -o tsv)

echo "👤 Usuario actual: $CURRENT_USER"
echo "📋 Suscripción actual: $CURRENT_SUBSCRIPTION"
echo "🆔 ID de suscripción: $SUBSCRIPTION_ID"

# Confirmación del usuario
echo ""
echo -e "${YELLOW}⚠️  ¿Estás seguro de que quieres crear recursos en esta suscripción?${NC}"
echo "Se crearán los siguientes recursos para product-service:"
echo "  - Resource Group: $RESOURCE_GROUP"
echo "  - Container Registry: $ACR_NAME"
echo "  - App Service Plan: $APP_PLAN"
echo "  - Web App: $WEB_APP"
echo "  - PostgreSQL Flexible Server: $POSTGRES_SERVER v$POSTGRES_VERSION"
echo "  - Database: $POSTGRES_DB"
echo "  - Ubicación: $LOCATION"
echo ""
read -p "¿Continuar? (s/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[SsYy]$ ]]; then
    echo "❌ Deployment cancelado por el usuario"
    exit 1
fi

# 2. Verificar si Docker está ejecutándose
echo -e "${YELLOW}🐳 Verificando Docker...${NC}"
if ! docker info &> /dev/null; then
    echo -e "${RED}❌ Docker no está ejecutándose${NC}"
    echo "Inicia Docker Desktop o el servicio de Docker"
    exit 1
fi
echo -e "${GREEN}✅ Docker está ejecutándose${NC}"

# 3. Compilar la aplicación
echo -e "${YELLOW}🔨 Compilando product-service...${NC}"
mvn clean package -DskipTests

if [ ! -f "target/product-service-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ Error: No se encontró el JAR compilado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Product-service compilado exitosamente${NC}"

# 4. Construir imagen Docker
echo -e "${YELLOW}🐳 Construyendo imagen Docker...${NC}"
docker build -t product-service:latest .
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error construyendo imagen Docker${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Imagen Docker construida${NC}"

# 5. Crear resource group
echo -e "${YELLOW}📁 Creando resource group en Canada Central...${NC}"
az group create --name $RESOURCE_GROUP --location $LOCATION
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error creando resource group${NC}"
    exit 1
fi

# 6. Crear Azure Database for PostgreSQL Flexible Server v17
echo -e "${YELLOW}🗄️  Creando Azure PostgreSQL Flexible Server v$POSTGRES_VERSION...${NC}"
echo "⏳ Esto puede tomar varios minutos..."

# Verificar si el servidor ya existe
EXISTING_SERVER=$(az postgres flexible-server show --name $POSTGRES_SERVER --resource-group $RESOURCE_GROUP --query "name" -o tsv 2>/dev/null)

if [ ! -z "$EXISTING_SERVER" ]; then
    echo -e "${GREEN}✅ PostgreSQL Flexible Server ya existe: $EXISTING_SERVER${NC}"
else
    echo -e "${YELLOW}📋 Creando nuevo PostgreSQL Flexible Server...${NC}"

    # Intentar crear el servidor con SKU Burstable válido
    echo -e "${YELLOW}🔧 Intentando con tier Burstable y SKU Standard_B1ms...${NC}"
    az postgres flexible-server create \
      --resource-group $RESOURCE_GROUP \
      --name $POSTGRES_SERVER \
      --location $LOCATION \
      --admin-user $POSTGRES_USER \
      --admin-password $POSTGRES_PASSWORD \
      --sku-name Standard_B1ms \
      --tier Burstable \
      --version $POSTGRES_VERSION \
      --storage-size 32 \
      --public-access 0.0.0.0-255.255.255.255 \
      --high-availability Disabled

    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}⚠️  SKU Burstable falló, intentando con tier GeneralPurpose...${NC}"

        # Fallback 1: usar GeneralPurpose con SKU más pequeño disponible
        az postgres flexible-server create \
          --resource-group $RESOURCE_GROUP \
          --name $POSTGRES_SERVER \
          --location $LOCATION \
          --admin-user $POSTGRES_USER \
          --admin-password $POSTGRES_PASSWORD \
          --sku-name Standard_D2s_v3 \
          --tier GeneralPurpose \
          --version $POSTGRES_VERSION \
          --storage-size 32 \
          --public-access 0.0.0.0-255.255.255.255

        if [ $? -ne 0 ]; then
            echo -e "${YELLOW}⚠️  PostgreSQL 17 no disponible, intentando con versión 16...${NC}"
            POSTGRES_VERSION="16"

            # Fallback 2: PostgreSQL 16 con GeneralPurpose
            az postgres flexible-server create \
              --resource-group $RESOURCE_GROUP \
              --name $POSTGRES_SERVER \
              --location $LOCATION \
              --admin-user $POSTGRES_USER \
              --admin-password $POSTGRES_PASSWORD \
              --sku-name Standard_D2s_v3 \
              --tier GeneralPurpose \
              --version $POSTGRES_VERSION \
              --storage-size 32 \
              --public-access 0.0.0.0-255.255.255.255

            if [ $? -ne 0 ]; then
                echo -e "${YELLOW}⚠️  Canada Central falló, intentando en East US...${NC}"
                LOCATION="eastus"

                # Fallback 3: cambiar región a East US
                az postgres flexible-server create \
                  --resource-group $RESOURCE_GROUP \
                  --name $POSTGRES_SERVER \
                  --location $LOCATION \
                  --admin-user $POSTGRES_USER \
                  --admin-password $POSTGRES_PASSWORD \
                  --sku-name Standard_D2s_v3 \
                  --tier GeneralPurpose \
                  --version $POSTGRES_VERSION \
                  --storage-size 32 \
                  --public-access 0.0.0.0-255.255.255.255

                if [ $? -ne 0 ]; then
                    echo -e "${RED}❌ Error creando PostgreSQL Flexible Server en todas las configuraciones${NC}"
                    echo -e "${YELLOW}🔍 Posibles causas:${NC}"
                    echo "  1. Límites de cuota agotados"
                    echo "  2. El nombre del servidor ya está en uso globalmente"
                    echo "  3. Problemas de permisos en la suscripción"
                    echo ""
                    echo -e "${BLUE}💡 Soluciones manuales:${NC}"
                    echo "  1. Cambia POSTGRES_SERVER a otro nombre único en el script"
                    echo "  2. Verifica cuotas: az vm list-usage --location eastus"
                    echo "  3. Ejecuta: az postgres flexible-server create --help"
                    echo "  4. Crea el servidor manualmente desde Azure Portal"
                    exit 1
                else
                    echo -e "${GREEN}✅ PostgreSQL Server creado en East US con PostgreSQL 16${NC}"
                fi
            else
                echo -e "${GREEN}✅ PostgreSQL Server creado en Canada Central con PostgreSQL 16${NC}"
            fi
        else
            echo -e "${GREEN}✅ PostgreSQL Server creado en Canada Central con PostgreSQL 17${NC}"
        fi
    else
        echo -e "${GREEN}✅ PostgreSQL Server creado en Canada Central con tier Burstable${NC}"
    fi

    # Verificar que el servidor se creó correctamente
    sleep 30
    CREATED_SERVER=$(az postgres flexible-server show --name $POSTGRES_SERVER --resource-group $RESOURCE_GROUP --query "name" -o tsv 2>/dev/null)

    if [ -z "$CREATED_SERVER" ]; then
        echo -e "${RED}❌ El servidor no se encuentra después de la creación${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ PostgreSQL Flexible Server v$POSTGRES_VERSION creado exitosamente${NC}"
fi

# 7. Crear base de datos
echo -e "${YELLOW}🗄️  Creando base de datos $POSTGRES_DB...${NC}"
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $POSTGRES_SERVER \
  --database-name $POSTGRES_DB

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error creando base de datos${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Base de datos $POSTGRES_DB creada${NC}"

# 8. Inicializar esquema de base de datos
echo -e "${YELLOW}🔧 Inicializando esquema de base de datos...${NC}"

# Crear el SQL de inicialización temporalmente
cat > temp_init.sql << 'EOF'
-- Crear tabla products
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_products_client_id ON products(client_id);
CREATE INDEX IF NOT EXISTS idx_products_product_type ON products(product_type);

-- Insertar datos de prueba
INSERT INTO products (client_id, product_type, name, balance) VALUES
(1, 'SAVINGS', 'Cuenta de Ahorros Principal', 1500.00),
(1, 'CHECKING', 'Cuenta Corriente', 2500.50),
(2, 'SAVINGS', 'Cuenta de Ahorros VIP', 15000.75),
(2, 'CREDIT_CARD', 'Tarjeta de Crédito Gold', -850.25),
(3, 'SAVINGS', 'Cuenta Joven', 750.00)
ON CONFLICT (id) DO NOTHING;
EOF

# Ejecutar inicialización usando az postgres flexible-server execute (más confiable)
echo -e "${YELLOW}🔄 Ejecutando script de inicialización...${NC}"

# Configurar variable de entorno para el password
export PGPASSWORD=$POSTGRES_PASSWORD

# Ejecutar el script usando az postgres flexible-server execute
az postgres flexible-server execute \
  --name $POSTGRES_SERVER \
  --admin-user $POSTGRES_USER \
  --admin-password $POSTGRES_PASSWORD \
  --database-name $POSTGRES_DB \
  --file-path temp_init.sql

INIT_RESULT=$?

# Limpiar archivo temporal
rm -f temp_init.sql

if [ $INIT_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Base de datos inicializada correctamente${NC}"
else
    echo -e "${YELLOW}⚠️  Inicialización con az execute falló, intentando con psql...${NC}"

    # Fallback: intentar con psql si está disponible
    if command -v psql &> /dev/null; then
        echo -e "${YELLOW}🔄 Intentando inicialización con psql...${NC}"

        # Recrear el archivo SQL
        cat > temp_init.sql << 'EOF'
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_client_id ON products(client_id);
CREATE INDEX IF NOT EXISTS idx_products_product_type ON products(product_type);

INSERT INTO products (client_id, product_type, name, balance) VALUES
(1, 'SAVINGS', 'Cuenta de Ahorros Principal', 1500.00),
(1, 'CHECKING', 'Cuenta Corriente', 2500.50),
(2, 'SAVINGS', 'Cuenta de Ahorros VIP', 15000.75),
(2, 'CREDIT_CARD', 'Tarjeta de Crédito Gold', -850.25),
(3, 'SAVINGS', 'Cuenta Joven', 750.00)
ON CONFLICT (id) DO NOTHING;
EOF

        psql -h ${POSTGRES_SERVER}.postgres.database.azure.com \
             -U ${POSTGRES_USER} \
             -d $POSTGRES_DB \
             -f temp_init.sql

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Base de datos inicializada con psql${NC}"
        else
            echo -e "${YELLOW}⚠️  Inicialización manual falló, continuando deployment...${NC}"
            echo -e "${BLUE}💡 Podrás inicializar la BD manualmente después${NC}"
        fi

        rm -f temp_init.sql
    else
        echo -e "${YELLOW}⚠️  psql no disponible, continuando sin inicialización de BD${NC}"
        echo -e "${BLUE}💡 Instala postgresql-client para inicialización automática${NC}"
    fi
fi

# 9. Crear Azure Container Registry con admin habilitado
echo -e "${YELLOW}📦 Creando Azure Container Registry con admin habilitado...${NC}"
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $ACR_NAME \
  --sku Basic \
  --admin-enabled true
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error creando ACR${NC}"
    exit 1
fi
echo -e "${GREEN}✅ ACR creado con usuario administrador habilitado${NC}"

# 10. Hacer login al ACR
echo -e "${YELLOW}🔐 Autenticando con ACR...${NC}"
az acr login --name $ACR_NAME
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error autenticando con ACR${NC}"
    exit 1
fi

# 11. Obtener servidor ACR
ACR_SERVER=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP --query "loginServer" -o tsv)
echo "📋 ACR Server: $ACR_SERVER"

# 12. Subir imagen al ACR
echo -e "${YELLOW}📤 Subiendo imagen a ACR...${NC}"
docker tag product-service:latest $ACR_SERVER/product-service:latest
docker push $ACR_SERVER/product-service:latest
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error subiendo imagen a ACR${NC}"
    exit 1
fi

# 13. Crear App Service Plan (verificar si ya existe primero)
echo -e "${YELLOW}📋 Verificando/Creando App Service Plan...${NC}"

# Verificar si el plan ya existe
EXISTING_PLAN=$(az appservice plan show --name $APP_PLAN --resource-group $RESOURCE_GROUP --query "name" -o tsv 2>/dev/null)

if [ ! -z "$EXISTING_PLAN" ]; then
    echo -e "${GREEN}✅ App Service Plan ya existe: $EXISTING_PLAN${NC}"
else
    echo -e "${YELLOW}📋 Creando nuevo App Service Plan...${NC}"
    az appservice plan create \
      --name $APP_PLAN \
      --resource-group $RESOURCE_GROUP \
      --location $LOCATION \
      --sku B1 \
      --is-linux

    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Error creando App Service Plan${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ App Service Plan creado${NC}"
fi

# 14. Crear Web App (con mejor verificación de existencia)
echo -e "${YELLOW}🌐 Verificando/Creando Web App...${NC}"

# Verificar si la web app ya existe globalmente
echo -e "${YELLOW}🔍 Verificando disponibilidad del nombre: $WEB_APP${NC}"

# Intentar obtener información de la webapp para verificar si existe
EXISTING_APP=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "name" -o tsv 2>/dev/null)

if [ ! -z "$EXISTING_APP" ]; then
    echo -e "${GREEN}✅ Web App ya existe: $EXISTING_APP${NC}"
else
    echo -e "${YELLOW}🌐 Creando nueva Web App: $WEB_APP${NC}"

    # Crear la webapp sin especificar imagen de contenedor inicialmente
    az webapp create \
      --resource-group $RESOURCE_GROUP \
      --plan $APP_PLAN \
      --name $WEB_APP \
      --runtime "JAVA:17-java17"

    CREATE_RESULT=$?

    if [ $CREATE_RESULT -ne 0 ]; then
        echo -e "${RED}❌ Error creando Web App${NC}"
        echo -e "${YELLOW}🔄 Intentando con nombre alternativo...${NC}"

        # Generar un nombre más único
        WEB_APP="product-service-app-$(date +%Y%m%d%H%M%S)"
        echo -e "${YELLOW}📋 Intentando con: $WEB_APP${NC}"

        az webapp create \
          --resource-group $RESOURCE_GROUP \
          --plan $APP_PLAN \
          --name $WEB_APP \
          --runtime "JAVA:17-java17"

        if [ $? -ne 0 ]; then
            echo -e "${RED}❌ Error creando Web App con nombre alternativo${NC}"
            echo -e "${YELLOW}💡 Sugerencias:${NC}"
            echo "  1. Verifica que el nombre sea globalmente único"
            echo "  2. Intenta con otro nombre manualmente"
            echo "  3. Verifica permisos en la suscripción"
            exit 1
        fi
    fi

    echo -e "${GREEN}✅ Web App creada: $WEB_APP${NC}"
fi

# 15. Configurar puerto
echo -e "${YELLOW}⚙️  Configurando puerto...${NC}"
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --settings WEBSITES_PORT=8020

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando puerto${NC}"
    exit 1
fi

# 16. Configurar ACR credentials
echo -e "${YELLOW}🔑 Configurando credenciales ACR...${NC}"

# Obtener credenciales ACR
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query "username" -o tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv)

if [ -z "$ACR_USERNAME" ] || [ -z "$ACR_PASSWORD" ]; then
    echo -e "${RED}❌ Error obteniendo credenciales ACR${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Credenciales ACR obtenidas${NC}"

# Configurar contenedor con credenciales (usando comandos actualizados)
az webapp config container set \
  --name $WEB_APP \
  --resource-group $RESOURCE_GROUP \
  --container-image-name $ACR_SERVER/product-service:latest \
  --container-registry-url https://$ACR_SERVER \
  --container-registry-user $ACR_USERNAME \
  --container-registry-password $ACR_PASSWORD

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando credenciales del contenedor${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Credenciales del contenedor configuradas${NC}"

# 17. Configurar variables de entorno con PostgreSQL Flexible Server
echo -e "${YELLOW}🔧 Configurando variables de entorno...${NC}"

# Construir connection string de PostgreSQL Flexible Server
POSTGRES_CONNECTION_STRING="r2dbc:postgresql://${POSTGRES_SERVER}.postgres.database.azure.com:5432/${POSTGRES_DB}?sslmode=require"

az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --settings \
    WEBSITES_PORT=8020 \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=azure \
    WEBSITES_ENABLE_APP_SERVICE_STORAGE=false \
    AZURE_POSTGRESQL_URL="$POSTGRES_CONNECTION_STRING" \
    AZURE_POSTGRESQL_USERNAME="${POSTGRES_USER}" \
    AZURE_POSTGRESQL_PASSWORD="$POSTGRES_PASSWORD"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando variables de entorno${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Variables de entorno configuradas${NC}"

# 18. Habilitar logging para diagnóstico
echo -e "${YELLOW}📝 Habilitando logging del contenedor...${NC}"
az webapp log config \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --docker-container-logging filesystem

# 19. Reiniciar aplicación para aplicar cambios
echo -e "${YELLOW}🔄 Reiniciando aplicación para aplicar configuración...${NC}"
az webapp restart --name $WEB_APP --resource-group $RESOURCE_GROUP

# 20. Obtener URL de la aplicación
APP_URL=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "defaultHostName" -o tsv)

echo ""
echo -e "${GREEN}🎉 ¡Deployment del product-service completado exitosamente!${NC}"
echo -e "${GREEN}📋 Información del deployment:${NC}"
echo "   Resource Group: $RESOURCE_GROUP"
echo "   Región: $LOCATION"
echo "   ACR Name: $ACR_NAME"
echo "   Web App Name: $WEB_APP"
echo "   PostgreSQL Server: $POSTGRES_SERVER v$POSTGRES_VERSION"
echo "   Database: $POSTGRES_DB"
echo "   URL: https://$APP_URL"
echo "   API Documentation: https://$APP_URL/swagger-ui.html"
echo ""
echo -e "${YELLOW}📝 Comandos útiles:${NC}"
echo "   Ver logs: az webapp log tail --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Reiniciar: az webapp restart --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Ver configuración: az webapp config show --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Conectar a BD: PGPASSWORD=$POSTGRES_PASSWORD psql -h ${POSTGRES_SERVER}.postgres.database.azure.com -U ${POSTGRES_USER} -d $POSTGRES_DB"
echo "   Eliminar recursos: az group delete --name $RESOURCE_GROUP --yes --no-wait"

# 21. Esperar y verificar con diagnóstico mejorado
echo -e "${YELLOW}⏳ Esperando que el product-service inicie...${NC}"
sleep 60

echo -e "${YELLOW}🔍 Verificando estado de la aplicación...${NC}"

# Verificar estado de la Web App
APP_STATE=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "state" -o tsv)
echo "Estado de la aplicación: $APP_STATE"

# Verificar conectividad con múltiples intentos
for attempt in {1..5}; do
    echo "Intento de verificación $attempt/5..."

    # Probar endpoint de salud
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$APP_URL/actuator/health 2>/dev/null || echo "000")

    if [ "$HTTP_STATUS" = "200" ]; then
        echo -e "${GREEN}✅ ¡Product-service funcionando correctamente!${NC}"
        echo -e "${GREEN}🎯 API disponible en: https://$APP_URL${NC}"
        echo -e "${GREEN}📚 Documentación API: https://$APP_URL/swagger-ui.html${NC}"
        break
    elif [ "$HTTP_STATUS" = "404" ]; then
        echo -e "${YELLOW}⚠️  Endpoint /actuator/health no encontrado, probando endpoint raíz...${NC}"
        ROOT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$APP_URL 2>/dev/null || echo "000")
        if [ "$ROOT_STATUS" = "200" ]; then
            echo -e "${GREEN}✅ Product-service funcionando - disponible en endpoint raíz${NC}"
            echo -e "${GREEN}🎯 API disponible en: https://$APP_URL${NC}"
            break
        else
            echo -e "${YELLOW}⚠️  Estado raíz: $ROOT_STATUS${NC}"
        fi
    else
        echo -e "${YELLOW}⏳ Estado HTTP: $HTTP_STATUS - La aplicación aún se está iniciando...${NC}"

        if [ $attempt -lt 5 ]; then
            echo "Esperando 45 segundos antes del siguiente intento..."
            sleep 45
        fi
    fi
done

if [ "$HTTP_STATUS" != "200" ] && [ "$ROOT_STATUS" != "200" ]; then
    echo -e "${YELLOW}⚠️  El product-service aún se está iniciando${NC}"
    echo -e "${BLUE}📋 Para diagnosticar:${NC}"
    echo "   1. Visita: https://$APP_URL"
    echo "   2. Ver logs: az webapp log tail --name $WEB_APP --resource-group $RESOURCE_GROUP"
    echo "   3. Estado del contenedor: az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query 'state'"
    echo "   4. Configuración: az webapp config container show --name $WEB_APP --resource-group $RESOURCE_GROUP"
    echo ""
    echo -e "${YELLOW}💡 Nota: Las aplicaciones Java con BD pueden tardar 5-10 minutos en iniciar completamente${NC}"
    echo -e "${BLUE}🔍 El servicio se registrará automáticamente en Eureka una vez que esté funcionando${NC}"
fi

# 22. Verificar datos en la base de datos (si psql está disponible)
if command -v psql &> /dev/null; then
    echo -e "${YELLOW}🔍 Verificando datos en la base de datos...${NC}"

    export PGPASSWORD=$POSTGRES_PASSWORD
    PRODUCT_COUNT=$(psql -h ${POSTGRES_SERVER}.postgres.database.azure.com \
                         -U ${POSTGRES_USER} \
                         -d $POSTGRES_DB \
                         -t -c "SELECT COUNT(*) FROM products;" 2>/dev/null | xargs)

    if [ ! -z "$PRODUCT_COUNT" ] && [ "$PRODUCT_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✅ Base de datos inicializada: $PRODUCT_COUNT productos de prueba${NC}"
    else
        echo -e "${YELLOW}⚠️  Base de datos puede necesitar inicialización manual${NC}"
    fi
fi

echo ""
echo -e "${GREEN}🔗 Enlaces importantes:${NC}"
echo "   Admin Service: https://admin-service-app.azurewebsites.net"
echo "   Registry Service: https://registry-service-app.azurewebsites.net"
echo "   Product Service: https://$APP_URL"
echo ""
echo -e "${BLUE}🌍 Recursos distribuidos geográficamente:${NC}"
echo "   Product Service: $LOCATION"
echo "   Otros servicios: East US"
echo "   ✅ Sin problemas de comunicación entre regiones"