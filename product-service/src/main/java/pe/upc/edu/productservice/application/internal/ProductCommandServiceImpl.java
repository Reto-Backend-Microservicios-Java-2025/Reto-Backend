package pe.upc.edu.productservice.application.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.upc.edu.productservice.application.external.clients.ReactiveExternalClientValidator;
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
    private static final Logger logger = LoggerFactory.getLogger(ProductCommandServiceImpl.class);

    private final ProductRepository productRepository;
    private final ReactiveExternalClientValidator clientValidator;

    public ProductCommandServiceImpl(ProductRepository productRepository,
                                     ReactiveExternalClientValidator clientValidator) {
        this.clientValidator = clientValidator;
        this.productRepository = productRepository;
        logger.info("✅ ProductCommandServiceImpl initialized with client validator");
    }

    @Override
    public Mono<Long> handle(CreateProductCommand command) {
        logger.info("🚀 Creating product for client: {}, name: {}", command.clientId(), command.name());

        return validateCreateCommand(command)
                .doOnNext(v -> logger.info("✅ Validation passed for product creation"))
                .then(Mono.defer(() -> {
                    logger.info("💾 Saving product to database");
                    var product = new Product(command);
                    return productRepository.save(product)
                            .map(Product::getId);
                }))
                .doOnSuccess(productId -> logger.info("✅ Product created successfully with ID: {}", productId))
                .doOnError(error -> logger.error("❌ Failed to create product: {}", error.getMessage()))
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
                .map(product -> product.updateInformation(
                        command.productType(),
                        command.name(),
                        command.balance()
                ))
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
        logger.info("🔍 Starting validation for CreateProductCommand");

        return Mono.fromRunnable(() -> {
                    logger.info("🔍 Validating basic fields");
                    if (command.name() == null || command.name().trim().isEmpty()) {
                        throw new IllegalArgumentException("Product name cannot be null or empty");
                    }
                    if (command.balance() == null || command.balance() < 0) {
                        throw new IllegalArgumentException("Product balance cannot be null or negative");
                    }
                    if (command.productType() == null) {
                        throw new IllegalArgumentException("Product type cannot be null");
                    }
                    if (command.clientId() == null || command.clientId() <= 0) {
                        throw new IllegalArgumentException("Client ID must be a positive number");
                    }
                    logger.info("✅ Basic field validation passed");
                })
                .then(Mono.defer(() -> {
                    logger.info("🔍 Validating client existence for clientId: {}", command.clientId());
                    return clientValidator.clientExists(command.clientId());
                }))
                .flatMap(clientExists -> {
                    logger.info("🎯 Client exists result: {}", clientExists);
                    if (!clientExists) {
                        logger.error("❌ Client validation failed - client {} does not exist", command.clientId());
                        return Mono.error(new IllegalArgumentException(
                                "Client with ID " + command.clientId() + " does not exist"));
                    }
                    logger.info("✅ Client validation passed");
                    return Mono.empty();
                })
                .then(Mono.defer(() -> {
                    logger.info("🔍 Checking for duplicate product name");
                    return productRepository.existsByClientIdAndName(command.clientId(), command.name());
                }))
                .flatMap(exists -> {
                    if (exists) {
                        logger.error("❌ Product name validation failed - duplicate name");
                        return Mono.error(new IllegalArgumentException(
                                "Product with name '" + command.name() + "' already exists for client " + command.clientId()));
                    }
                    logger.info("✅ Product name validation passed");
                    return Mono.empty();
                });
    }

    private Mono<Void> validateUpdateCommand(UpdateProductCommand command) {
        return Mono.fromRunnable(() -> {
                    if (command.id() == null || command.id() <= 0) {
                        throw new IllegalArgumentException("Product ID must be a positive number");
                    }
                    if (command.name() == null) {
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