package pe.upc.edu.iamservice.infrastructure.persistence.r2dbc.repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pe.upc.edu.iamservice.domain.model.aggregates.User;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    @Query("SELECT COUNT(*) > 0 FROM users WHERE id = :id")
    Mono<Boolean> existsById(Long id);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    Mono<Boolean> existsByEmail(String email);

    Mono<User> findByEmail(String email);

    Mono<User> findByPassword(String password);
}