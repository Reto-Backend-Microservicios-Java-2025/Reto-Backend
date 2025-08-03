package pe.upc.edu.iamservice.application.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.upc.edu.iamservice.domain.model.aggregates.User;
import pe.upc.edu.iamservice.domain.model.queries.GetUserByEmailQuery;
import pe.upc.edu.iamservice.domain.model.queries.GetUserByIdQuery;
import pe.upc.edu.iamservice.domain.services.UserQueryService;
import pe.upc.edu.iamservice.infrastructure.persistence.r2dbc.repositories.UserRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public Mono<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public Mono<User> handle(GetUserByEmailQuery query) {
        return userRepository.findByEmail(query.email());
    }
}