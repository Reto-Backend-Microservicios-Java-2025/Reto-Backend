package pe.upc.edu.productservice.domain.services;

import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.domain.model.commands.DeleteProductCommand;
import pe.upc.edu.productservice.domain.model.commands.UpdateProductCommand;

import java.util.Optional;

public interface ProductCommandService {
    Optional<Product> handle(UpdateProductCommand command);
    Long handle(CreateProductCommand command);
    void handle(DeleteProductCommand command);
}
