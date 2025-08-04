package pe.upc.edu.customerservice.application.external.products;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.upc.edu.customerservice.interfaces.rest.resources.ProductResource;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
public class ReactiveExternalProduct {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveExternalProduct.class);
    private final WebClient productServiceWebClient;

    public ReactiveExternalProduct(WebClient productServiceWebClient) {
        this.productServiceWebClient = productServiceWebClient;
    }

    /**
     * Obtiene de forma asíncrona todos los productos de un cliente
     * @param clientId El ID del cliente
     * @return Mono<List<ProductResource>> - Lista de productos del cliente
     */
    public Mono<List<ProductResource>> getProductsByClientId(Long clientId) {
        logger.info("🔍 Getting products for clientId: {}", clientId);

        String uri = "/api/v1/products/client/" + clientId;
        logger.info("🌐 Making request to: {}", uri);

        return productServiceWebClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(ProductResource[].class)
                .doOnNext(products -> logger.info("✅ Found {} products for client {}",
                        products != null ? products.length : 0, clientId))
                .map(products -> products != null ? List.of(products) : List.<ProductResource>of())
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException.NotFound) {
                        logger.warn("❌ No products found for client {} (404)", clientId);
                    } else {
                        logger.error("💥 Error getting products for client {}: {}", clientId, error.getMessage());
                    }
                })
                .onErrorReturn(WebClientResponseException.NotFound.class, List.of()) // 404 = sin productos
                .timeout(Duration.ofSeconds(10)) // Timeout de 10 segundos para productos
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))) // 2 reintentos con backoff
                .doOnSuccess(products -> logger.info("🎯 Final result for client {}: {} products",
                        clientId, products.size()))
                .onErrorResume(throwable -> {
                    // Manejo de errores diferentes a 404
                    logger.error("💥 Critical error getting products for client {}: {}",
                            clientId, throwable.getMessage());
                    return Mono.error(new RuntimeException(
                            "Error al obtener productos del cliente " + clientId + ": " + throwable.getMessage()));
                });
    }
}