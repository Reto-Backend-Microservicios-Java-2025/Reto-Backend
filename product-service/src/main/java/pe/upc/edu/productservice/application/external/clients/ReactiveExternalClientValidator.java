package pe.upc.edu.productservice.application.external.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;

@Component
public class ReactiveExternalClientValidator {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveExternalClientValidator.class);
    private final WebClient customerServiceWebClient;

    public ReactiveExternalClientValidator(WebClient customerServiceWebClient) {
        this.customerServiceWebClient = customerServiceWebClient;
    }

    /**
     * Valida de forma asíncrona si un cliente existe usando su ID directamente
     * @param clientId El ID del cliente (no uniqueCode, sino el ID real)
     * @return Mono<Boolean> - true si existe, false si no existe
     */
    public Mono<Boolean> clientExists(Long clientId) {
        logger.info("🔍 Validating client existence for clientId: {}", clientId);

        String uri = "/api/v1/clients/id/" + clientId;
        logger.info("🌐 Making request to: {}", uri);

        return customerServiceWebClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Object.class)
                .doOnNext(response -> logger.info("✅ Client {} exists, response: {}", clientId, response))
                .map(response -> true) // Si hay respuesta, el cliente existe
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException.NotFound) {
                        logger.warn("❌ Client {} not found (404)", clientId);
                    } else {
                        logger.error("💥 Error validating client {}: {}", clientId, error.getMessage());
                    }
                })
                .onErrorReturn(WebClientResponseException.NotFound.class, false) // 404 = no existe
                .timeout(Duration.ofSeconds(5)) // Timeout de 5 segundos
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))) // 2 reintentos con backoff
                .doOnSuccess(exists -> logger.info("🎯 Final validation result for client {}: {}", clientId, exists))
                .onErrorResume(throwable -> {
                    // Manejo de errores diferentes a 404
                    logger.error("💥 Critical error validating client {}: {}", clientId, throwable.getMessage());
                    return Mono.error(new RuntimeException(
                            "Error validating client " + clientId + ": " + throwable.getMessage()));
                });
    }
}