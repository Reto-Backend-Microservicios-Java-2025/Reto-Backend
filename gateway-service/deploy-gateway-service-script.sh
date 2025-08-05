#!/bin/bash

# Script para el deployment del gateway-service con Docker y Azure App Service
# Gateway Service - API Gateway para microservicios Financia

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
RESOURCE_GROUP="gateway-service-rg"
LOCATION="eastus"  # Misma región que admin y registry
ACR_NAME="gatewayserviceacr$(date +%s)"
APP_PLAN="gateway-service-plan"
WEB_APP="gateway-service-app-$(date +%s)"

echo "🚀 Iniciando deployment del gateway-service en Azure App Service..."
echo "📍 Región: East US (misma que admin-service y registry-service)"

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

REQUIRED_PROVIDERS=("Microsoft.ContainerRegistry" "Microsoft.Web")

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
echo -e "${YELLOW}⚠️  ¿Estás seguro de que quieres crear recursos para gateway-service?${NC}"
echo "Se crearán los siguientes recursos:"
echo "  - Resource Group: $RESOURCE_GROUP"
echo "  - Container Registry: $ACR_NAME"
echo "  - App Service Plan: $APP_PLAN"
echo "  - Web App: $WEB_APP"
echo "  - Ubicación: $LOCATION"
echo ""
echo -e "${BLUE}🔗 El gateway se conectará a:${NC}"
echo "  - Registry Service: https://registry-service-app.azurewebsites.net"
echo "  - Admin Service: https://admin-service-app.azurewebsites.net"
echo "  - Product Service: https://product-service-app-1754370901.azurewebsites.net"
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
echo -e "${YELLOW}🔨 Compilando gateway-service...${NC}"
mvn clean package -DskipTests

if [ ! -f "target/gateway-service-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ Error: No se encontró el JAR compilado${NC}"
    echo "Verifica que el proyecto se compile correctamente"
    exit 1
fi
echo -e "${GREEN}✅ Gateway-service compilado exitosamente${NC}"

# 4. Construir imagen Docker
echo -e "${YELLOW}🐳 Construyendo imagen Docker...${NC}"
docker build -t gateway-service:latest .
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error construyendo imagen Docker${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Imagen Docker construida${NC}"

# 5. Crear resource group
echo -e "${YELLOW}📁 Creando resource group en East US...${NC}"
az group create --name $RESOURCE_GROUP --location $LOCATION
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error creando resource group${NC}"
    exit 1
fi

# 6. Crear Azure Container Registry con admin habilitado
echo -e "${YELLOW}📦 Creando Azure Container Registry...${NC}"
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

# 7. Hacer login al ACR
echo -e "${YELLOW}🔐 Autenticando con ACR...${NC}"
az acr login --name $ACR_NAME
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error autenticando con ACR${NC}"
    exit 1
fi

# 8. Obtener servidor ACR
ACR_SERVER=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP --query "loginServer" -o tsv)
echo "📋 ACR Server: $ACR_SERVER"

# 9. Subir imagen al ACR
echo -e "${YELLOW}📤 Subiendo imagen a ACR...${NC}"
docker tag gateway-service:latest $ACR_SERVER/gateway-service:latest
docker push $ACR_SERVER/gateway-service:latest
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error subiendo imagen a ACR${NC}"
    exit 1
fi

# 10. Crear App Service Plan
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

# 11. Crear Web App
echo -e "${YELLOW}🌐 Verificando/Creando Web App...${NC}"

# Verificar si la web app ya existe
EXISTING_APP=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "name" -o tsv 2>/dev/null)

if [ ! -z "$EXISTING_APP" ]; then
    echo -e "${GREEN}✅ Web App ya existe: $EXISTING_APP${NC}"
else
    echo -e "${YELLOW}🌐 Creando nueva Web App: $WEB_APP${NC}"

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
        WEB_APP="gateway-service-app-$(date +%Y%m%d%H%M%S)"
        echo -e "${YELLOW}📋 Intentando con: $WEB_APP${NC}"

        az webapp create \
          --resource-group $RESOURCE_GROUP \
          --plan $APP_PLAN \
          --name $WEB_APP \
          --runtime "JAVA:17-java17"

        if [ $? -ne 0 ]; then
            echo -e "${RED}❌ Error creando Web App con nombre alternativo${NC}"
            exit 1
        fi
    fi

    echo -e "${GREEN}✅ Web App creada: $WEB_APP${NC}"
fi

# 12. Configurar puerto
echo -e "${YELLOW}⚙️  Configurando puerto...${NC}"
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --settings WEBSITES_PORT=8010

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando puerto${NC}"
    exit 1
