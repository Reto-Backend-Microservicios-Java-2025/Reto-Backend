package pe.upc.edu.productservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.domain.model.queries.GetAllProductsQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductByIdQuery;
import pe.upc.edu.productservice.domain.services.ProductQueryService;
import pe.upc.edu.productservice.infrastructure.persistence.jpa.repositories.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    public ProductQueryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> handle(GetAllProductsQuery query) {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> handle(GetProductByIdQuery query) {
        if (!productRepository.existsById(query.productId())) {
            throw new IllegalArgumentException("productId not found");
        }
        return productRepository.findById(query.productId());
    }
}
