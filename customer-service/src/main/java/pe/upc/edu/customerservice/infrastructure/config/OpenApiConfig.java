package pe.upc.edu.customerservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Service API (Reactive)")
                        .version("2.0")
                        .description("Reactive API for Customer Service using WebFlux"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8010/customer-service")
                                .description("Gateway Server (Reactive)")
                ));
    }
}