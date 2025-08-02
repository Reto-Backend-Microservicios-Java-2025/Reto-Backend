package pe.upc.edu.productservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.productservice.domain.exceptions.ProductNotFoundException;
import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.domain.model.commands.DeleteProductCommand;
import pe.upc.edu.productservice.domain.model.commands.UpdateProductCommand;
import pe.upc.edu.productservice.domain.services.ProductCommandService;
import pe.upc.edu.productservice.infrastructure.persistence.jpa.repositories.ProductRepository;

import java.util.Optional;

@Service
public class ProductCommandServiceImpl implements ProductCommandService {
    private final ProductRepository productRepository;

    public ProductCommandServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Long handle(CreateProductCommand command) {
        var product = new Product(command);
        try {
            if (productRepository.existsById(product.getId())) {
                throw new IllegalArgumentException("Product with this ID already exists");
            }
            if (productRepository.existsByName(product.getName())) {
                throw new IllegalArgumentException("Product with this NAME already exists");
            }
            productRepository.save(product);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while saving product: " + e.getMessage());
        }
        return product.getId();
    }

    @Override
    public Optional<Product> handle(UpdateProductCommand command) {
        if (!productRepository.existsById(command.id())) throw new ProductNotFoundException(command.id());
        var result = productRepository.findById(command.id());
        if (result.isEmpty()) throw new IllegalArgumentException("Product does not exist");
        var productToUpdate = result.get();
        try {
            var updatedProduct = productRepository.save(productToUpdate.updateInformation(
                    command.productType(),
                    command.name(),
                    command.balance()));
            return Optional.of(updatedProduct);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while updating product: " + e.getMessage());
        }
    }

    @Override
    public void handle(DeleteProductCommand command) {
        if (!productRepository.existsById(command.productId())) {
            throw new IllegalArgumentException("Product does not exist");
        }
        try {
            productRepository.deleteById(command.productId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while deleting product: " + e.getMessage());
        }
    }
}