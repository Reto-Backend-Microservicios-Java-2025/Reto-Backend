package pe.upc.edu.iamservice.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpResource(
        @Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be blank")
        String email,

        @Size(min = 8, message = "Password must be at least 8 characters long")
        @NotBlank(message = "Password cannot be blank")
        String password) {
}
