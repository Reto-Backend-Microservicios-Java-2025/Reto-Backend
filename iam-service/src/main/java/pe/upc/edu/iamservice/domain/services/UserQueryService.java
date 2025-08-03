package pe.upc.edu.iamservice.domain.services;

import pe.upc.edu.iamservice.domain.model.queries.GetUserByIdQuery;
import pe.upc.edu.iamservice.domain.model.queries.GetUserByEmailQuery;
import pe.upc.edu.iamservice.domain.model.aggregates.User;
import reactor.core.publisher.Mono;

public interface UserQueryService {
    Mono<User> handle(GetUserByIdQuery query);
    Mono<User> handle(GetUserByEmailQuery query);
}