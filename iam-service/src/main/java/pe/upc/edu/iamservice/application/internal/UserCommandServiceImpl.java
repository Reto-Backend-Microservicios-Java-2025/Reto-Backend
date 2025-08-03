package pe.upc.edu.iamservice.application.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.upc.edu.iamservice.domain.model.aggregates.User;
import pe.upc.edu.iamservice.domain.model.commands.SignInCommand;
import pe.upc.edu.iamservice.domain.model.commands.SignUpCommand;
import pe.upc.edu.iamservice.domain.services.UserCommandService;
import pe.upc.edu.iamservice.infrastructure.persistence.r2dbc.repositories.UserRepository;
import pe.upc.edu.iamservice.infrastructure.security.JwtService;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public Mono<User> handle(SignUpCommand command) {
        return userRepository.existsByEmail(command.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("User with email already exists"));
                    }
                    String encodedPassword = passwordEncoder.encode(command.password());
                    User user = new User(command.email(), encodedPassword);
                    return userRepository.save(user);
                });
    }

    @Override
    public Mono<String> handle(SignInCommand command) {
        return userRepository.findByEmail(command.email())
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")))
                .flatMap(user -> {
                    if (passwordEncoder.matches(command.password(), user.getPassword())) {
                        String token = jwtService.generateToken(user.getEmail(), user.getId());
                        return Mono.just(token);
                    } else {
                        return Mono.error(new RuntimeException("Invalid credentials"));
                    }
                });
    }
}