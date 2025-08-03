package pe.upc.edu.iamservice.domain.model.queries;

public record GetUserByEmailQuery(String email) {
    public GetUserByEmailQuery {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
    }
}
