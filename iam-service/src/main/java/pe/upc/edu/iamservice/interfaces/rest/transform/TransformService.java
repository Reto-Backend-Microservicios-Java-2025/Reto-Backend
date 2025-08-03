package pe.upc.edu.iamservice.interfaces.rest.transform;

import pe.upc.edu.iamservice.domain.model.aggregates.User;
import pe.upc.edu.iamservice.domain.model.commands.SignInCommand;
import pe.upc.edu.iamservice.domain.model.commands.SignUpCommand;
import pe.upc.edu.iamservice.interfaces.rest.resources.*;

public class TransformService {

    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(resource.email(), resource.password());
    }

    public static SignInCommand toCommandFromResource(SignInResource resource) {
        return new SignInCommand(resource.email(), resource.password());
    }

    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(user.getId(), user.getEmail());
    }

    public static AuthenticationResponse toAuthenticationResponse(String token, User user) {
        return new AuthenticationResponse(token, toResourceFromEntity(user));
    }
}