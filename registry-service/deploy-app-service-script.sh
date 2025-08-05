#!/bin/bash

# Script para el deployment de la aplicación con Docker y Azure App Service

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuración
RESOURCE_GROUP="registry-rg"
LOCATION="eastus"
ACR_NAME="registryacr$(date +%s)"
APP_PLAN="registry-plan"
WEB_APP="registry-service-app"

echo "🚀 Iniciando deployment en Azure App Service..."

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

REQUIRED_PROVIDERS=("Microsoft.ContainerRegistry" "Microsoft.Web" "Microsoft.Storage")

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
echo "Se crearán los siguientes recursos:"
echo "  - Resource Group: $RESOURCE_GROUP"
echo "  - Container Registry: $ACR_NAME"
echo "  - App Service Plan: $APP_PLAN"
echo "  - Web App: $WEB_APP"
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
echo -e "${YELLOW}🔨 Compilando aplicación...${NC}"
mvn clean package -DskipTests

if [ ! -f "target/registry-service-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ Error: No se encontró el JAR compilado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Aplicación compilada exitosamente${NC}"

# 4. Construir imagen Docker
echo -e "${YELLOW}🐳 Construyendo imagen Docker...${NC}"
docker build -t registry-service:latest .
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error construyendo imagen Docker${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Imagen Docker construida${NC}"

# 5. Crear resource group
echo -e "${YELLOW}📁 Creando resource group...${NC}"
az group create --name $RESOURCE_GROUP --location $LOCATION
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error creando resource group${NC}"
    exit 1
fi

# 6. Crear Azure Container Registry con admin habilitado
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
docker tag registry-service:latest $ACR_SERVER/registry-service:latest
docker push $ACR_SERVER/registry-service:latest
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error subiendo imagen a ACR${NC}"
    exit 1
fi

# 10. Crear App Service Plan
echo -e "${YELLOW}📋 Creando App Service Plan...${NC}"
az appservice plan create \
  --name $APP_PLAN \
  --resource-group $RESOURCE_GROUP \
  --sku B1 \
  --is-linux

# 11. Crear Web App
echo -e "${YELLOW}🌐 Creando Web App...${NC}"
az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan $APP_PLAN \
  --name $WEB_APP \
  --deployment-container-image-name $ACR_SERVER/registry-service:latest

# 12. Configurar puerto
echo -e "${YELLOW}⚙️  Configurando puerto...${NC}"
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --settings WEBSITES_PORT=8090

# 13. Configurar ACR credentials
echo -e "${YELLOW}🔑 Configurando credenciales ACR...${NC}"

# Verificar que el admin está habilitado y obtener credenciales
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query "username" -o tsv 2>/dev/null)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv 2>/dev/null)

if [ -z "$ACR_USERNAME" ] || [ -z "$ACR_PASSWORD" ]; then
    echo -e "${YELLOW}⚠️  Habilitando admin en ACR y obteniendo credenciales...${NC}"
    az acr update -n $ACR_NAME --admin-enabled true
    ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query "username" -o tsv)
    ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv)
fi

if [ -z "$ACR_USERNAME" ] || [ -z "$ACR_PASSWORD" ]; then
    echo -e "${RED}❌ Error obteniendo credenciales ACR${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Credenciales ACR obtenidas${NC}"
echo "   Usuario: $ACR_USERNAME"

