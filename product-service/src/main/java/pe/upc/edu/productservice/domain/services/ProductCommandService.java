package pe.upc.edu.productservice.domain.services;

import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.domain.model.commands.DeleteProductCommand;
import pe.upc.edu.productservice.domain.model.commands.UpdateProductCommand;
import reactor.core.publisher.Mono;

public interface ProductCommandService {
    Mono<Long> handle(CreateProductCommand command);
    Mono<Product> handle(UpdateProductCommand command);
    Mono<Void> handle(DeleteProductCommand command);
}