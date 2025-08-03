package pe.upc.edu.iamservice.interfaces.rest.resources;

public record AuthenticationResponse(String token, UserResource user) {
}
