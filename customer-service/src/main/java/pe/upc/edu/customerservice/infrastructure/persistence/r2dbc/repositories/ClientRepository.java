package pe.upc.edu.customerservice.infrastructure.persistence.r2dbc.repositories;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import reactor.core.publisher.Mono;

@Repository
public interface ClientRepository extends R2dbcRepository<Client, Long> {
    Mono<Client> findByUniqueCode(Long uniqueCode);
    Mono<Boolean> existsByFullName(String fullName);
}