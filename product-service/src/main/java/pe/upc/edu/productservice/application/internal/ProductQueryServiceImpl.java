package pe.upc.edu.productservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.queries.GetAllProductsQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductByIdQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductsByClientIdQuery;
import pe.upc.edu.productservice.domain.services.ProductQueryService;
import pe.upc.edu.productservice.infrastructure.persistence.r2dbc.repositories.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Flux<Product> handle(GetAllProductsQuery query) {
        return productRepository.findAll()
                .onErrorResume(throwable -> {
                    return Flux.error(new RuntimeException("Failed to retrieve products", throwable));
                });
    }

    @Override
    public Mono<Product> handle(GetProductByIdQuery query) {
        if (query.productId() == null || query.productId() <= 0) {
            return Mono.error(new IllegalArgumentException("Product ID must be a positive number"));
        }

        return productRepository.findById(query.productId())
                .onErrorResume(throwable -> {
                    return Mono.error(new RuntimeException("Failed to retrieve product", throwable));
                });
    }

    @Override
    public Flux<Product> handle(GetProductsByClientIdQuery query) {
        return productRepository.findByClientId(query.clientId());
    }
}