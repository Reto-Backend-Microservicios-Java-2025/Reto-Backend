package pe.upc.edu.productservice.infrastructure.persistence.r2dbc.repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pe.upc.edu.productservice.domain.model.aggregates.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long> {

      @Query("SELECT COUNT(*) > 0 FROM products WHERE id = :id")
      Mono<Boolean> existsById(Long id);

      Mono<Boolean> existsByClientIdAndName(Long clientId, String name);

      Flux<Product> findByClientId(Long clientId);
}
