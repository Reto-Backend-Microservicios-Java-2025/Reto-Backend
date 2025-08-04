package pe.upc.edu.customerservice.application.external.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient productServiceWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8010/product-service") // URL del microservicio de productos
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB para productos
                .build();
    }
}