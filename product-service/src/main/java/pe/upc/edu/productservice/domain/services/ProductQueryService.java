package pe.upc.edu.productservice.domain.services;

import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.queries.GetAllProductsQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductByIdQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductQueryService {
   Flux<Product> handle(GetAllProductsQuery query);
   Mono<Product> handle(GetProductByIdQuery query);
}