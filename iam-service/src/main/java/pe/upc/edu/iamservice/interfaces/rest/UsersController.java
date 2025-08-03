package pe.upc.edu.iamservice.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.upc.edu.iamservice.infrastructure.security.JwtService;
import pe.upc.edu.iamservice.domain.model.commands.SignInCommand;
import pe.upc.edu.iamservice.domain.model.commands.SignUpCommand;
import pe.upc.edu.iamservice.domain.model.queries.GetAllUsersQuery;
import pe.upc.edu.iamservice.domain.model.queries.GetUserByEmailQuery;
import pe.upc.edu.iamservice.domain.services.UserCommandService;
import pe.upc.edu.iamservice.domain.services.UserQueryService;
import pe.upc.edu.iamservice.interfaces.rest.resources.*;
import pe.upc.edu.iamservice.interfaces.rest.transform.TransformService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final JwtService jwtService;

    @PostMapping("/sign-up")
    public Mono<ResponseEntity<AuthenticationResponse>> signUp(@RequestBody SignUpResource resource) {
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

    @GetMapping
    public Flux<UserResource> getAllUsers() {
        GetAllUsersQuery query = new GetAllUsersQuery();
        return userQueryService.handle(query)
                .map(TransformService::toResourceFromEntity);
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResource>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String token = authHeader.substring(7); // Remove "Bearer "
        String email;

        try {
            email = jwtService.extractUsername(token);
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        GetUserByEmailQuery query = new GetUserByEmailQuery(email);
        return userQueryService.handle(query)
                .map(TransformService::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
