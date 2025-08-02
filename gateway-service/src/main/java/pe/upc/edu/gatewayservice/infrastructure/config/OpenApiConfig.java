package pe.upc.edu.gatewayservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Financia Gateway API")
                        .version("1.0")
                        .description("API Gateway for Financia Microservices - WebFlux Reactive Gateway"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8010")
                                .description("Gateway Server")
                ));
    }

    @Bean
    @Lazy(false)
    public Mono<List<GroupedOpenApi>> apis(RouteDefinitionLocator routeDefinitionLocator) {
        return routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> routeDefinition.getId().matches(".*-service"))
                .collectList()
                .map(definitions -> {
                    List<GroupedOpenApi> groups = new ArrayList<>();
                    definitions.forEach(routeDefinition -> {
                        String name = routeDefinition.getId();
                        groups.add(GroupedOpenApi.builder()
                                .pathsToMatch("/" + name + "/**")
                                .group(name)
                                .addOpenApiCustomizer(openApiCustomizer())
                                .build());
                    });
                    return groups;
                });
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            // Ensure all operations use the gateway server
            openApi.servers(List.of(
                    new Server()
                            .url("http://localhost:8010")
                            .description("Gateway Server")
            ));
        };
    }
}