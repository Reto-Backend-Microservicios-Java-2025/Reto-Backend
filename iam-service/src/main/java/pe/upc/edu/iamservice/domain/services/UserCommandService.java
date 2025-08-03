package pe.upc.edu.iamservice.domain.services;

import pe.upc.edu.iamservice.domain.model.commands.SignUpCommand;
import pe.upc.edu.iamservice.domain.model.commands.SignInCommand;
import pe.upc.edu.iamservice.domain.model.aggregates.User;
import reactor.core.publisher.Mono;

public interface UserCommandService {
    Mono<User> handle(SignUpCommand command);
    Mono<String> handle(SignInCommand command);
}