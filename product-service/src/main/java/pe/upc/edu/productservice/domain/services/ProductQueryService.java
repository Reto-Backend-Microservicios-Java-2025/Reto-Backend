package pe.upc.edu.productservice.domain.services;

import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.queries.GetAllProductsQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductByIdQuery;

import java.util.List;
import java.util.Optional;

public interface ProductQueryService {
   Optional<Product> handle(GetProductByIdQuery query);
   List<Product> handle(GetAllProductsQuery query);
}