# Configurar contenedor con credenciales
az webapp config container set \
  --name $WEB_APP \
  --resource-group $RESOURCE_GROUP \
  --docker-custom-image-name $ACR_SERVER/registry-service:latest \
  --docker-registry-server-url https://$ACR_SERVER \
  --docker-registry-server-user $ACR_USERNAME \
  --docker-registry-server-password $ACR_PASSWORD

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando credenciales del contenedor${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Credenciales del contenedor configuradas${NC}"

# 14. Configurar variables de entorno adicionales
echo -e "${YELLOW}🔧 Configurando variables de entorno...${NC}"
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --settings \
    WEBSITES_PORT=8090 \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    WEBSITES_ENABLE_APP_SERVICE_STORAGE=false

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error configurando variables de entorno${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Variables de entorno configuradas${NC}"

# 14.1. Habilitar logging para diagnóstico
echo -e "${YELLOW}📝 Habilitando logging del contenedor...${NC}"
az webapp log config \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP \
  --docker-container-logging filesystem

# 14.2. Reiniciar aplicación para aplicar cambios
echo -e "${YELLOW}🔄 Reiniciando aplicación para aplicar configuración...${NC}"
az webapp restart --name $WEB_APP --resource-group $RESOURCE_GROUP

# 15. Obtener URL de la aplicación
APP_URL=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "defaultHostName" -o tsv)

echo ""
echo -e "${GREEN}🎉 ¡Deployment completado exitosamente!${NC}"
echo -e "${GREEN}📋 Información del deployment:${NC}"
echo "   Resource Group: $RESOURCE_GROUP"
echo "   ACR Name: $ACR_NAME"
echo "   Web App Name: $WEB_APP"
echo "   URL: https://$APP_URL"
echo "   Eureka Dashboard: https://$APP_URL"
echo ""
echo -e "${YELLOW}📝 Comandos útiles:${NC}"
echo "   Ver logs: az webapp log tail --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Reiniciar: az webapp restart --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Ver configuración: az webapp config show --name $WEB_APP --resource-group $RESOURCE_GROUP"
echo "   Eliminar recursos: az group delete --name $RESOURCE_GROUP --yes --no-wait"

# 16. Esperar y verificar con diagnóstico mejorado
echo -e "${YELLOW}⏳ Esperando que la aplicación inicie...${NC}"
sleep 45

echo -e "${YELLOW}🔍 Verificando estado de la aplicación...${NC}"

# Verificar estado de la Web App
APP_STATE=$(az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query "state" -o tsv)
echo "Estado de la aplicación: $APP_STATE"

# Verificar conectividad con múltiples intentos
for attempt in {1..3}; do
    echo "Intento de verificación $attempt/3..."

    # Probar endpoint de salud
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$APP_URL/actuator/health 2>/dev/null || echo "000")

    if [ "$HTTP_STATUS" = "200" ]; then
        echo -e "${GREEN}✅ ¡Aplicación funcionando correctamente!${NC}"
        echo -e "${GREEN}🎯 Eureka Server disponible en: https://$APP_URL${NC}"
        break
    elif [ "$HTTP_STATUS" = "404" ]; then
        echo -e "${YELLOW}⚠️  Endpoint /actuator/health no encontrado, probando endpoint raíz...${NC}"
        ROOT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$APP_URL 2>/dev/null || echo "000")
        if [ "$ROOT_STATUS" = "200" ]; then
            echo -e "${GREEN}✅ Aplicación funcionando (disponible en endpoint raíz)${NC}"
            echo -e "${GREEN}🎯 Eureka Server disponible en: https://$APP_URL${NC}"
            break
        else
            echo -e "${YELLOW}⚠️  Estado raíz: $ROOT_STATUS${NC}"
        fi
    else
        echo -e "${YELLOW}⏳ Estado HTTP: $HTTP_STATUS - La aplicación aún se está iniciando...${NC}"

        if [ $attempt -lt 3 ]; then
            echo "Esperando 30 segundos antes del siguiente intento..."
            sleep 30
        fi
    fi
done

if [ "$HTTP_STATUS" != "200" ] && [ "$ROOT_STATUS" != "200" ]; then
    echo -e "${YELLOW}⚠️  La aplicación aún se está iniciando${NC}"
    echo -e "${BLUE}📋 Para diagnosticar:${NC}"
    echo "   1. Visita: https://$APP_URL"
    echo "   2. Ver logs: az webapp log tail --name $WEB_APP --resource-group $RESOURCE_GROUP"
    echo "   3. Estado del contenedor: az webapp show --name $WEB_APP --resource-group $RESOURCE_GROUP --query 'state'"
    echo "   4. Configuración: az webapp config container show --name $WEB_APP --resource-group $RESOURCE_GROUP"
    echo ""
    echo -e "${YELLOW}💡 Nota: Las aplicaciones Java pueden tardar 2-5 minutos en iniciar completamente${NC}"
fi