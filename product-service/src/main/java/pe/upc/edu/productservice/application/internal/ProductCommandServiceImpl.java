package pe.upc.edu.productservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.productservice.domain.exceptions.ProductNotFoundException;
import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.domain.model.commands.DeleteProductCommand;
import pe.upc.edu.productservice.domain.model.commands.UpdateProductCommand;
import pe.upc.edu.productservice.domain.services.ProductCommandService;
import pe.upc.edu.productservice.infrastructure.persistence.r2dbc.repositories.ProductRepository;
import reactor.core.publisher.Mono;

@Service
public class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductRepository productRepository;

    public ProductCommandServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Mono<Long> handle(CreateProductCommand command) {
        return validateCreateCommand(command)
                .then(Mono.defer(() -> {
                    var product = new Product(command);
                    return productRepository.save(product)
                            .map(Product::getId);
                }))
                .onErrorResume(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Mono.error(throwable);
                    }
                    return Mono.error(new RuntimeException("Failed to create product", throwable));
                });
    }

    @Override
    public Mono<Product> handle(UpdateProductCommand command) {
        return validateUpdateCommand(command)
                .then(productRepository.findById(command.id()))
                .switchIfEmpty(Mono.error(new ProductNotFoundException(command.id())))
                .map(product -> product.updateInformation(command.productType(), command.name(), command.balance()))
                .flatMap(productRepository::save)
                .onErrorResume(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Mono.error(throwable);
                    }
                    return Mono.error(new RuntimeException("Failed to update product", throwable));
                });
    }

    @Override
    public Mono<Void> handle(DeleteProductCommand command) {
        return productRepository.existsById(command.productId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ProductNotFoundException(command.productId()));
                    }
                    return productRepository.deleteById(command.productId());
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Mono.error(throwable);
                    }
                    return Mono.error(new RuntimeException("Failed to delete product", throwable));
                });
    }

    private Mono<Void> validateCreateCommand(CreateProductCommand command) {
        return Mono.fromRunnable(() -> {
                    if (command.name() == null) {
                        throw new IllegalArgumentException("Product name cannot be null or empty");
                    }
                    if (command.balance() == null || command.balance() < 0) {
                        throw new IllegalArgumentException("Product balance cannot be null or negative");
                    }
                    if (command.productType() == null) {
                        throw new IllegalArgumentException("Product type cannot be null");
                    }
                })
                .then(productRepository.existsByName(command.name()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Product with name '" + command.name() + "' already exists"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateUpdateCommand(UpdateProductCommand command) {
        return Mono.fromRunnable(() -> {
            if (command.id() == null || command.id() <= 0) {
                throw new IllegalArgumentException("Product ID must be a positive number");
            }
            if (command.name() == null || command.name().trim().isEmpty()) {
                throw new IllegalArgumentException("Product name cannot be null or empty");
            }
            if (command.balance() == null || command.balance() < 0) {
                throw new IllegalArgumentException("Product balance cannot be null or negative");
            }
            if (command.productType() == null) {
                throw new IllegalArgumentException("Product type cannot be null");
            }
        });
    }
}