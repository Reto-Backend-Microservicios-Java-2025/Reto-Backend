package pe.upc.edu.iamservice.domain.model.queries;

public record GetUserByIdQuery(Long userId) {
    public GetUserByIdQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }
}
