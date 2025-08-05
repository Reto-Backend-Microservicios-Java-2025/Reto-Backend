package pe.upc.edu.productservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Service API (Reactive)")
                        .version("2.0")
                        .description("Reactive API for Product Service using WebFlux"))
                .servers(getServers());
    }

    private List<Server> getServers() {
        if ("azure".equals(activeProfile) || "prod".equals(activeProfile)) {
            return List.of(
                    new Server()
                            .url("https://product-service-app.azurewebsites.net")
                            .description("Azure Production Server"),
                    new Server()
                            .url("https://gateway-service-app.azurewebsites.net/product-service")
                            .description("Azure Gateway Server (Reactive)")
            );
        } else {
            return List.of(
                    new Server()
                            .url("http://localhost:8010/product-service")
                            .description("Local Gateway Server (Reactive)")
            );
        }
    }
}