fi

# 13. Configurar ACR credentials
echo -e "${YELLOW}🔑 Configurando credenciales ACR...${NC}"

# Obtener credenciales ACR
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query "username" -o tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv)

if [ -z "$ACR_USERNAME" ] || [ -z "$ACR_PASSWORD" ]; then
    echo -e "${RED}❌ Error obteniendo credenciales ACR${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Credenciales ACR obtenidas${NC}"

# Configurar contenedor con credenciales
az webapp config container set \
  --name $WEB_APP \
  --resource-group $RESOURCE_GROUP \
  --container-image-name $ACR_SERVER/gateway-service:latest \
  --container-registry-url https://$ACR_SERVER \
  --container-registry-user $ACR_USERNAME \
  --container-registry-password $ACR_PASSWORD

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando credenciales del contenedor${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Credenciales del contenedor configuradas${NC}"

# 14. Configurar variables de entorno
echo -e "${YELLOW}🔧 Configurando variables de entorno...${NC}"

az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --settings \
    WEBSITES_PORT=8010 \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=azure \
    WEBSITES_ENABLE_APP_SERVICE_STORAGE=false

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando variables de entorno${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Variables de entorno configuradas${NC}"

# 15. Habilitar logging para diagnóstico
echo -e "${YELLOW}📝 Comandos útiles:${NC}"
echo "   Ver logs: az webapp log tail --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Reiniciar: az webapp restart --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Ver configuración: az webapp config show --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Eliminar recursos: az group delete --name $RESOURCE_GROUP --yes --no-wait"

# 18. Esperar y verificar con diagnóstico mejorado
echo -e "${YELLOW}⏳ Esperando que el gateway-service inicie...${NC}"
echo -e "${BLUE}💡 El gateway necesita conectarse a registry-service para funcionar correctamente${NC}"
sleep 90

echo -e "${YELLOW}🔍 Verificando estado de la aplicación...${NC}"

# Verificar estado de la Web App
APP_STATE=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "state" -o tsv)
echo "Estado de la aplicación: $APP_STATE"

# Verificar conectividad con múltiples intentos
for attempt in {1..6}; do
    echo "Intento de verificación $attempt/6..."

    # Probar endpoint de salud
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$APP_URL/actuator/health 2>/dev/null || echo "000")

    if [ "$HTTP_STATUS" = "200" ]; then
        echo -e "${GREEN}✅ ¡Gateway-service funcionando correctamente!${NC}"
        echo -e "${GREEN}🎯 Gateway disponible en: https://$APP_URL${NC}"
        echo -e "${GREEN}📚 Documentación API: https://$APP_URL/swagger-ui.html${NC}"
        break
    elif [ "$HTTP_STATUS" = "404" ]; then
        echo -e "${YELLOW}⚠️  Endpoint /actuator/health no encontrado, probando endpoint raíz...${NC}"
        ROOT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$APP_URL 2>/dev/null || echo "000")
        if [ "$ROOT_STATUS" = "200" ]; then
            echo -e "${GREEN}✅ Gateway-service funcionando - disponible en endpoint raíz${NC}"
            echo -e "${GREEN}🎯 Gateway disponible en: https://$APP_URL${NC}"
            break
        else
            echo -e "${YELLOW}⚠️  Estado raíz: $ROOT_STATUS${NC}"
        fi
    else
        echo -e "${YELLOW}⏳ Estado HTTP: $HTTP_STATUS - La aplicación aún se está iniciando...${NC}"

        if [ $attempt -lt 6 ]; then
            echo "Esperando 60 segundos antes del siguiente intento..."
            sleep 60
        fi
    fi
done

if [ "$HTTP_STATUS" != "200" ] && [ "$ROOT_STATUS" != "200" ]; then
    echo -e "${YELLOW}⚠️  El gateway-service aún se está iniciando${NC}"
    echo -e "${BLUE}📋 Para diagnosticar:${NC}"
    echo "   1. Visita: https://$APP_URL"
    echo "   2. Ver logs: az webapp log tail --name $WEB_APP --resource-group $RESOURCE_GROUP"
    echo "   3. Estado del contenedor: az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query 'state'"
    echo "   4. Configuración: az webapp config container show --name $WEB_APP --resource-group $RESOURCE_GROUP"
    echo ""
    echo -e "${YELLOW}💡 Nota: Los gateways pueden tardar 5-15 minutos en registrarse completamente${NC}"
    echo -e "${BLUE}🔍 Verificar que registry-service esté funcionando antes de usar el gateway${NC}"
