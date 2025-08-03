package pe.upc.edu.iamservice.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import pe.upc.edu.iamservice.domain.model.commands.SignInCommand;
import pe.upc.edu.iamservice.domain.model.commands.SignUpCommand;
import pe.upc.edu.iamservice.domain.model.queries.GetUserByEmailQuery;
import pe.upc.edu.iamservice.domain.services.UserCommandService;
import pe.upc.edu.iamservice.domain.services.UserQueryService;
import pe.upc.edu.iamservice.interfaces.rest.resources.*;
import pe.upc.edu.iamservice.interfaces.rest.transform.TransformService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    @PostMapping("/sign-up")
    public Mono<ResponseEntity<AuthenticationResponse>> signUp(@Valid @RequestBody SignUpResource resource) {
        SignUpCommand command = TransformService.toCommandFromResource(resource);
        return userCommandService.handle(command)
                .flatMap(user -> {
                    SignInCommand signInCommand = new SignInCommand(user.getEmail(), resource.password());
                    return userCommandService.handle(signInCommand)
                            .map(token -> TransformService.toAuthenticationResponse(token, user));
                })
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @PostMapping("/sign-in")
    public Mono<ResponseEntity<AuthenticationResponse>> signIn(@RequestBody SignInResource resource) {
        SignInCommand command = TransformService.toCommandFromResource(resource);
        return userCommandService.handle(command)
                .flatMap(token -> {
                    GetUserByEmailQuery query = new GetUserByEmailQuery(resource.email());
                    return userQueryService.handle(query)
                            .map(user -> TransformService.toAuthenticationResponse(token, user));
                })
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResource>> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(Authentication.class)
                .map(Authentication::getName) // Obtiene el email del usuario autenticado
                .flatMap(email -> {
                    GetUserByEmailQuery query = new GetUserByEmailQuery(email);
                    return userQueryService.handle(query);
                })
                .map(TransformService::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
