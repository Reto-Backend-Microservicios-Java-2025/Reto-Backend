package pe.upc.edu.productservice.interfaces.rest.transform;

import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.interfaces.rest.resources.CreateProductResource;

public class CreateProductCommandFromResourceAssembler {
    public static CreateProductCommand toCommandFromResource(CreateProductResource resource) {
        return new CreateProductCommand(
                resource.clientId(),
                resource.productType(),
                resource.name(),
                resource.balance());
    }
}