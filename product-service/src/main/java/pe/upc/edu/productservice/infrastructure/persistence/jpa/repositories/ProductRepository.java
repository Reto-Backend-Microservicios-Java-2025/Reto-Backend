package pe.upc.edu.productservice.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.upc.edu.productservice.domain.model.aggregates.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
      boolean existsById(Long id);
      boolean existsByName(String name);
}