fi

# 19. Verificar conectividad con servicios dependientes
echo -e "${YELLOW}🔍 Verificando conectividad con servicios dependientes...${NC}"

# Verificar Registry Service
echo "Verificando Registry Service..."
REGISTRY_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://registry-service-app.azurewebsites.net/actuator/health 2>/dev/null || echo "000")
if [ "$REGISTRY_STATUS" = "200" ]; then
    echo -e "${GREEN}✅ Registry Service: Funcionando${NC}"
else
    echo -e "${YELLOW}⚠️  Registry Service: Estado $REGISTRY_STATUS${NC}"
fi

# Verificar Admin Service
echo "Verificando Admin Service..."
ADMIN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://admin-service-app.azurewebsites.net/actuator/health 2>/dev/null || echo "000")
if [ "$ADMIN_STATUS" = "200" ]; then
    echo -e "${GREEN}✅ Admin Service: Funcionando${NC}"
else
    echo -e "${YELLOW}⚠️  Admin Service: Estado $ADMIN_STATUS${NC}"
fi

# Verificar Product Service
echo "Verificando Product Service..."
PRODUCT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://product-service-app-1754370901.azurewebsites.net/actuator/health 2>/dev/null || echo "000")
if [ "$PRODUCT_STATUS" = "200" ]; then
    echo -e "${GREEN}✅ Product Service: Funcionando${NC}"
else
    echo -e "${YELLOW}⚠️  Product Service: Estado $PRODUCT_STATUS${NC}"
fi

echo ""
echo -e "${GREEN}🎯 Gateway Service deployado exitosamente!${NC}"
echo ""
echo -e "${BLUE}🔗 URLs importantes:${NC}"
echo "   Gateway Service: https://$APP_URL"
echo "   Gateway Swagger: https://$APP_URL/swagger-ui.html"
echo "   Product Service (via Gateway): https://$APP_URL/product-service/api/v1/"
echo "   Registry Service: https://registry-service-app.azurewebsites.net"
echo "   Admin Service: https://admin-service-app.azurewebsites.net"
echo ""
echo -e "${GREEN}🧪 Endpoints de prueba:${NC}"
echo "   Gateway Health: https://$APP_URL/actuator/health"
echo "   Product Service (via Gateway): https://$APP_URL/product-service/api/v1/products"
echo "   Product Service (direct): https://product-service-app-1754370901.azurewebsites.net/api/v1/products"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANTE:${NC}"
echo "   - El gateway puede tardar 10-15 minutos en registrarse completamente en Eureka"
echo "   - Verifica que todos los servicios dependientes estén funcionando"
echo "   - Las rutas del gateway solo funcionarán cuando los servicios estén registrados"
echo ""
echo -e "${BLUE}🔍 Para verificar el registro en Eureka:${NC}"
echo "   Visita: https://registry-service-app.azurewebsites.net"
echo "   Busca 'gateway-service' en la lista de aplicaciones registradas"Habilitando logging del contenedor...${NC}"
az webapp log config \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --docker-container-logging filesystem

# 16. Reiniciar aplicación para aplicar cambios
echo -e "${YELLOW}🔄 Reiniciando aplicación para aplicar configuración...${NC}"
az webapp restart --name $WEB_APP --resource-group $RESOURCE_GROUP

# 17. Obtener URL de la aplicación
APP_URL=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "defaultHostName" -o tsv)

echo ""
echo -e "${GREEN}🎉 ¡Deployment del gateway-service completado exitosamente!${NC}"
echo -e "${GREEN}📋 Información del deployment:${NC}"
echo "   Resource Group: $RESOURCE_GROUP"
echo "   Región: $LOCATION"
echo "   ACR Name: $ACR_NAME"
echo "   Web App Name: $WEB_APP"
echo "   URL Gateway: https://$APP_URL"
echo "   Swagger UI: https://$APP_URL/swagger-ui.html"
echo ""
echo -e "${BLUE}🔗 URLs de los servicios conectados:${NC}"
echo "   Registry Service: https://registry-service-app.azurewebsites.net"
echo "   Admin Service: https://admin-service-app.azurewebsites.net"
echo "   Product Service: https://product-service-app-1754370901.azurewebsites.net"
echo ""
echo -e "${GREEN}🌐 Endpoints del API Gateway:${NC}"
echo "   Product Service: https://$APP_URL/product-service/api/v1/"
echo "   Customer Service: https://$APP_URL/customer-service/api/v1/"
echo "   IAM Service: https://$APP_URL/iam-service/api/v1/"
echo ""
echo -e "${YELLOW}📝